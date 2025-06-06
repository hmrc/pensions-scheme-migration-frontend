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

package services.common.details

import controllers.ControllerSpecBase
import forms.UTRFormProvider
import identifiers.TypedIdentifier
import models.{NormalMode, ReferenceValue}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import play.api.data.Form
import play.api.libs.json.Json
import play.api.test.Helpers._
import services.CommonServiceSpecBase
import utils.{FakeNavigator, UserAnswers}
import views.html.{EnterReferenceValueView, EnterReferenceValueWithHintView}

import scala.concurrent.Future

class CommonEnterReferenceValueServiceSpec extends ControllerSpecBase with CommonServiceSpecBase {

  val service = new CommonEnterReferenceValueService(
    controllerComponents = controllerComponents,
    userAnswersCacheConnector = mockUserAnswersCacheConnector,
    navigator = new FakeNavigator(desiredRoute = onwardCall),
    enterReferenceValueView = app.injector.instanceOf[EnterReferenceValueView],
    enterReferenceValueWithHintView = app.injector.instanceOf[EnterReferenceValueWithHintView],
    messagesApi = messagesApi
  )

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockUserAnswersCacheConnector)
  }

  private val formProvider: UTRFormProvider = new UTRFormProvider()
  private val id: TypedIdentifier[ReferenceValue] = new TypedIdentifier[ReferenceValue] {}
  private val referenceValueForm: Form[ReferenceValue] = formProvider()
  private val userAnswers: UserAnswers = UserAnswers().setOrException(id, ReferenceValue("1234567890"))
  private val fakeRequestWithFormData = fakeRequest.withFormUrlEncodedBody("value" -> "1234567890")

  "get" should {
    "return OK and render the correct template" in {
      val updatedAnswers = userAnswers.set(id, ReferenceValue("1234567890")).success.value
      val req = fakeDataRequest(updatedAnswers, fakeRequestWithFormData)
      val testForm = req.userAnswers.get[ReferenceValue](id).fold(referenceValueForm)(referenceValueForm.fill)
      val expectedView = app.injector.instanceOf[EnterReferenceValueView].apply(
        testForm,
        "Test Scheme",
        "Test Title",
        "Test Heading",
        None,
        Seq(),
        submitCall = onwardCall
      )(req, implicitly)



      val result = service.get(
        pageTitle = "Test Title",
        pageHeading = "Test Heading",
        isPageHeading = true,
        id = id,
        form = referenceValueForm,
        schemeName = "Test Scheme",
        submitCall = onwardCall
      )(req)

      status(result) mustBe OK

      compareResultAndView(result, expectedView)
    }
  }

    "post" should {
      "return a BadRequest when form has errors" in {
        val formWithErrors = referenceValueForm.withError("value", "error.required")

        val result = service.post(
          pageTitle = "Test Title",
          pageHeading = "Test Heading",
          isPageHeading = true,
          id = id,
          form = formWithErrors,
          schemeName = "Test Scheme",
          mode = NormalMode,
          submitCall = onwardCall
        )(fakeDataRequest(userAnswers, fakeRequestWithFormData), global)

        status(result) mustBe BAD_REQUEST
      }

      "redirect to the next page on valid data submission" in {
        when(mockUserAnswersCacheConnector.save(any(), any())(any(), any())).thenReturn(Future.successful(Json.obj()))

        val result = service.post(
          pageTitle = "Test Title",
          pageHeading = "Test Heading",
          isPageHeading = true,
          id = id,
          form = referenceValueForm.bind(Map("value" -> "1234567890")),
          schemeName = "Test Scheme",
          mode = NormalMode,
          submitCall = onwardCall
        )(fakeDataRequest(userAnswers, fakeRequestWithFormData), global)

        status(result) mustBe SEE_OTHER
        redirectLocation(result) mustBe Some(onwardCall.url)
        verify(mockUserAnswersCacheConnector).save(any(), any())(any(), any())
      }
    }
}
