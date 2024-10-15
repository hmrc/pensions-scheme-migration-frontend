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
import controllers.trustees.individual.details.routes
import forms.DOBFormProvider
import identifiers.TypedIdentifier
import identifiers.beforeYouStart.SchemeNameId
import identifiers.establishers.individual.EstablisherNameId
import models.{Index, NormalMode, PersonName}
import org.mockito.ArgumentMatchers.any
import play.api.data.Form
import play.api.libs.json.Json
import play.api.test.Helpers._
import services.CommonServiceSpecBase
import uk.gov.hmrc.viewmodels.DateInput
import utils.{Data, FakeNavigator, UserAnswers}

import java.time.LocalDate
import scala.concurrent.Future

class CommonDateOfBirthServiceSpec extends ControllerSpecBase with CommonServiceSpecBase {

  // Instantiate service


  override def beforeEach(): Unit = reset(mockUserAnswersCacheConnector)

  // Define the form provider for the date of birth form
  private val minDate: LocalDate = LocalDate.of(2020,2, 1)
  private val formProvider: DOBFormProvider = new DOBFormProvider()
  private val dobId: TypedIdentifier[LocalDate] = new TypedIdentifier[LocalDate] {}
  private val personNameId: TypedIdentifier[PersonName] = EstablisherNameId(0)
  private val form: Form[LocalDate] = formProvider()
  val userAnswers: UserAnswers = UserAnswers().setOrException(SchemeNameId, Data.schemeName)
    .setOrException(personNameId, PersonName("John", "Doe"))
  val fakeRequestWithFormData = fakeRequest.withFormUrlEncodedBody("date.day" -> "01", "date.month" -> "01", "date.year" -> "1990")

  val view = app.injector.instanceOf[views.html.DobView]
  val service = new CommonDateOfBirthService(
    controllerComponents = controllerComponents,
    dobView = view,
    userAnswersCacheConnector = mockUserAnswersCacheConnector,
    navigator = new FakeNavigator(desiredRoute = onwardCall),
    messagesApi = messagesApi
  )


  "get" should {
    "return OK and render the correct template" in {

      val expectedView = view(
        form,
        DateInput.localDate(form("date")),
        "John Doe",
        "Test Scheme",
        "individual",
        routes.TrusteeDOBController.onSubmit(Index(0), NormalMode)
      )(fakeRequestWithFormData, messages)

      val result = service.get(
        form = form,
        dobId = dobId,
        personNameId = personNameId,
        schemeName = "Test Scheme",
        entityType = "individual",
        routes.TrusteeDOBController.onSubmit(Index(0), NormalMode)
      )(fakeDataRequest(userAnswers, fakeRequestWithFormData), global)

      status(result) mustBe OK
      compareResultAndView(result, expectedView)
    }

    "populate the form with existing data" in {
      val dateFilled = LocalDate.of(1995, 1, 1)
      val updatedAnswers = userAnswers.set(dobId, dateFilled).success.value

      val expectedView = view(
        form.fill(dateFilled),
        DateInput.localDate(form("date")),
        "John Doe",
        "Test Scheme",
        "individual",
        routes.TrusteeDOBController.onSubmit(Index(0), NormalMode)
      )(fakeRequestWithFormData, messages)

      val result = service.get(
        form = form,
        dobId = dobId,
        personNameId = personNameId,
        schemeName = "Test Scheme",
        entityType = "individual",
        routes.TrusteeDOBController.onSubmit(Index(0), NormalMode)
      )(fakeDataRequest(updatedAnswers, fakeRequestWithFormData), global)

      status(result) mustBe OK
      compareResultAndView(result, expectedView)
    }
  }

  "post" should {
    "return a BadRequest when form has errors" in {
      val formWithErrors = form.withError("date", "error.required")

      val result = service.post(
        form = formWithErrors,
        dobId = dobId,
        personNameId = personNameId,
        schemeName = "Test Scheme",
        entityType = "individual",
        mode = NormalMode,
        call = routes.TrusteeDOBController.onSubmit(Index(0), NormalMode)
      )(fakeDataRequest(userAnswers, fakeRequestWithFormData), global)

      status(result) mustBe BAD_REQUEST
    }

    "redirect to the next page on valid data submission" in {
      when(mockUserAnswersCacheConnector.save(any(), any())(any(), any())).thenReturn(Future.successful(Json.obj()))


      val result = service.post(
        form = form.bind(Map("date.day" -> "01", "date.month" -> "01", "date.year" -> "1990")),
        dobId = dobId,
        personNameId = personNameId,
        schemeName = "Test Scheme",
        entityType = "individual",
        mode = NormalMode,
        None,
        routes.TrusteeDOBController.onSubmit(Index(0), NormalMode)
      )(fakeDataRequest(userAnswers, fakeRequestWithFormData), global)

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(onwardCall.url)
      verify(mockUserAnswersCacheConnector).save(any(), any())(any(), any())
    }
  }
}
