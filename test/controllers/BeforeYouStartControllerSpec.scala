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

package controllers

import controllers.ControllerSpecBase
import controllers.actions._
import identifiers.establishers.company.CompanyDetailsId
import identifiers.establishers.individual.EstablisherNameId
import matchers.JsonMatchers
import models.{CompanyDetails, Index, NormalMode, PersonName}
import org.mockito.ArgumentCaptor
import org.mockito.Matchers.any
import org.mockito.Mockito.{times, verify, when}
import org.scalatest.TryValues
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.Result
import play.api.test.Helpers.{status, _}
import play.twirl.api.Html
import renderer.Renderer
import uk.gov.hmrc.viewmodels.NunjucksSupport
import utils.Data.ua
import utils.UserAnswers

import scala.concurrent.Future

class BeforeYouStartControllerSpec extends ControllerSpecBase with NunjucksSupport with JsonMatchers with TryValues {
  private val companyName: CompanyDetails = CompanyDetails("ABC Ltd")
  private val userAnswers: UserAnswers = ua.set(CompanyDetailsId(0), companyName).success.value
  private val templateToBeRendered: String = "beforeYouStart.njk"
  private def json: JsObject =
    Json.obj(
      "continueUrl" -> controllers.routes.TaskListController.onPageLoad().url,
      "schemeName"  -> "Test scheme name"
    )

  private def controller(dataRetrievalAction: DataRetrievalAction): BeforeYouStartController =
    new BeforeYouStartController(appConfig,messagesApi, new FakeAuthAction(), dataRetrievalAction,
      new DataRequiredActionImpl, minimalDetailsConnector, controllerComponents, new Renderer(mockAppConfig, mockRenderer))

  "BeforeYouStartController" must {
    "return OK and the correct view for a GET" in {
      when(mockRenderer.render(any(), any())(any())).thenReturn(Future.successful(Html("")))

      val templateCaptor = ArgumentCaptor.forClass(classOf[String])
      val jsonCaptor = ArgumentCaptor.forClass(classOf[JsObject])

      val getData = new FakeDataRetrievalAction(Some(userAnswers))
      val result: Future[Result] = controller(getData).onPageLoad()(fakeDataRequest(userAnswers))

      status(result) mustBe OK

      verify(mockRenderer, times(1)).render(templateCaptor.capture(), jsonCaptor.capture())(any())

      templateCaptor.getValue mustEqual templateToBeRendered

      jsonCaptor.getValue must containJson(json)
    }
  }
}
