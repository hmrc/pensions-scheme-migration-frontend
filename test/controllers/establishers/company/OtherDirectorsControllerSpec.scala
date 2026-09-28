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

package controllers.establishers.company

import controllers.ControllerSpecBase
import controllers.actions.{DataRequiredActionImpl, DataRetrievalAction, FakeAuthAction, FakeDataRetrievalAction}
import forms.HasReferenceNumberFormProvider
import identifiers.establishers.company.OtherDirectorsId
import identifiers.establishers.company.director.DirectorNameId
import matchers.JsonMatchers
import models.{NormalMode, PersonName}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import org.scalatest.{BeforeAndAfterEach, TryValues}
import play.api.data.Form
import play.api.i18n.Messages
import play.api.libs.json.Json
import play.api.mvc.{AnyContentAsFormUrlEncoded, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers.{status, _}
import services.common.details.CommonHasReferenceValueService
import utils.Data.{schemeName, ua}
import utils.{FakeNavigator, UserAnswers}
import views.html.{HasReferenceValueView, HasReferenceValueWithHintView}

import scala.concurrent.Future

class OtherDirectorsControllerSpec extends ControllerSpecBase with JsonMatchers

  with TryValues
  with BeforeAndAfterEach{

  private val formProvider: HasReferenceNumberFormProvider =
    new HasReferenceNumberFormProvider()

  private val personName: PersonName =
    PersonName("Jane", "Doe")
  private val userAnswers: UserAnswers =
    ua.set(DirectorNameId(0,0), personName).success.value

  private val form: Form[Boolean] =
    formProvider("Select yes if you need to tell us about other directors")

  override def beforeEach(): Unit = {
    reset(
      mockUserAnswersCacheConnector
    )
  }

  private def controller(
                          dataRetrievalAction: DataRetrievalAction
                        ): OtherDirectorsController =
    new OtherDirectorsController(
      messagesApi               = messagesApi,
      authenticate              = new FakeAuthAction(),
      getData                   = dataRetrievalAction,
      requireData               = new DataRequiredActionImpl,
      formProvider              = formProvider,
      common = new CommonHasReferenceValueService(
        controllerComponents = controllerComponents,
        hasReferenceValueWithHintView = app.injector.instanceOf[HasReferenceValueWithHintView],
        hasReferenceValueView = app.injector.instanceOf[HasReferenceValueView],
        userAnswersCacheConnector = mockUserAnswersCacheConnector,
        navigator = new FakeNavigator(desiredRoute = onwardCall),
        messagesApi = messagesApi
      )
    )

  "OtherDirectorsController" must {

    "return OK and the correct view for a GET" in{
      val getData = new FakeDataRetrievalAction(Some(userAnswers))
      val result: Future[Result] =
        controller(getData)
          .onPageLoad(0, NormalMode)(fakeDataRequest(userAnswers))

      status(result) mustBe OK

      val view = app.injector.instanceOf[HasReferenceValueWithHintView].apply(
        form,
        schemeName,
        Messages("messages__otherDirectors__title"),
        Messages("messages__otherDirectors__heading"),
        utils.Radios.yesNo(form("value")),
        "govuk-visually-hidden",
        Seq(Messages("messages__otherDirectors__lede")),
        routes.OtherDirectorsController.onSubmit(0, NormalMode)
      )(fakeRequest, messages)
      compareResultAndView(result, view)
    }

    "populate the view correctly on a GET when the question has previously been answered Yes" in {
      val ua =
        userAnswers
          .set(OtherDirectorsId(0), true).success.value
      val getData = new FakeDataRetrievalAction(Some(ua))
      val filledFrom = form.fill(true)

      val result: Future[Result] =
        controller(getData)
          .onPageLoad(0,NormalMode)(fakeDataRequest(userAnswers))

      status(result) mustBe OK

      val view = app.injector.instanceOf[HasReferenceValueWithHintView].apply(
        filledFrom,
        schemeName,
        Messages("messages__otherDirectors__title"),
        Messages("messages__otherDirectors__heading"),
        utils.Radios.yesNo(filledFrom("value")),
        "govuk-visually-hidden",
        Seq(Messages("messages__otherDirectors__lede")),
        routes.OtherDirectorsController.onSubmit(0, NormalMode)
      )(fakeRequest, messages)
      compareResultAndView(result, view)
    }

    "populate the view correctly on a GET when the question has previously been answered No" in {
      val ua =
        userAnswers
          .set(OtherDirectorsId(0), false).success.value
      val getData = new FakeDataRetrievalAction(Some(ua))
      val filledFrom = form.fill(false)


      val result: Future[Result] =
        controller(getData)
          .onPageLoad(0,NormalMode)(fakeDataRequest(userAnswers))

      status(result) mustBe OK

      val view = app.injector.instanceOf[HasReferenceValueWithHintView].apply(
        filledFrom,
        schemeName,
        Messages("messages__otherDirectors__title"),
        Messages("messages__otherDirectors__heading"),
        utils.Radios.yesNo(filledFrom("value")),
        "govuk-visually-hidden",
        Seq(Messages("messages__otherDirectors__lede")),
        routes.OtherDirectorsController.onSubmit(0, NormalMode)
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
          .onSubmit(0,NormalMode)(request)

      status(result) mustBe SEE_OTHER

      redirectLocation(result) mustBe Some(onwardCall.url)

      verify(mockUserAnswersCacheConnector, times(1))
        .save(any(), any())(any(), any())
    }

    "return a Bad Request and errors when invalid data is submitted" in {
      val request =
        fakeRequest
          .withFormUrlEncodedBody(("value", "invalid value"))

      val getData = new FakeDataRetrievalAction(Some(userAnswers))
      val result: Future[Result] =
        controller(getData)
          .onSubmit(0,NormalMode)(request)

      status(result) mustBe BAD_REQUEST

      contentAsString(result) must include(messages("messages__otherDirectors__title"))
      contentAsString(result) must include(messages("error.summary.title"))
      contentAsString(result) must include(messages("error.boolean"))

      verify(mockUserAnswersCacheConnector, times(0))
        .save(any(), any())(any(), any())
    }
  }
}
