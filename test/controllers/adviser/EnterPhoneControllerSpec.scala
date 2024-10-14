/*
 * Copyright 2024 HM Revenue & Customs
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
import controllers.actions._
import forms.PhoneFormProvider
import identifiers.adviser.{AdviserNameId, EnterPhoneId}
import matchers.JsonMatchers
import models.NormalMode
import org.mockito.ArgumentMatchers.any
import org.scalatest.{BeforeAndAfterEach, TryValues}
import play.api.i18n.Messages
import play.api.libs.json.Json
import play.api.mvc.{AnyContentAsFormUrlEncoded, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.common.contact.CommonPhoneService
import uk.gov.hmrc.nunjucks.NunjucksSupport
import utils.Data.ua
import utils.{Data, FakeNavigator, UserAnswers}
import viewmodels.Message
import views.html.PhoneView

import scala.concurrent.Future

class EnterPhoneControllerSpec extends ControllerSpecBase

  with JsonMatchers
  with TryValues
  with BeforeAndAfterEach {

  private val advisorName: String = "test"
  private val phone = "777"
  private val formProvider: PhoneFormProvider = new PhoneFormProvider()
  private val form = formProvider(Message("messages__error__common__phone__required"),Some(Message("messages__phone__invalid")))

  private val userAnswers: UserAnswers = ua.set(AdviserNameId, advisorName).success.value

  private val formData: String = phone

  override def beforeEach(): Unit = {
    reset(
      mockUserAnswersCacheConnector
    )
  }

  private def controller(
                          dataRetrievalAction: DataRetrievalAction
                        ): EnterPhoneController =
    new EnterPhoneController(
      messagesApi = messagesApi,
      authenticate = new FakeAuthAction(),
      getData = dataRetrievalAction,
      requireData = new DataRequiredActionImpl,
      formProvider = formProvider,
      common = new CommonPhoneService(
        controllerComponents = controllerComponents,
        userAnswersCacheConnector = mockUserAnswersCacheConnector,
        navigator = new FakeNavigator(desiredRoute = onwardCall),
        phoneView = app.injector.instanceOf[PhoneView],
        messagesApi = messagesApi
      )
    )

  "EnterPhoneController" must {
    "return OK and the correct view for a GET" in {
      val getData = new FakeDataRetrievalAction(Some(userAnswers))

      val result: Future[Result] = controller(getData).onPageLoad(NormalMode)(fakeDataRequest(userAnswers))

      status(result) mustBe OK

      val view = app.injector.instanceOf[PhoneView].apply(
        form,
        Data.schemeName,
        advisorName,
        Messages("messages__pension__adviser"),
        Seq(),
        routes.EnterPhoneController.onSubmit(NormalMode)
      )(fakeRequest, messages)

      compareResultAndView(result, view)
    }

    "populate the view correctly on a GET when the question has previously been answered" in {
      val ua = userAnswers.set(EnterPhoneId, formData).success.value
      val getData = new FakeDataRetrievalAction(Some(ua))

      val result: Future[Result] =
        controller(getData)
          .onPageLoad(NormalMode)(fakeDataRequest(userAnswers))

      status(result) mustBe OK
      contentAsString(result) must include(messages("messages__enterPhone_pageHeading", advisorName))
      contentAsString(result) must include(formData)
    }

    "redirect to the next page when valid data is submitted" in {
      when(mockUserAnswersCacheConnector.save(any(), any())(any(), any())).thenReturn(Future.successful(Json.obj()))
      val request: FakeRequest[AnyContentAsFormUrlEncoded] = fakeRequest.withFormUrlEncodedBody("value" -> phone)

      val getData = new FakeDataRetrievalAction(Some(userAnswers))
      val result: Future[Result] = controller(getData).onSubmit(NormalMode)(request)

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(onwardCall.url)
      verify(mockUserAnswersCacheConnector, times(1))
        .save(any(), any())(any(), any())
    }

    "return a Bad Request and errors when invalid data is submitted" in {
      val request: FakeRequest[AnyContentAsFormUrlEncoded] = fakeRequest.withFormUrlEncodedBody("value" -> "invalid value")
      val getData = new FakeDataRetrievalAction(Some(userAnswers))

      val result: Future[Result] = controller(getData).onSubmit(NormalMode)(request)

      status(result) mustBe BAD_REQUEST
      contentAsString(result) must include(messages("messages__enterPhone_pageHeading", advisorName))
      contentAsString(result) must include(messages("messages__error__common__phone__required"))
      contentAsString(result) must include(messages("messages__phone__invalid"))
      verify(mockUserAnswersCacheConnector, times(0))
        .save(any(), any())(any(), any())
    }
  }
}
