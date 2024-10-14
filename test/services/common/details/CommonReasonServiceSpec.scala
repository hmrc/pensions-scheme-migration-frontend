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
import controllers.actions.MutableFakeDataRetrievalAction
import forms.ReasonFormProvider
import identifiers.TypedIdentifier
import identifiers.beforeYouStart.SchemeNameId
import models.NormalMode
import org.mockito.ArgumentMatchers.any
import play.api.Application
import play.api.data.Form
import play.api.inject.bind
import play.api.inject.guice.GuiceableModule
import play.api.libs.json.Json
import play.api.test.Helpers._
import play.twirl.api.Html
import renderer.Renderer
import services.CommonServiceSpecBase
import utils.{Data, FakeNavigator, UserAnswers}
import views.html.ReasonView

import scala.concurrent.Future

class CommonReasonServiceSpec extends ControllerSpecBase with CommonServiceSpecBase {

  private val mutableFakeDataRetrievalAction: MutableFakeDataRetrievalAction = new MutableFakeDataRetrievalAction()
  private val mockCommonReasonService = mock[CommonReasonService]
  val extraModules: Seq[GuiceableModule] = Seq(
    bind[CommonReasonService].to(mockCommonReasonService)
  )
  private val application: Application = applicationBuilderMutableRetrievalAction(mutableFakeDataRetrievalAction, extraModules).build()
  val fakeRequestWithFormData = fakeRequest.withFormUrlEncodedBody("value" -> "true")

  val view = application.injector.instanceOf[ReasonView]

  // Instantiate service
  val service = new CommonReasonService(
    controllerComponents = controllerComponents,
    userAnswersCacheConnector = mockUserAnswersCacheConnector,
    navigator = new FakeNavigator(desiredRoute = onwardCall),
    messagesApi = messagesApi,
    view
  )

  override def beforeEach(): Unit = reset(mockUserAnswersCacheConnector)

  // Define the form provider for the reason form
  private val formProvider: ReasonFormProvider = new ReasonFormProvider()
  private val id: TypedIdentifier[String] = new TypedIdentifier[String] {}
  private val reasonForm: Form[String] = formProvider(messages("error.required"))
  val userAnswers: UserAnswers = UserAnswers().setOrException(SchemeNameId, Data.schemeName)



  "get" should {
    "return OK and render the correct template" in {


      val result = service.get(
        pageTitle = "Test Title",
        pageHeading = "Test Heading",
        isPageHeading = true,
        id = id,
        form = reasonForm,
        schemeName = "Test Scheme",
        submitUrl = onwardCall
      )(fakeDataRequest(userAnswers, fakeRequestWithFormData), global)

      val expectedView = view.apply(
        pageTitle = "Test Title",
        pageHeading = "Test Heading",
        isPageHeading = true,
        reasonForm,
        schemeName = "Test Scheme",
        submitUrl = onwardCall
      )(fakeDataRequest(userAnswers), messages)

      status(result) mustBe OK
      compareResultAndView(result, expectedView)
    }

    "populate the form with existing data" in {
      val updatedAnswers = userAnswers.set(id, "Test Reason").success.value
      mutableFakeDataRetrievalAction.setDataToReturn(Some(updatedAnswers))



      val result = service.get(
        pageTitle = "Test Title",
        pageHeading = "Test Heading",
        isPageHeading = true,
        id = id,
        form = reasonForm,
        schemeName = "Test Scheme",
        submitUrl = onwardCall
      )(fakeDataRequest(updatedAnswers, fakeRequestWithFormData), global)

      val filledForm = reasonForm.bind(Map("value" -> "Test Reason"))

      val expectedView = view.apply(
        pageTitle = "Test Title",
        pageHeading = "Test Heading",
        isPageHeading = true,
        filledForm,
        schemeName = "Test Scheme",
        submitUrl = onwardCall
      )(fakeDataRequest(userAnswers), messages)

      status(result) mustBe OK
      compareResultAndView(result, expectedView)
    }
  }

  "post" should {
    "return a BadRequest when form has errors" in {

      val filledForm = reasonForm.bind(Map("value" -> ""))

      val result = service.post(
        pageTitle = "Test Title",
        pageHeading = "Test Heading",
        isPageHeading = true,
        id = id,
        form = filledForm,
        schemeName = "Test Scheme",
        mode = NormalMode,
        submitUrl = onwardCall
      )(fakeDataRequest(userAnswers, fakeRequestWithFormData), global)

      status(result) mustBe BAD_REQUEST
      verify(mockUserAnswersCacheConnector, times(0)).save(any(), any())(any(), any())
    }

    "redirect to the next page on valid data submission" in {
      when(mockUserAnswersCacheConnector.save(any(), any())(any(), any())).thenReturn(Future.successful(Json.obj()))

      val result = service.post(
        pageTitle = "Test Title",
        pageHeading = "Test Heading",
        isPageHeading = true,
        id = id,
        form = reasonForm.bind(Map("value" -> "Test Reason")),
        schemeName = "Test Scheme",
        mode = NormalMode,
        submitUrl = onwardCall
      )(fakeDataRequest(userAnswers, fakeRequestWithFormData), global)

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(onwardCall.url)
      verify(mockUserAnswersCacheConnector).save(any(), any())(any(), any())
    }
  }
}
