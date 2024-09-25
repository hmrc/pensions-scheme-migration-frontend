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
import forms.DOBFormProvider
import identifiers.TypedIdentifier
import identifiers.beforeYouStart.SchemeNameId
import identifiers.establishers.individual.EstablisherNameId
import models.{NormalMode, PersonName}
import org.mockito.ArgumentMatchers.any
import play.api.data.Form
import play.api.libs.json.Json
import play.api.test.Helpers._
import play.twirl.api.Html
import renderer.Renderer
import services.CommonServiceSpecBase
import utils.{Data, FakeNavigator, UserAnswers}

import java.time.LocalDate
import scala.concurrent.Future

class CommonDateOfBirthServiceSpec extends ControllerSpecBase with CommonServiceSpecBase {

  // Instantiate service
  val service = new CommonDateOfBirthService(
    controllerComponents = controllerComponents,
    renderer = new Renderer(mockAppConfig, mockRenderer),
    userAnswersCacheConnector = mockUserAnswersCacheConnector,
    navigator = new FakeNavigator(desiredRoute = onwardCall),
    messagesApi = messagesApi
  )

  override def beforeEach(): Unit = reset(mockRenderer, mockUserAnswersCacheConnector)

  // Define the form provider for the date of birth form
  private val formProvider: DOBFormProvider = new DOBFormProvider()
  private val dobId: TypedIdentifier[LocalDate] = new TypedIdentifier[LocalDate] {}
  private val personNameId: TypedIdentifier[PersonName] = EstablisherNameId(0)
  private val dobForm: Form[LocalDate] = formProvider()
  val userAnswers: UserAnswers = UserAnswers().setOrException(SchemeNameId, Data.schemeName)
    .setOrException(personNameId, PersonName("John", "Doe"))
  val fakeRequestWithFormData = fakeRequest.withFormUrlEncodedBody("date.day" -> "01", "date.month" -> "01", "date.year" -> "1990")

  "get" should {
    "return OK and render the correct template" in {
      when(mockRenderer.render(any(), any())(any())).thenReturn(Future.successful(Html("")))

      val result = service.get(
        form = dobForm,
        dobId = dobId,
        personNameId = personNameId,
        schemeName = "Test Scheme",
        entityType = "individual"
      )(fakeDataRequest(userAnswers, fakeRequestWithFormData), global)

      status(result) mustBe OK
      verify(mockRenderer).render(any(), any())(any())
    }

    "populate the form with existing data" in {
      val updatedAnswers = userAnswers.set(dobId, LocalDate.of(1995, 1, 1)).success.value
      when(mockRenderer.render(any(), any())(any())).thenReturn(Future.successful(Html("")))

      val result = service.get(
        form = dobForm,
        dobId = dobId,
        personNameId = personNameId,
        schemeName = "Test Scheme",
        entityType = "individual"
      )(fakeDataRequest(updatedAnswers, fakeRequestWithFormData), global)

      status(result) mustBe OK
      verify(mockRenderer).render(any(), any())(any())
    }
  }

  "post" should {
    "return a BadRequest when form has errors" in {
      val formWithErrors = dobForm.withError("date", "error.required")
      when(mockRenderer.render(any(), any())(any())).thenReturn(Future.successful(Html("")))

      val result = service.post(
        form = formWithErrors,
        dobId = dobId,
        personNameId = personNameId,
        schemeName = "Test Scheme",
        entityType = "individual",
        mode = NormalMode
      )(fakeDataRequest(userAnswers, fakeRequestWithFormData), global)

      status(result) mustBe BAD_REQUEST
      verify(mockRenderer).render(any(), any())(any())
    }

    "redirect to the next page on valid data submission" in {
      when(mockUserAnswersCacheConnector.save(any(), any())(any(), any())).thenReturn(Future.successful(Json.obj()))
      when(mockRenderer.render(any(), any())(any())).thenReturn(Future.successful(Html("")))

      val result = service.post(
        form = dobForm.bind(Map("date.day" -> "01", "date.month" -> "01", "date.year" -> "1990")),
        dobId = dobId,
        personNameId = personNameId,
        schemeName = "Test Scheme",
        entityType = "individual",
        mode = NormalMode
      )(fakeDataRequest(userAnswers, fakeRequestWithFormData), global)

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(onwardCall.url)
      verify(mockUserAnswersCacheConnector).save(any(), any())(any(), any())
    }
  }
}
