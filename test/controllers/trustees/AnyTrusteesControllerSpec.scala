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
import forms.YesNoFormProvider
import identifiers.trustees.AnyTrusteesId
import matchers.JsonMatchers
import org.mockito.ArgumentMatchers.any
import org.scalatest.{BeforeAndAfterEach, TryValues}
import play.api.data.Form
import play.api.libs.json.Json
import play.api.mvc.{AnyContentAsFormUrlEncoded, Request, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers.{status, _}
import uk.gov.hmrc.govukfrontend.views.viewmodels.radios.RadioItem
import uk.gov.hmrc.nunjucks.NunjucksSupport
import uk.gov.hmrc.viewmodels.Radios
import utils.Data.{schemeName, ua}
import utils.{FakeNavigator, TwirlMigration}

import scala.concurrent.Future
class AnyTrusteesControllerSpec extends ControllerSpecBase

  with JsonMatchers
  with TryValues
  with BeforeAndAfterEach{


  private val formProvider: YesNoFormProvider =
    new YesNoFormProvider()

  private val form: Form[Boolean] =
    formProvider(messages("messages__otherTrustees__error__required"))

  private def getView(req: Request[_], form: Form[_], radios: Seq[RadioItem]) = {
    app.injector.instanceOf[views.html.trustees.AnyTrusteesView].apply(
      form,
      controllers.trustees.routes.AnyTrusteesController.onSubmit,
      messages("messages__the_scheme"),
      schemeName,
      radios
    )(req, implicitly)
  }

  override def beforeEach(): Unit = {
    reset(
      mockUserAnswersCacheConnector
    )
  }

  private def controller(
                          dataRetrievalAction: DataRetrievalAction
                        ): AnyTrusteesController =
    new AnyTrusteesController(
      messagesApi               = messagesApi,
      navigator                 = new FakeNavigator(desiredRoute = onwardCall),
      authenticate              = new FakeAuthAction(),
      getData                   = dataRetrievalAction,
      requireData               = new DataRequiredActionImpl,
      formProvider              = formProvider,
      controllerComponents      = controllerComponents,
      userAnswersCacheConnector = mockUserAnswersCacheConnector,
      view = app.injector.instanceOf[views.html.trustees.AnyTrusteesView]
    )

  "AnyTrusteesController" must {

    "return OK and the correct view for a GET" in{

      val getData = new FakeDataRetrievalAction(Some(ua))

      val req = fakeDataRequest(ua)
      val result: Future[Result] =
        controller(getData)
          .onPageLoad(req)

      status(result) mustBe OK
      compareResultAndView(result, getView(req, form, TwirlMigration.toTwirlRadios(Radios.yesNo(form("value")))))
    }

    "populate the view correctly on a GET when the question has previously been answered Yes" in {
      val userAnswers =
        ua
          .set(AnyTrusteesId, true).success.value

      val getData = new FakeDataRetrievalAction(Some(userAnswers))

      val req = fakeDataRequest(userAnswers)
      val result: Future[Result] =
        controller(getData)
          .onPageLoad(req)

      val testForm = form.fill(true)

      status(result) mustBe OK
      compareResultAndView(result, getView(req, testForm, TwirlMigration.toTwirlRadios(
        Radios.yesNo(testForm.apply("value"))
      )))
    }

    "populate the view correctly on a GET when the question has previously been answered No" in {
      val userAnswers =
        ua
          .set(AnyTrusteesId, false).success.value

      val getData = new FakeDataRetrievalAction(Some(userAnswers))

      val req = fakeDataRequest(userAnswers)
      val result: Future[Result] =
        controller(getData)
          .onPageLoad(req)

      val testForm = form.fill(false)

      status(result) mustBe OK
      compareResultAndView(result, getView(req, testForm, TwirlMigration.toTwirlRadios(
        Radios.yesNo(testForm.apply("value"))
      )))
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

      val boundForm = form.bind(Map("value" -> "invalid value"))

      status(result) mustBe BAD_REQUEST
      compareResultAndView(result, getView(request, boundForm, TwirlMigration.toTwirlRadios(Radios.yesNo(boundForm.apply("value")))))

      verify(mockUserAnswersCacheConnector, times(0))
        .save(any(), any())(any(), any())
    }


  }

}
