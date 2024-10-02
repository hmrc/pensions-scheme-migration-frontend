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

import forms.UTRFormProvider
import identifiers.TypedIdentifier
import models.{NormalMode, ReferenceValue}
import org.mockito.ArgumentMatchers.any
import play.api.data.Form
import play.api.libs.json.Json
import play.api.test.Helpers._
import play.twirl.api.Html
import renderer.Renderer
import services.CommonServiceSpecBase
import utils.{FakeNavigator, UserAnswers}
import views.html.{EnterReferenceValueView, EnterReferenceValueWithHintView}

import scala.concurrent.Future

class CommonEnterReferenceValueServiceSpec extends CommonServiceSpecBase {

  val service = new CommonEnterReferenceValueService(
    controllerComponents = controllerComponents,
    userAnswersCacheConnector = mockUserAnswersCacheConnector,
    navigator = new FakeNavigator(desiredRoute = onwardCall),
    enterReferenceValueView = app.injector.instanceOf[EnterReferenceValueView],
    enterReferenceValueWithHintView = app.injector.instanceOf[EnterReferenceValueWithHintView],
    messagesApi = messagesApi
  )

  override def beforeEach(): Unit = reset(mockRenderer, mockUserAnswersCacheConnector)

  private val formProvider: UTRFormProvider = new UTRFormProvider()
  private val id: TypedIdentifier[ReferenceValue] = new TypedIdentifier[ReferenceValue] {}
  private val referenceValueForm: Form[ReferenceValue] = formProvider()
  val userAnswers: UserAnswers = UserAnswers().setOrException(id, ReferenceValue("1234567890"))
  val fakeRequestWithFormData = fakeRequest.withFormUrlEncodedBody("value" -> "1234567890")

  "get" should {
    "return OK and render the correct template" in {
      val result = service.get(
        pageTitle = "Test Title",
        pageHeading = "Test Heading",
        isPageHeading = true,
        id = id,
        form = referenceValueForm,
        schemeName = "Test Scheme",
        submitCall = onwardCall
      )(fakeDataRequest(userAnswers, fakeRequestWithFormData), global)

      status(result) mustBe OK
      verify(mockRenderer).render(any(), any())(any())
    }

    "populate the form with existing data" in {
      val updatedAnswers = userAnswers.set(id, ReferenceValue("1234567890")).success.value
      when(mockRenderer.render(any(), any())(any())).thenReturn(Future.successful(Html("")))

      val result = service.get(
        pageTitle = "Test Title",
        pageHeading = "Test Heading",
        isPageHeading = true,
        id = id,
        form = referenceValueForm,
        schemeName = "Test Scheme",
        submitCall = onwardCall
      )(fakeDataRequest(updatedAnswers, fakeRequestWithFormData), global)

      status(result) mustBe OK
      verify(mockRenderer).render(any(), any())(any())
    }
  }

  "post" should {
    "return a BadRequest when form has errors" in {
      val formWithErrors = referenceValueForm.withError("value", "error.required")
      when(mockRenderer.render(any(), any())(any())).thenReturn(Future.successful(Html("")))

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
      verify(mockRenderer).render(any(), any())(any())
    }

    "redirect to the next page on valid data submission" in {
      when(mockUserAnswersCacheConnector.save(any(), any())(any(), any())).thenReturn(Future.successful(Json.obj()))
      when(mockRenderer.render(any(), any())(any())).thenReturn(Future.successful(Html("")))

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
