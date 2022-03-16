/*
 * Copyright 2022 HM Revenue & Customs
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

package controllers.preMigration

import connectors.cache.FeatureToggleConnector
import connectors.{AncillaryPsaException, ListOfSchemes5xxException, ListOfSchemesConnector}
import controllers.ControllerSpecBase
import controllers.actions._
import forms.ListSchemesFormProvider
import matchers.JsonMatchers
import models.FeatureToggle.Enabled
import models.FeatureToggleName.MigrationTransfer
import models.{Items, ListOfLegacySchemes, RacDac, Scheme}
import org.mockito.ArgumentMatchers.any
import org.mockito.{ArgumentMatchers, MockitoSugar}
import org.scalatest.TryValues
import play.api.mvc.Result
import play.api.mvc.Results._
import play.api.test.Helpers.{status, _}
import services.{LockingService, SchemeSearchService}
import uk.gov.hmrc.nunjucks.NunjucksSupport

import scala.concurrent.Future
class ListOfSchemesControllerSpec extends ControllerSpecBase with NunjucksSupport with JsonMatchers with TryValues with MockitoSugar {

  private val mockSchemeSearchService: SchemeSearchService = mock[SchemeSearchService]
  private val mockLockingService: LockingService = mock[LockingService]
  private val mockListOfSchemesConnector: ListOfSchemesConnector = mock[ListOfSchemesConnector]
  private val schemeDetail = Items("10000678RE", "2020-10-10", racDac = false, "abcdefghi", "2020-12-12", None)
  private val racDacDetail = Items("10000678RF", "2020-10-10", racDac = true, "abcdefghi", "2020-12-12", Some("12345678"))
  private val expectedResponse = ListOfLegacySchemes(1, Some(List(schemeDetail, racDacDetail)))
  private val expectedResponseWithEmpty = ListOfLegacySchemes(1, None)
  private val mockFeatureToggleConnector: FeatureToggleConnector = mock[FeatureToggleConnector]

  private val formProvider: ListSchemesFormProvider = new ListSchemesFormProvider()

  private def controller: ListOfSchemesController =
    new ListOfSchemesController(mockAppConfig, messagesApi, new FakeAuthAction(),
      controllerComponents, formProvider,mockListOfSchemesConnector, mockSchemeSearchService, mockLockingService, mockFeatureToggleConnector)


  override def beforeEach: Unit = {
    super.beforeEach
    reset(mockFeatureToggleConnector)
    when(mockFeatureToggleConnector.get(any())(any(), any())).thenReturn(Future.successful(Enabled(MigrationTransfer)))
  }


  "onPageLoad" must {
    "return OK and the correct view returned by the service" in {

      when(mockSchemeSearchService.searchAndRenderView(any(), any(), any(), any(), any())(any(), any(), any(), any()))
        .thenReturn(Future.successful(Ok("")))
      when(mockListOfSchemesConnector.getListOfSchemes(any())(any(),any())).thenReturn(Future.successful(Right(expectedResponse)))

      val result = controller.onPageLoad(Scheme)(fakeRequest)

      status(result) mustBe OK
      contentAsString(result) mustBe ""
    }

    "redirect to the cannot migrate page when AncillaryPSAException is thrown" in {
      reset(mockListOfSchemesConnector)
      when(mockListOfSchemesConnector.getListOfSchemes(any())(any(),any())).thenReturn(Future.failed(AncillaryPsaException()))
      val result = controller.onPageLoad(Scheme)(fakeRequest)
      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(controllers.preMigration.routes.CannotMigrateController.onPageLoad().url)
    }

    "redirect to the 'There is a problem' page when ListOfSchemes5xxException is thrown" in {
      reset(mockListOfSchemesConnector)
      when(mockListOfSchemesConnector.getListOfSchemes(any())(any(),any())).thenReturn(Future.failed(ListOfSchemes5xxException()))
      val result = controller.onPageLoad(Scheme)(fakeRequest)
      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(controllers.preMigration.routes.ThereIsAProblemController.onPageLoad().url)
    }

    "return OK and the correct view for a GET for scheme with RacDac Only" in {
      reset(mockListOfSchemesConnector)
      when(mockListOfSchemesConnector.getListOfSchemes(any())(any(),any())).thenReturn(Future.successful(Right(expectedResponseWithEmpty)))
      val result: Future[Result] = controller.onPageLoad(RacDac)(fakeDataRequest())
      status(result) mustEqual SEE_OTHER
      redirectLocation(result) mustBe Some(routes.NoSchemeToAddController.onPageLoadRacDac().url)
    }

    "return OK and the correct view for a GET for scheme with Scheme Only" in {
      reset(mockListOfSchemesConnector)
      when(mockListOfSchemesConnector.getListOfSchemes(any())(any(),any())).thenReturn(Future.successful(Right(expectedResponseWithEmpty)))
      val result: Future[Result] = controller.onPageLoad(Scheme)(fakeDataRequest())
      status(result) mustEqual SEE_OTHER
      redirectLocation(result) mustBe Some(routes.NoSchemeToAddController.onPageLoadScheme().url)
    }

  }

  "onPageLoadWithPageNumber" must {
    "return OK and the correct view returned by the service" in {

      when(mockSchemeSearchService.searchAndRenderView(any(), any(), any(), any(), any())(any(), any(), any(), any()))
        .thenReturn(Future.successful(Ok("")))

      val result = controller.onPageLoadWithPageNumber(1, Scheme)(fakeRequest)

      status(result) mustBe OK
      contentAsString(result) mustBe ""

    }
  }

  "onSearch" when {
    "return OK and the correct view when there are schemes without pagination and search on non empty string" in {
      when(mockSchemeSearchService.searchAndRenderView(any(), any(), any(), any(), any())(any(), any(), any(), any()))
        .thenReturn(Future.successful(Ok("")))
      val searchText = "pstr1"


      val postRequest = fakeRequest.withFormUrlEncodedBody(("value", searchText))
      val result = controller.onSearch(Scheme)(postRequest)

      status(result) mustBe OK
    }

    "return BADREQUEST and error when no value is entered into search" in {
      when(mockSchemeSearchService.searchAndRenderView(any(), any(), ArgumentMatchers.eq(None), any(), any())(any(), any(), any(), any()))
        .thenReturn(Future.successful(BadRequest("")))

      val postRequest = fakeRequest.withFormUrlEncodedBody(("value", ""))
      val result = controller.onSearch(Scheme)(postRequest)

      status(result) mustBe BAD_REQUEST

    }
  }


}
