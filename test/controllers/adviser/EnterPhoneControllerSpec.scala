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

package controllers.adviser

import controllers.ControllerSpecBase
import controllers.actions.{DataRequiredActionImpl, DataRetrievalAction, FakeAuthAction, FakeDataRetrievalAction}
import forms.PhoneFormProvider
import identifiers.adviser.{AdviserNameId, EnterPhoneId}
import matchers.JsonMatchers
import models.NormalMode
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, times, verify, when}
import org.scalatest.{BeforeAndAfterEach, TryValues}
import play.api.i18n.Messages
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.{AnyContentAsFormUrlEncoded, Call, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.twirl.api.Html
import renderer.Renderer
import uk.gov.hmrc.nunjucks.NunjucksSupport
import utils.Data.ua
import utils.{Data, FakeNavigator, UserAnswers}
import viewmodels.Message

import scala.concurrent.Future

class EnterPhoneControllerSpec extends ControllerSpecBase
  with NunjucksSupport
  with JsonMatchers
  with TryValues
  with BeforeAndAfterEach {

  private val advisorName: String = "test"
  private val phone = "777"
  private val formProvider: PhoneFormProvider = new PhoneFormProvider()
  private val form = formProvider(Message("messages__error__common__phone__required"),Some(Message("messages__phone__invalid")))
  private val onwardRoute: Call = controllers.routes.IndexController.onPageLoad
  private val userAnswers: UserAnswers = ua.set(AdviserNameId, advisorName).success.value
  private val templateToBeRendered: String = "phone.njk"

  private val commonJson: JsObject =
    Json.obj(
      "entityName" -> advisorName,
      "entityType" -> Messages("messages__pension__adviser"),
      "schemeName" -> Data.schemeName
    )
  private val formData: String = phone

  override def beforeEach(): Unit = {
    reset(
      mockRenderer,
      mockUserAnswersCacheConnector
    )
    when(mockRenderer.render(any(), any())(any())).thenReturn(Future.successful(Html("")))
  }

  private def controller(
                          dataRetrievalAction: DataRetrievalAction
                        ): EnterPhoneController =
    new EnterPhoneController(
      messagesApi = messagesApi,
      navigator = new FakeNavigator(desiredRoute = onwardRoute),
      authenticate = new FakeAuthAction(),
      getData = dataRetrievalAction,
      requireData = new DataRequiredActionImpl,
      formProvider = formProvider,
      controllerComponents = controllerComponents,
      userAnswersCacheConnector = mockUserAnswersCacheConnector,
      renderer = new Renderer(mockAppConfig, mockRenderer)
    )

  private val templateCaptor = ArgumentCaptor.forClass(classOf[String])
  private val jsonCaptor = ArgumentCaptor.forClass(classOf[JsObject])

  "EnterPhoneController" must {
    "return OK and the correct view for a GET" in {
      val getData = new FakeDataRetrievalAction(Some(userAnswers))

      val result: Future[Result] = controller(getData).onPageLoad(NormalMode)(fakeDataRequest(userAnswers))

      status(result) mustBe OK

      verify(mockRenderer, times(1)).render(templateCaptor.capture(), jsonCaptor.capture())(any())
      templateCaptor.getValue mustEqual templateToBeRendered
      val json: JsObject = Json.obj("form" -> form)
      jsonCaptor.getValue must containJson(commonJson ++ json)
    }

    "populate the view correctly on a GET when the question has previously been answered" in {
      val ua = userAnswers.set(EnterPhoneId, formData).success.value
      val getData = new FakeDataRetrievalAction(Some(ua))

      val result: Future[Result] =
        controller(getData)
          .onPageLoad(NormalMode)(fakeDataRequest(userAnswers))

      status(result) mustBe OK
      verify(mockRenderer, times(1))
        .render(templateCaptor.capture(), jsonCaptor.capture())(any())
      templateCaptor.getValue mustEqual templateToBeRendered
      val json: JsObject = Json.obj("form" -> form.fill(formData))
      jsonCaptor.getValue must containJson(commonJson ++ json)
    }

    "redirect to the next page when valid data is submitted" in {
      when(mockUserAnswersCacheConnector.save(any(), any())(any(), any())).thenReturn(Future.successful(Json.obj()))
      val request: FakeRequest[AnyContentAsFormUrlEncoded] = fakeRequest.withFormUrlEncodedBody("value" -> phone)

      val getData = new FakeDataRetrievalAction(Some(userAnswers))
      val result: Future[Result] = controller(getData).onSubmit(NormalMode)(request)

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(onwardRoute.url)
      verify(mockUserAnswersCacheConnector, times(1))
        .save(any(), any())(any(), any())
    }

    "return a Bad Request and errors when invalid data is submitted" in {
      val request: FakeRequest[AnyContentAsFormUrlEncoded] = fakeRequest.withFormUrlEncodedBody("value" -> "invalid value")
      val getData = new FakeDataRetrievalAction(Some(userAnswers))

      val result: Future[Result] = controller(getData).onSubmit(NormalMode)(request)
      val boundForm = form.bind(Map("value" -> "invalid value"))

      status(result) mustBe BAD_REQUEST
      verify(mockRenderer, times(1)).render(templateCaptor.capture(), jsonCaptor.capture())(any())
      templateCaptor.getValue mustEqual templateToBeRendered

      val json: JsObject = Json.obj("form" -> Json.toJson(boundForm))

      jsonCaptor.getValue must containJson(commonJson ++ json)
      verify(mockUserAnswersCacheConnector, times(0))
        .save(any(), any())(any(), any())
    }
  }
}
