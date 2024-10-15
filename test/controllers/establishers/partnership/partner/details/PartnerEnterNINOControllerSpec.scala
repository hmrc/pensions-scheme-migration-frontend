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

package controllers.establishers.partnership.partner.details

import controllers.ControllerSpecBase
import controllers.actions.{DataRequiredActionImpl, DataRetrievalAction, FakeAuthAction, FakeDataRetrievalAction}
import forms.NINOFormProvider
import identifiers.establishers.partnership.partner.PartnerNameId
import identifiers.establishers.partnership.partner.details.PartnerNINOId
import matchers.JsonMatchers
import models.{NormalMode, PersonName, ReferenceValue}
import org.mockito.ArgumentMatchers.any
import org.scalatest.{BeforeAndAfterEach, TryValues}
import play.api.data.Form
import play.api.libs.json.Json
import play.api.mvc.{AnyContentAsFormUrlEncoded, Request, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.common.details.CommonEnterReferenceValueService
import utils.Data.ua
import utils.{FakeNavigator, UserAnswers}
import viewmodels.Message
import views.html.{EnterReferenceValueView, EnterReferenceValueWithHintView}

import scala.concurrent.Future

class PartnerEnterNINOControllerSpec
  extends ControllerSpecBase

    with JsonMatchers
    with TryValues
    with BeforeAndAfterEach {

  private val personName: PersonName =
    PersonName("Jane", "Doe")
  private val formProvider: NINOFormProvider =
    new NINOFormProvider()
  private val form: Form[ReferenceValue] =
    formProvider(personName.fullName)
  private val userAnswers: UserAnswers =
    ua.set(PartnerNameId(0,0), personName).success.value

  private def getView(req: Request[_], form: Form[_]) = {
    app.injector.instanceOf[EnterReferenceValueWithHintView].apply(
      form,
      "Test scheme name",
      "What is the partner’s National Insurance number?",
      "What is Jane Doe’s National Insurance number?",
      "govuk-label--xl",
      Seq(),
      Some(Message("messages__enterNINO__hint")),
      routes.PartnerEnterNINOController.onSubmit(0, 0, NormalMode)
    )(req, implicitly)
  }

  private val formData: ReferenceValue =
    ReferenceValue(value = "AB123456C")

  override def beforeEach(): Unit = {
    reset(
      mockUserAnswersCacheConnector
    )
  }

  private def controller(
                          dataRetrievalAction: DataRetrievalAction
                        ): PartnerEnterNINOController =
    new PartnerEnterNINOController(
      messagesApi               = messagesApi,
      authenticate              = new FakeAuthAction(),
      getData                   = dataRetrievalAction,
      requireData               = new DataRequiredActionImpl,
      formProvider              = formProvider,
      common = new CommonEnterReferenceValueService(
        controllerComponents = controllerComponents,
        userAnswersCacheConnector = mockUserAnswersCacheConnector,
        navigator = new FakeNavigator(desiredRoute = onwardCall),
        enterReferenceValueView = app.injector.instanceOf[EnterReferenceValueView],
        enterReferenceValueWithHintView = app.injector.instanceOf[EnterReferenceValueWithHintView],
        messagesApi = messagesApi
      )
    )

  "PartnerEnterNINOController" must {
    "return OK and the correct view for a GET" in {
      val getData = new FakeDataRetrievalAction(Some(userAnswers))

      val request = fakeDataRequest(userAnswers)

      val result: Future[Result] =
        controller(getData)
          .onPageLoad(0,0, NormalMode)(request)

      status(result) mustBe OK
      compareResultAndView(result, getView(request, form))
    }

    "populate the view correctly on a GET when the question has previously been answered" in {
      val ua =
        userAnswers
          .set(PartnerNINOId(0,0), formData).success.value

      val getData = new FakeDataRetrievalAction(Some(ua))

      val req = fakeDataRequest(userAnswers)
      val result: Future[Result] =
        controller(getData)
          .onPageLoad(0,0, NormalMode)(req)

      val testForm = form.fill(formData)
      status(result) mustBe OK
      compareResultAndView(result, getView(req, testForm))
    }

    "redirect to the next page when valid data is submitted" in {
      when(mockUserAnswersCacheConnector.save(any(), any())(any(), any()))
        .thenReturn(Future.successful(Json.obj()))

      val request: FakeRequest[AnyContentAsFormUrlEncoded] =
        fakeRequest
          .withFormUrlEncodedBody("value" -> "AB123456C")

      val getData = new FakeDataRetrievalAction(Some(userAnswers))

      val result: Future[Result] =
        controller(getData)
          .onSubmit(0,0, NormalMode)(request)

      status(result) mustBe SEE_OTHER

      redirectLocation(result) mustBe Some(onwardCall.url)

      verify(mockUserAnswersCacheConnector, times(1))
        .save(any(), any())(any(), any())
    }

    "return a Bad Request and errors when invalid data is submitted" in {
      val request: FakeRequest[AnyContentAsFormUrlEncoded] =
        fakeRequest
          .withFormUrlEncodedBody("value" -> "invalid value")

      val getData = new FakeDataRetrievalAction(Some(userAnswers))

      val result: Future[Result] =
        controller(getData)
          .onSubmit(0,0, NormalMode)(request)

      val boundForm = form.bind(Map("value" -> "invalid value"))

      status(result) mustBe BAD_REQUEST
      compareResultAndView(result, getView(request, boundForm))
      verify(mockUserAnswersCacheConnector, times(0))
        .save(any(), any())(any(), any())
    }
  }
}
