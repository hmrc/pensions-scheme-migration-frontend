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

package controllers.trustees

import controllers.ControllerSpecBase
import controllers.actions.{DataRequiredActionImpl, DataRetrievalAction, FakeAuthAction, FakeDataRetrievalAction}
import forms.HasReferenceNumberFormProvider
import identifiers.trustees.OtherTrusteesId
import matchers.JsonMatchers
import org.mockito.ArgumentMatchers.any
import org.scalatest.{BeforeAndAfterEach, TryValues}
import play.api.data.Form
import play.api.libs.json.Json
import play.api.mvc.{AnyContentAsFormUrlEncoded, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers.{status, _}
import services.common.details.CommonHasReferenceValueService
import uk.gov.hmrc.viewmodels.Radios
import utils.Data.{schemeName, ua}
import utils.{FakeNavigator, TwirlMigration}
import views.html.HasReferenceValueWithHintView

import scala.concurrent.Future
class OtherTrusteesControllerSpec extends ControllerSpecBase
  with JsonMatchers
  with TryValues
  with BeforeAndAfterEach{


  private val formProvider: HasReferenceNumberFormProvider =
    new HasReferenceNumberFormProvider()

  private val form: Form[Boolean] =
    formProvider(messages("messages__otherTrustees__error__required"))

  override def beforeEach(): Unit = {
    reset(
      mockUserAnswersCacheConnector
    )
  }

  private def controller(
                          dataRetrievalAction: DataRetrievalAction
                        ): OtherTrusteesController =
    new OtherTrusteesController(
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

  "OtherTrusteesController" must {

    "return OK and the correct view for a GET" in{
      val getData = new FakeDataRetrievalAction(Some(ua))
      val result: Future[Result] =
        controller(getData)
          .onPageLoad(fakeDataRequest(ua))

      status(result) mustBe OK

      val view = app.injector.instanceOf[HasReferenceValueWithHintView].apply(
        form,
        schemeName,
        messages("messages__otherTrustees__title"),
        messages("messages__otherTrustees__heading"),
        TwirlMigration.toTwirlRadios(Radios.yesNo(form("value"))),
        "govuk-visually-hidden",
        Seq(messages("messages__otherTrustees__lede")),
        routes.OtherTrusteesController.onSubmit
      )(fakeRequest, messages)
      compareResultAndView(result, view)
    }

    "populate the view correctly on a GET when the question has previously been answered Yes" in {
      val userAnswers =
        ua
          .set(OtherTrusteesId, true).success.value
      val getData = new FakeDataRetrievalAction(Some(userAnswers))
      val filledFrom = form.fill(true)
      val result: Future[Result] =
        controller(getData)
          .onPageLoad(fakeDataRequest(userAnswers))

      status(result) mustBe OK

      val view = app.injector.instanceOf[HasReferenceValueWithHintView].apply(
        filledFrom,
        schemeName,
        messages("messages__otherTrustees__title"),
        messages("messages__otherTrustees__heading"),
        TwirlMigration.toTwirlRadios(Radios.yesNo(filledFrom("value"))),
        "govuk-visually-hidden",
        Seq(messages("messages__otherTrustees__lede")),
        routes.OtherTrusteesController.onSubmit
      )(fakeRequest, messages)
      compareResultAndView(result, view)
    }

    "populate the view correctly on a GET when the question has previously been answered No" in {
      val userAnswers =
        ua
          .set(OtherTrusteesId, false).success.value
      val filledFrom = form.fill(false)
      val getData = new FakeDataRetrievalAction(Some(userAnswers))

      val result: Future[Result] =
        controller(getData)
          .onPageLoad(fakeDataRequest(userAnswers))

      status(result) mustBe OK

      val view = app.injector.instanceOf[HasReferenceValueWithHintView].apply(
        filledFrom,
        schemeName,
        messages("messages__otherTrustees__title"),
        messages("messages__otherTrustees__heading"),
        TwirlMigration.toTwirlRadios(Radios.yesNo(filledFrom("value"))),
        "govuk-visually-hidden",
        Seq(messages("messages__otherTrustees__lede")),
        routes.OtherTrusteesController.onSubmit
      )(fakeRequest, messages)
      compareResultAndView(result, view)
    }

    "redirect to the next page when valid data is submitted" in {
      when(mockUserAnswersCacheConnector.save(any(), any())(any(), any()))
        .thenReturn(Future.successful(Json.obj()))

      val request: FakeRequest[AnyContentAsFormUrlEncoded] =
        fakeRequest
          .withFormUrlEncodedBody(("value", "true"))

      val getData = new FakeDataRetrievalAction(Some(ua))

      val result: Future[Result] =
        controller(getData)
          .onSubmit(request)

      status(result) mustBe SEE_OTHER

      redirectLocation(result) mustBe Some(onwardCall.url)

      verify(mockUserAnswersCacheConnector, times(1))
        .save(any(), any())(any(), any())
    }

    "return a Bad Request and errors when invalid data is submitted" in {
      val request =
        fakeRequest
          .withFormUrlEncodedBody(("value", "invalid value"))
      val getData = new FakeDataRetrievalAction(Some(ua))
      val result: Future[Result] =
        controller(getData)
          .onSubmit(request)

      status(result) mustBe BAD_REQUEST

      contentAsString(result) must include(messages("messages__otherTrustees__title"))
      contentAsString(result) must include(messages("error.summary.title"))
      contentAsString(result) must include(messages("error.boolean"))
      verify(mockUserAnswersCacheConnector, times(0))
        .save(any(), any())(any(), any())
    }
  }
}
