/*
 * Copyright 2021 HM Revenue & Customs
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

import controllers.ControllerSpecBase
import controllers.actions.MutableFakeDataRetrievalAction
import matchers.JsonMatchers
import org.mockito.ArgumentMatchers.any
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceableModule
import play.api.mvc.Results.{BadRequest, Ok}
import play.api.test.Helpers._
import services.BulkRacDacService
import uk.gov.hmrc.nunjucks.NunjucksSupport
import uk.gov.hmrc.viewmodels.Table
import utils.Enumerable

import scala.concurrent.Future

class BulkListControllerSpec extends ControllerSpecBase with NunjucksSupport with JsonMatchers with Enumerable.Implicits {

  val table: Table = Table(head = Nil, rows = Nil)
  private val mockBulkRacDacService: BulkRacDacService = mock[BulkRacDacService]

  private val mutableFakeDataRetrievalAction: MutableFakeDataRetrievalAction = new MutableFakeDataRetrievalAction()
  val extraModules: Seq[GuiceableModule] = Seq(
    bind[BulkRacDacService].toInstance(mockBulkRacDacService)
  )
  private val application: Application = applicationBuilderMutableRetrievalAction(mutableFakeDataRetrievalAction, extraModules).build()

  private def httpPathGET: String = routes.BulkListController.onPageLoad().url
  private def httpPathPOST: String = routes.BulkListController.onSubmit().url

  private val valuesValid: Map[String, Seq[String]] = Map(
    "value" -> Seq("true")
  )

  private val valuesInvalid: Map[String, Seq[String]] = Map(
    "value" -> Seq("invalid")
  )

  override def beforeEach: Unit = {
    super.beforeEach
    when(mockBulkRacDacService.renderRacDacBulkView(any(), any())(any(), any(), any())).thenReturn(Future.successful(Ok("")))
    when(mockAppConfig.psaOverviewUrl) thenReturn appConfig.psaOverviewUrl
  }


  "BulkListController" must {

    "return OK and the correct view for a GET, passing the correct no of trustees and max trustees into template" in {


      val result = route(application, httpGETRequest(httpPathGET)).value

      status(result) mustEqual OK
    }

    "redirect to next page when user answers yes" in {

      val result = route(application, httpPOSTRequest(httpPathPOST, valuesValid)).value

      status(result) mustEqual SEE_OTHER

      redirectLocation(result) mustBe Some(routes.DeclarationController.onPageLoad().url)
    }

    "redirect to next page when user answers no" in {

      val result = route(application, httpPOSTRequest(httpPathPOST, Map("value" -> Seq("false")))).value

      status(result) mustEqual SEE_OTHER

      redirectLocation(result) mustBe Some(appConfig.psaOverviewUrl)
    }

    "return a BAD REQUEST when invalid data is submitted" in {
      when(mockBulkRacDacService.renderRacDacBulkView(any(), any())(any(), any(), any())).thenReturn(Future.successful(BadRequest("")))
      val result = route(application, httpPOSTRequest(httpPathPOST, valuesInvalid)).value

      status(result) mustEqual BAD_REQUEST
    }
  }
}
