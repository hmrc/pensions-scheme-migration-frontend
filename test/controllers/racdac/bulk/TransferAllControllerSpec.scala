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

import connectors.MinimalDetailsConnector
import controllers.ControllerSpecBase
import controllers.actions.FakeAuthAction
import forms.YesNoFormProvider
import matchers.JsonMatchers
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.scalatest.{BeforeAndAfterEach, TryValues}
import play.api.data.Form
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.{AnyContentAsFormUrlEncoded, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers.{status, _}
import play.twirl.api.Html
import renderer.Renderer
import uk.gov.hmrc.nunjucks.NunjucksSupport
import uk.gov.hmrc.viewmodels.Radios
import utils.Data.ua

import scala.concurrent.Future

class TransferAllControllerSpec extends ControllerSpecBase with NunjucksSupport with JsonMatchers with TryValues with BeforeAndAfterEach{

  private val psaName: String = "Psa Name"
  private val formProvider: YesNoFormProvider = new YesNoFormProvider()
  private val mockMinDetailsConnector: MinimalDetailsConnector = mock[MinimalDetailsConnector]

  private val templateToBeRendered: String = "racdac/transferAll.njk"
  private val form: Form[Boolean] = formProvider(messages("messages__transferAll__error"))

  private val commonJson: Form[Boolean] => JsObject = form => Json.obj(
    "form" -> form,
    "psaName" -> psaName,
    "returnUrl" -> appConfig.psaOverviewUrl
  )
  override def beforeEach: Unit = {
    reset(mockRenderer, mockUserAnswersCacheConnector)
    when(mockMinDetailsConnector.getPSAName(any(), any())) thenReturn Future.successful(psaName)
  }

  private def controller: TransferAllController =
    new TransferAllController(appConfig, messagesApi, new FakeAuthAction(), formProvider, mockMinDetailsConnector,
      controllerComponents, new Renderer(mockAppConfig, mockRenderer))

  "TransferAllController" must {

    "return OK and the correct view for a GET" in{

      val templateCaptor : ArgumentCaptor[String] = ArgumentCaptor.forClass(classOf[String])
      val jsonCaptor: ArgumentCaptor[JsObject] = ArgumentCaptor.forClass(classOf[JsObject])

      when(mockRenderer.render(any(), any())(any()))
        .thenReturn(Future.successful(Html("")))

      val result: Future[Result] = controller.onPageLoad(fakeDataRequest(ua))

      status(result) mustBe OK

      verify(mockRenderer, times(1)).render(templateCaptor.capture(), jsonCaptor.capture())(any())

      templateCaptor.getValue mustEqual  templateToBeRendered

      val json: JsObject = Json.obj("radios" -> Radios.yesNo(form("value")))

      jsonCaptor.getValue must containJson(commonJson(form) ++ json)
    }

    "redirect to the next page when valid data is submitted" in {
      val request: FakeRequest[AnyContentAsFormUrlEncoded] = fakeRequest.withFormUrlEncodedBody(("value", "true"))

      val result: Future[Result] = controller.onSubmit(request)

      status(result) mustBe SEE_OTHER

      redirectLocation(result) mustBe Some(routes.BulkListController.onPageLoad().url)
    }

    "return a Bad Request and errors when invalid data is submitted" in {
      when(mockRenderer.render(any(), any())(any()))
        .thenReturn(Future.successful(Html("")))

      val request = fakeRequest.withFormUrlEncodedBody(("value", "invalid value"))

      val jsonCaptor: ArgumentCaptor[JsObject] = ArgumentCaptor.forClass(classOf[JsObject])

      val templateCaptor : ArgumentCaptor[String] = ArgumentCaptor.forClass(classOf[String])

      val result: Future[Result] = controller.onSubmit(request)

      val boundForm = form.bind(Map("value" -> "invalid value"))

      status(result) mustBe BAD_REQUEST

      verify(mockRenderer, times(1)).render(templateCaptor.capture(), jsonCaptor.capture())(any())

      templateCaptor.getValue mustEqual templateToBeRendered

      val json: JsObject =
        Json.obj("radios" -> Radios.yesNo(boundForm.apply("value")))

      jsonCaptor.getValue must containJson(commonJson(boundForm) ++ json)

    }

  }

}
