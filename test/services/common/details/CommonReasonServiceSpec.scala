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

import forms.ReasonFormProvider
import identifiers.TypedIdentifier
import identifiers.beforeYouStart.SchemeNameId
import models.NormalMode
import org.mockito.ArgumentMatchers.any
import play.api.data.Form
import play.api.libs.json.Json
import play.api.test.Helpers._
import play.twirl.api.Html
import renderer.Renderer
import services.CommonServiceSpecBase
import utils.{Data, FakeNavigator, UserAnswers}

import scala.concurrent.Future

class CommonReasonServiceSpec extends CommonServiceSpecBase {

  // Instantiate service
  val service = new CommonReasonService(
    controllerComponents = controllerComponents,
    renderer = new Renderer(mockAppConfig, mockRenderer),
    userAnswersCacheConnector = mockUserAnswersCacheConnector,
    navigator = new FakeNavigator(desiredRoute = onwardCall),
    messagesApi = messagesApi
  )

  override def beforeEach(): Unit = reset(mockRenderer, mockUserAnswersCacheConnector)

  // Define the form provider for the reason form
  private val formProvider: ReasonFormProvider = new ReasonFormProvider()
  private val id: TypedIdentifier[String] = new TypedIdentifier[String] {}
  private val reasonForm: Form[String] = formProvider(messages("error.required"))
  val userAnswers: UserAnswers = UserAnswers().setOrException(SchemeNameId, Data.schemeName)
  val fakeRequestWithFormData = fakeRequest.withFormUrlEncodedBody("value" -> "true")
  "get" should {
    "return OK and render the correct template" in {
      when(mockRenderer.render(any(), any())(any())).thenReturn(Future.successful(Html("")))

      val result = service.get(
        pageTitle = "Test Title",
        pageHeading = "Test Heading",
        isPageHeading = true,
        id = id,
        form = reasonForm,
        schemeName = "Test Scheme"
      )(fakeDataRequest(userAnswers, fakeRequestWithFormData), global)

      status(result) mustBe OK
      verify(mockRenderer).render(any(), any())(any())
    }

    "populate the form with existing data" in {
      val updatedAnswers = userAnswers.set(id, "Test Reason").success.value
      when(mockRenderer.render(any(), any())(any())).thenReturn(Future.successful(Html("")))

      val result = service.get(
        pageTitle = "Test Title",
        pageHeading = "Test Heading",
        isPageHeading = true,
        id = id,
        form = reasonForm,
        schemeName = "Test Scheme"
      )(fakeDataRequest(updatedAnswers, fakeRequestWithFormData), global)

      status(result) mustBe OK
      verify(mockRenderer).render(any(), any())(any())
    }
  }

  "post" should {
    "return a BadRequest when form has errors" in {
      val formWithErrors = reasonForm.withError("value", "error.required")
      when(mockRenderer.render(any(), any())(any())).thenReturn(Future.successful(Html("")))

      val result = service.post(
        pageTitle = "Test Title",
        pageHeading = "Test Heading",
        isPageHeading = true,
        id = id,
        form = formWithErrors,
        schemeName = "Test Scheme",
        mode = NormalMode
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
        form = reasonForm.bind(Map("value" -> "Test Reason")),
        schemeName = "Test Scheme",
        mode = NormalMode
      )(fakeDataRequest(userAnswers, fakeRequestWithFormData), global)

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(onwardCall.url)
      verify(mockUserAnswersCacheConnector).save(any(), any())(any(), any())
    }
  }
}
