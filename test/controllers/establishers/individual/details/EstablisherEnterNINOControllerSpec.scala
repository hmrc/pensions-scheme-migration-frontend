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

package controllers.establishers.individual.details

import controllers.ControllerSpecBase
import controllers.actions.{DataRequiredActionImpl, DataRetrievalAction, FakeAuthAction, FakeDataRetrievalAction}
import forms.NINOFormProvider
import identifiers.establishers.individual.EstablisherNameId
import identifiers.establishers.individual.details.EstablisherNINOId
import matchers.JsonMatchers
import models.{NormalMode, PersonName, ReferenceValue}
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.scalatest.{BeforeAndAfterEach, TryValues}
import play.api.data.Form
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.{AnyContentAsFormUrlEncoded, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.twirl.api.Html
import renderer.Renderer
import services.common.details.CommonEnterReferenceValueService
import uk.gov.hmrc.nunjucks.NunjucksSupport
import utils.Data.ua
import utils.{FakeNavigator, UserAnswers}
import views.html.{EnterReferenceValueView, EnterReferenceValueWithHintView}

import scala.concurrent.Future

class EstablisherEnterNINOControllerSpec
  extends ControllerSpecBase
    with NunjucksSupport
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
    ua.set(EstablisherNameId(0), personName).success.value

//  private val commonJson: JsObject =
//    Json.obj(
//      "pageTitle"     -> "What is the individual’s National Insurance number?",
//      "pageHeading"     -> "What is Jane Doe’s National Insurance number?",
//      "schemeName"    -> "Test scheme name",
//      "legendClass"   -> "govuk-label--xl",
//      "isPageHeading" -> true
//    )
  private val formData: ReferenceValue =
    ReferenceValue(value = "AB123456C")

  override def beforeEach(): Unit = {
    reset(
      mockRenderer,
      mockUserAnswersCacheConnector
    )
  }

  private def controller(
                          dataRetrievalAction: DataRetrievalAction
                        ): EstablisherEnterNINOController =
    new EstablisherEnterNINOController(
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

  "EstablisherEnterNINOController" must {
    "return OK and the correct view for a GET" in {
      val getData = new FakeDataRetrievalAction(Some(userAnswers))

      val result: Future[Result] =
        controller(getData)
          .onPageLoad(0, NormalMode)(fakeDataRequest(userAnswers))

      status(result) mustBe OK

      val view = app.injector.instanceOf[EnterReferenceValueWithHintView].apply(
        form = form,
        schemeName = "Test scheme name",
        pageTitle = "What is the individual’s National Insurance number?",
        pageHeading = "What is Jane Doe’s National Insurance number?",
        legendClass = "govuk-label--xl",
        paragraphs = Seq(),
        hintText = Some("For example, QQ 12 34 56 C"),
        submitCall= routes.EstablisherEnterNINOController.onSubmit(0, NormalMode)
      )(fakeRequest, messages)
      compareResultAndView(result, view)

    }

    "populate the view correctly on a GET when the question has previously been answered" in {
      val ua =
        userAnswers
          .set(EstablisherNINOId(0), formData).success.value

      val getData = new FakeDataRetrievalAction(Some(ua))

      val result: Future[Result] =
        controller(getData)
          .onPageLoad(0, NormalMode)(fakeDataRequest(userAnswers))

      status(result) mustBe OK

      contentAsString(result) must include(messages("messages__enterNINO_title", "the individual"))
      contentAsString(result) must include(formData.value)
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
          .onSubmit(0, NormalMode)(request)

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
          .onSubmit(0, NormalMode)(request)

      status(result) mustBe BAD_REQUEST

      contentAsString(result) must include(messages("messages__enterNINO_title", "the individual"))
      contentAsString(result) must include(messages("messages__error__common_nino_invalid", personName.fullName))

      verify(mockUserAnswersCacheConnector, times(0))
        .save(any(), any())(any(), any())
    }
  }
}
