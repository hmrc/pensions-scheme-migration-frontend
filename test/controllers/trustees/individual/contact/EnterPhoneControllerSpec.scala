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

package controllers.trustees.individual.contact

import controllers.ControllerSpecBase
import controllers.actions.{DataRequiredActionImpl, DataRetrievalAction, FakeAuthAction, FakeDataRetrievalAction}
import forms.PhoneFormProvider
import identifiers.trustees.individual.TrusteeNameId
import identifiers.trustees.individual.contact.EnterPhoneId
import matchers.JsonMatchers
import models.{NormalMode, PersonName}
import org.mockito.ArgumentMatchers.any
import org.scalatest.{BeforeAndAfterEach, TryValues}
import org.mockito.Mockito._
import play.api.i18n.Messages
import play.api.libs.json.Json
import play.api.mvc.{AnyContentAsFormUrlEncoded, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.common.contact.CommonPhoneService
import utils.Data.ua
import utils.{Data, FakeNavigator, UserAnswers}
import views.html.PhoneView

import scala.concurrent.Future
class EnterPhoneControllerSpec extends ControllerSpecBase

  with JsonMatchers
  with TryValues
  with BeforeAndAfterEach {

  private val personName: PersonName = PersonName("Jane", "Doe")
  private val phone = "777"
  private val formProvider: PhoneFormProvider = new PhoneFormProvider()
  private val form = formProvider("")

  private val userAnswers: UserAnswers = ua.set(TrusteeNameId(0), personName).success.value

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
      dataUpdateService = mockDataUpdateService,
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

      val result: Future[Result] = controller(getData).onPageLoad(0, NormalMode)(fakeDataRequest(userAnswers))

      status(result) mustBe OK
      val view = app.injector.instanceOf[PhoneView].apply(
        form,
        Data.schemeName,
        personName.fullName,
        Messages("messages__individual"),
        Seq(),
        routes.EnterPhoneController.onSubmit(0, NormalMode)
      )(fakeRequest, messages)

      compareResultAndView(result, view)
    }

    "populate the view correctly on a GET when the question has previously been answered" in {
      val ua = userAnswers.set(EnterPhoneId(0), formData).success.value
      val getData = new FakeDataRetrievalAction(Some(ua))

      val result: Future[Result] =
        controller(getData)
          .onPageLoad(0, NormalMode)(fakeDataRequest(userAnswers))

      status(result) mustBe OK
      contentAsString(result) must include(messages("messages__enterPhone_pageHeading", personName.fullName))
      contentAsString(result) must include(formData)
    }

    "redirect to the next page when valid data is submitted" in {
      when(mockUserAnswersCacheConnector.save(any(), any())(any(), any())).thenReturn(Future.successful(Json.obj()))
      val request: FakeRequest[AnyContentAsFormUrlEncoded] = fakeRequest.withFormUrlEncodedBody("value" -> phone)

      val getData = new FakeDataRetrievalAction(Some(userAnswers))
      val result: Future[Result] = controller(getData).onSubmit(0, NormalMode)(request)

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(onwardCall.url)
      verify(mockUserAnswersCacheConnector, times(1))
        .save(any(), any())(any(), any())
    }

    "return a Bad Request and errors when invalid data is submitted" in {
      val request: FakeRequest[AnyContentAsFormUrlEncoded] = fakeRequest.withFormUrlEncodedBody("value" -> "invalid value")
      val getData = new FakeDataRetrievalAction(Some(userAnswers))

      val result: Future[Result] = controller(getData).onSubmit(0, NormalMode)(request)

      status(result) mustBe BAD_REQUEST
      contentAsString(result) must include(messages("messages__enterPhone_pageHeading", personName.fullName))
      contentAsString(result) must include(messages("messages__enterPhone__error_invalid"))

      verify(mockUserAnswersCacheConnector, times(0))
        .save(any(), any())(any(), any())
    }
  }
}
