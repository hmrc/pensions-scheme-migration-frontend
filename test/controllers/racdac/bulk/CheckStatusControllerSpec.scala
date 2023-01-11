/*
 * Copyright 2023 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package controllers.racdac.bulk

import connectors.cache.BulkMigrationQueueConnector
import connectors.{AncillaryPsaException, ListOfSchemes5xxException, ListOfSchemesConnector}
import controllers.ControllerSpecBase
import controllers.actions.MutableFakeDataRetrievalAction
import matchers.JsonMatchers
import models.{Items, ListOfLegacySchemes}
import org.mockito.ArgumentMatchers.any
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceableModule
import play.api.test.Helpers._
import uk.gov.hmrc.nunjucks.NunjucksSupport
import utils.Enumerable

import scala.concurrent.Future
class CheckStatusControllerSpec extends ControllerSpecBase with NunjucksSupport with JsonMatchers with Enumerable.Implicits {

  private val mutableFakeDataRetrievalAction: MutableFakeDataRetrievalAction = new MutableFakeDataRetrievalAction()
  private val mockQueueConnector = mock[BulkMigrationQueueConnector]
  private val mockListSchemesConnector = mock[ListOfSchemesConnector]
  private val extraModules: Seq[GuiceableModule] = Seq(
    bind[BulkMigrationQueueConnector].to(mockQueueConnector),
    bind[ListOfSchemesConnector].to(mockListSchemesConnector)
  )
  private val application: Application = applicationBuilderMutableRetrievalAction(mutableFakeDataRetrievalAction, extraModules).build()

  private def httpPathGET: String = controllers.racdac.bulk.routes.CheckStatusController.onPageLoad.url

  "CheckStatusController" must {

    "redirect to the cannot migrate page when AncillaryPSAException is thrown" in {
      reset(mockListSchemesConnector)
      when(mockQueueConnector.isAllFailed(any())(any(), any())).thenReturn(Future.successful(None))
      when(mockListSchemesConnector.getListOfSchemes(any())(any(),any())).thenReturn(Future.failed(AncillaryPsaException()))
      val result = route(application, httpGETRequest(httpPathGET)).value
      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(controllers.preMigration.routes.CannotMigrateController.onPageLoad.url)
    }

    "redirect to the 'There is a problem' page when ListOfSchemes5xxException is thrown" in {
      reset(mockListSchemesConnector)
      when(mockQueueConnector.isAllFailed(any())(any(), any())).thenReturn(Future.successful(None))
      when(mockListSchemesConnector.getListOfSchemes(any())(any(),any())).thenReturn(Future.failed(ListOfSchemes5xxException()))
      val result = route(application, httpGETRequest(httpPathGET)).value
      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(controllers.preMigration.routes.ThereIsAProblemController.onPageLoad.url)
    }

    "redirect to finished status page when all failed items left in the queue" in {
      when(mockQueueConnector.isAllFailed(any())(any(), any())).thenReturn(Future.successful(Some(true)))

      val result = route(application, httpGETRequest(httpPathGET)).value

      status(result) mustEqual SEE_OTHER
      redirectLocation(result).value mustBe controllers.racdac.bulk.routes.FinishedStatusController.onPageLoad.url
    }

    "redirect to in progress page when items are in progress/todo in the queue" in {
      when(mockQueueConnector.isAllFailed(any())(any(), any())).thenReturn(Future.successful(Some(false)))

      val result = route(application, httpGETRequest(httpPathGET)).value

      status(result) mustEqual SEE_OTHER
      redirectLocation(result).value mustBe controllers.racdac.bulk.routes.InProgressController.onPageLoad.url
    }

    "redirect to no scheme to add page when there is nothing in the queue and no schemes" in {
      when(mockQueueConnector.isAllFailed(any())(any(), any())).thenReturn(Future.successful(None))
      when(mockListSchemesConnector.getListOfSchemes(any())(any(), any())).thenReturn(Future(Right(ListOfLegacySchemes(0, None))))

      val result = route(application, httpGETRequest(httpPathGET)).value

      status(result) mustEqual SEE_OTHER
      redirectLocation(result).value mustBe controllers.preMigration.routes.NoSchemeToAddController.onPageLoadRacDac.url
    }

    "redirect to psa overview page when there is nothing in the queue and there are schemes" in {
      val listOfSchemes = List(Items("test-pstr", "", true, "test-scheme", "", Some("")))
      when(mockQueueConnector.isAllFailed(any())(any(), any())).thenReturn(Future.successful(None))
      when(mockListSchemesConnector.getListOfSchemes(any())(any(), any())).
        thenReturn(Future(Right(ListOfLegacySchemes(1, Some(listOfSchemes)))))
      when(mockAppConfig.psaOverviewUrl).thenReturn("/foo")

      val result = route(application, httpGETRequest(httpPathGET)).value

      status(result) mustEqual SEE_OTHER
      redirectLocation(result).value mustBe "/foo"
    }

    "redirect to cannot migrate page when there is nothing in the queue and list of schems has thrown AncillaryPsaException" in {
      when(mockQueueConnector.isAllFailed(any())(any(), any())).thenReturn(Future.successful(None))
      when(mockListSchemesConnector.getListOfSchemes(any())(any(), any())).
        thenReturn(Future.failed(new AncillaryPsaException))

      val result = route(application, httpGETRequest(httpPathGET)).value

      status(result) mustEqual SEE_OTHER
      redirectLocation(result).value mustBe controllers.preMigration.routes.CannotMigrateController.onPageLoad.url
    }
  }
}
