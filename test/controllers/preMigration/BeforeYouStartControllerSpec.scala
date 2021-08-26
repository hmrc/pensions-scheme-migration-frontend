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

package controllers.preMigration

import connectors.MinimalDetailsConnector
import controllers.ControllerSpecBase
import controllers.actions._
import matchers.JsonMatchers
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.scalatest.TryValues
import org.mockito.MockitoSugar
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.Result
import play.api.test.Helpers.{status, _}
import play.twirl.api.Html
import renderer.Renderer
import uk.gov.hmrc.viewmodels.NunjucksSupport
import utils.UserAnswers

import scala.concurrent.Future

class BeforeYouStartControllerSpec extends ControllerSpecBase with NunjucksSupport with JsonMatchers with TryValues with MockitoSugar {
  private val psaName: String = "Nigel"
  private val templateToBeRendered: String = "preMigration/beforeYouStart.njk"
  private val mockMinimalDetailsConnector: MinimalDetailsConnector = mock[MinimalDetailsConnector]
  private def json: JsObject =
    Json.obj(
      "continueUrl" -> controllers.routes.TaskListController.onPageLoad().url,
      "psaName" -> psaName,
      "returnUrl" -> appConfig.psaOverviewUrl
    )

  private def controller(): BeforeYouStartController =
    new BeforeYouStartController(appConfig,messagesApi, new FakeAuthAction(), new FakeDataRetrievalAction(Some(UserAnswers())),
      new DataRequiredActionImpl, mockMinimalDetailsConnector, controllerComponents, new Renderer(mockAppConfig, mockRenderer))

  "BeforeYouStartController" must {
    "return OK and the correct view for a GET" in {
      when(mockRenderer.render(any(), any())(any())).thenReturn(Future.successful(Html("")))
      when(mockMinimalDetailsConnector.getPSAName(any(),any())).thenReturn(Future.successful(psaName))
      val templateCaptor = ArgumentCaptor.forClass(classOf[String])
      val jsonCaptor = ArgumentCaptor.forClass(classOf[JsObject])

      val result: Future[Result] = controller().onPageLoad()(fakeDataRequest())

      status(result) mustBe OK

      verify(mockRenderer, times(1)).render(templateCaptor.capture(), jsonCaptor.capture())(any())

      templateCaptor.getValue mustEqual templateToBeRendered

      jsonCaptor.getValue must containJson(json)
    }
  }
}
