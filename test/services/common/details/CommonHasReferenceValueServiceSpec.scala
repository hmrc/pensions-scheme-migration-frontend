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
import forms.YesNoFormProvider
import identifiers.TypedIdentifier
import models.NormalMode
import org.mockito.ArgumentMatchers.any
import play.api.data.Form
import play.api.libs.json.Json
import play.api.test.Helpers._
import services.CommonServiceSpecBase
import uk.gov.hmrc.viewmodels.Radios
import utils.Data.ua
import utils.{FakeNavigator, TwirlMigration}
import views.html.{HasReferenceValueView, HasReferenceValueWithHintView}

import scala.concurrent.Future

class CommonHasReferenceValueServiceSpec extends ControllerSpecBase with CommonServiceSpecBase {

  val service = new CommonHasReferenceValueService(
    controllerComponents = controllerComponents,
    hasReferenceValueWithHintView = app.injector.instanceOf[HasReferenceValueWithHintView],
    hasReferenceValueView = app.injector.instanceOf[HasReferenceValueView],
    userAnswersCacheConnector = mockUserAnswersCacheConnector,
    navigator = new FakeNavigator(desiredRoute = onwardCall),
    messagesApi = messagesApi
  )

  override def beforeEach(): Unit = reset(mockRenderer, mockUserAnswersCacheConnector)

  private val formProviderYesNo: YesNoFormProvider = new YesNoFormProvider()
  private val id: TypedIdentifier[Boolean] = new TypedIdentifier[Boolean] {}
  private val yesNoForm: Form[Boolean] = formProviderYesNo(messages("error.required"))

  val fakeRequestWithFormData = fakeRequest.withFormUrlEncodedBody("value" -> "true")

  "get" should {
    "return OK and render the correct template" in {

      val expectedView = app.injector.instanceOf[HasReferenceValueView].apply(
        yesNoForm,
        "Test Scheme",
        "Test Title",
        "Test Heading",
        TwirlMigration.toTwirlRadios(Radios.yesNo(yesNoForm("value"))),
        "govuk-fieldset__legend--s",
        onwardCall
      )(fakeRequest, messages)

      val result = service.get(
        pageTitle = "Test Title",
        pageHeading = "Test Heading",
        isPageHeading = true,
        id = id,
        form = yesNoForm,
        schemeName = "Test Scheme",
        submitCall = onwardCall
      )(fakeDataRequest(ua, fakeRequestWithFormData), global)


      status(result) mustBe OK

      compareResultAndView(result, expectedView)
    }
  }

  "post" should {
    "return a BadRequest when form has errors" in {
      val formWithErrors = yesNoForm.withError("value", "error.required")

      val result = service.post(
        pageTitle = "Test Title",
        pageHeading = "Test Heading",
        isPageHeading = true,
        id = id,
        form = formWithErrors,
        schemeName = "Test Scheme",
        mode = NormalMode,
        submitCall = onwardCall
      )(fakeDataRequest(ua, fakeRequestWithFormData), global)

      status(result) mustBe BAD_REQUEST
    }

    "redirect to the next page on valid data submission" in {

      when(mockUserAnswersCacheConnector.save(any(), any())(any(), any())).thenReturn(Future.successful(Json.obj()))

      val result = service.post(
        pageTitle = "Test Title",
        pageHeading = "Test Heading",
        isPageHeading = true,
        id = id,
        form = yesNoForm,
        schemeName = "Test Scheme",
        mode = NormalMode,
        submitCall = onwardCall
      )(fakeDataRequest(ua, fakeRequestWithFormData), global)

      // Check if the response is a redirect (303)
      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(onwardCall.url)

      // Verify that the save method was called on the UserAnswersCacheConnector
      verify(mockUserAnswersCacheConnector).save(any(), any())(any(), any())
    }
  }
}
