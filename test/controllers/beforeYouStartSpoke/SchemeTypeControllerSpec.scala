/*
 * Copyright 2021 HM Revenue & Customs
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

package controllers.beforeYouStartSpoke

import com.codahale.metrics.SharedMetricRegistries
import connectors.cache.FakeUserAnswersCacheConnector
import controllers.ControllerSpecBase
import controllers.actions._
import forms.beforeYouStart.SchemeTypeFormProvider
import identifiers.beforeYouStart.{SchemeNameId, SchemeTypeId}
import models.SchemeType
import play.api.data.Form
import play.api.mvc.Call
import play.api.test.Helpers._
import utils.{FakeNavigator, UserAnswers}
import views.html.beforeYouStart.schemeType

class SchemeTypeControllerSpec extends ControllerSpecBase {
  private def onwardRoute: Call = controllers.routes.IndexController.onPageLoad()

  private val view = injector.instanceOf[schemeType]

  val formProvider = new SchemeTypeFormProvider()
  val form = formProvider()
  val schemeName = "Test Scheme Name"

  val minData = UserAnswers().set(SchemeNameId, schemeName).get

  def controller(dataRetrievalAction: DataRetrievalAction = new FakeDataRetrievalAction(Some(minData))): SchemeTypeController =
    new SchemeTypeController(
      messagesApi,
      FakeUserAnswersCacheConnector,
      new FakeNavigator(desiredRoute = onwardRoute),
      FakeAuthAction,
      dataRetrievalAction,
      new DataRequiredActionImpl,
      formProvider,
      controllerComponents,
      view
    )

  private def viewAsString(form: Form[_] = form) = view(form, schemeName)(fakeRequest, messages).toString

  "SchemeType Controller" must {

    "return OK and the correct view for a GET" in {
      SharedMetricRegistries.clear()
      val result = controller().onPageLoad()(fakeRequest)

      status(result) mustBe OK
      contentAsString(result) mustBe viewAsString()
    }

    "populate the view correctly on a GET when the question has previously been answered" in {
      SharedMetricRegistries.clear()
      val validData = minData.set(SchemeTypeId, SchemeType.SingleTrust).get
      val getRelevantData = new FakeDataRetrievalAction(Some(validData))

      val result = controller(getRelevantData).onPageLoad()(fakeRequest)

      contentAsString(result) mustBe viewAsString(form.fill(SchemeType.SingleTrust))
    }

    "redirect to the next page when valid data is submitted" in {
      SharedMetricRegistries.clear()
      val postRequest = fakeRequest.withFormUrlEncodedBody(("schemeType.type", "single"))

      val result = controller().onSubmit()(postRequest)

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(onwardRoute.url)
    }

    "return a Bad Request and errors" when {

      "invalid data is submitted" in {
        SharedMetricRegistries.clear()
        val postRequest = fakeRequest.withFormUrlEncodedBody(("value", "invalid value"))
        val boundForm = form.bind(Map("value" -> "invalid value"))

        val result = controller().onSubmit()(postRequest)

        status(result) mustBe BAD_REQUEST
        contentAsString(result) mustBe viewAsString(boundForm)
      }

      "scheme name matches psa name" in {
        SharedMetricRegistries.clear()
        val postRequest = fakeRequest.withFormUrlEncodedBody(("value", "My PSA"))
        val boundForm = form.bind(Map("value" -> "My PSA"))

        val result = controller().onSubmit()(postRequest)

        status(result) mustBe BAD_REQUEST
        contentAsString(result) mustBe viewAsString(boundForm)
      }
    }
  }
}
