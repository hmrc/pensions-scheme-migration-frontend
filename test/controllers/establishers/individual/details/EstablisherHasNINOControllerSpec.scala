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
import forms.HasReferenceNumberFormProvider
import identifiers.establishers.individual.EstablisherNameId
import identifiers.establishers.individual.details.EstablisherHasNINOId
import matchers.JsonMatchers
import models.{NormalMode, PersonName}
import org.mockito.ArgumentMatchers.any
import org.scalatest.{BeforeAndAfterEach, TryValues}
import play.api.data.Form
import play.api.libs.json.Json
import play.api.mvc.{AnyContentAsFormUrlEncoded, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers.{status, _}
import services.common.details.CommonHasReferenceValueService
import uk.gov.hmrc.viewmodels.{NunjucksSupport, Radios}
import utils.Data.ua
import utils.{FakeNavigator, TwirlMigration, UserAnswers}
import views.html.HasReferenceValueWithHintView

import scala.concurrent.Future

class EstablisherHasNINOControllerSpec
  extends ControllerSpecBase
    with NunjucksSupport
    with JsonMatchers
    with TryValues
    with BeforeAndAfterEach {

  private val personName: PersonName =
    PersonName("Jane", "Doe")
  private val formProvider: HasReferenceNumberFormProvider =
    new HasReferenceNumberFormProvider()
  private val form: Form[Boolean] =
    formProvider("Select Yes if Jane Doe has a National Insurance number")
  private val userAnswers: UserAnswers =
    ua.set(EstablisherNameId(0), personName).success.value

  private def controller(
                          dataRetrievalAction: DataRetrievalAction
                        ): EstablisherHasNINOController =
    new EstablisherHasNINOController(
      messagesApi               = messagesApi,
      authenticate              = new FakeAuthAction(),
      getData                   = dataRetrievalAction,
      requireData               = new DataRequiredActionImpl,
      formProvider              = formProvider,
      common = new CommonHasReferenceValueService(
        controllerComponents = controllerComponents,
        hasReferenceValueWithHintView = app.injector.instanceOf[HasReferenceValueWithHintView],
        userAnswersCacheConnector = mockUserAnswersCacheConnector,
        navigator = new FakeNavigator(desiredRoute = onwardCall),
        messagesApi = messagesApi
      )
    )

  override def beforeEach(): Unit = {
    reset(
      mockUserAnswersCacheConnector
    )
  }

  "EstablisherHasNINOController" must {
    "return OK and the correct view for a GET" in {
      val getData = new FakeDataRetrievalAction(Some(userAnswers))

      val result: Future[Result] =
        controller(getData)
          .onPageLoad(0, NormalMode)(fakeDataRequest(userAnswers))

      status(result) mustBe OK

      val view = app.injector.instanceOf[HasReferenceValueWithHintView].apply(
        form,
        "Test scheme name",
        "Does the individual have a National Insurance number?",
        "Does Jane Doe have a National Insurance number?",
        TwirlMigration.toTwirlRadios(Radios.yesNo(form("value"))),
        "govuk-label--xl",
        Seq(),
        routes.EstablisherHasNINOController.onSubmit(0, NormalMode)
      )(fakeRequest, messages)
      compareResultAndView(result, view)
    }

    "populate the view correctly on a GET when the question has previously been answered" in {
      val ua =
        userAnswers
          .set(EstablisherHasNINOId(0), true).success.value
      val getData = new FakeDataRetrievalAction(Some(ua))
      val filledFrom = form.fill(true)

      val result: Future[Result] =
        controller(getData)
          .onPageLoad(0, NormalMode)(fakeDataRequest(userAnswers))

      status(result) mustBe OK

      val view = app.injector.instanceOf[HasReferenceValueWithHintView].apply(
        filledFrom,
        "Test scheme name",
        "Does the individual have a National Insurance number?",
        "Does Jane Doe have a National Insurance number?",
        TwirlMigration.toTwirlRadios(Radios.yesNo(filledFrom("value"))),
        "govuk-label--xl",
        Seq(),
        routes.EstablisherHasNINOController.onSubmit(0, NormalMode)
      )(fakeRequest, messages)
      compareResultAndView(result, view)
    }

    "redirect to the next page when valid data is submitted" in {
      when(mockUserAnswersCacheConnector.save(any(), any())(any(), any()))
        .thenReturn(Future.successful(Json.obj()))

      val request: FakeRequest[AnyContentAsFormUrlEncoded] =
        fakeRequest
          .withFormUrlEncodedBody(("value", "true"))

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
          .withFormUrlEncodedBody(("value", "invalid value"))

      val getData = new FakeDataRetrievalAction(Some(userAnswers))

      val result: Future[Result] =
        controller(getData)
          .onSubmit(0, NormalMode)(request)

      status(result) mustBe BAD_REQUEST

      contentAsString(result) must include( "Does the individual have a National Insurance number?")
      contentAsString(result) must include(messages("error.summary.title"))
      contentAsString(result) must include(messages("error.boolean"))

      verify(mockUserAnswersCacheConnector, times(0))
        .save(any(), any())(any(), any())
    }
  }
}
