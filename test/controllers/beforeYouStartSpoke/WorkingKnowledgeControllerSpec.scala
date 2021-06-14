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

import connectors.cache.FakeUserAnswersCacheConnector
import controllers.ControllerSpecBase
import controllers.actions._
import forms.beforeYouStart.WorkingKnowledgeFormProvider
import identifiers.beforeYouStart.WorkingKnowledgeId
import play.api.data.Form
import play.api.mvc.Call
import play.api.test.Helpers._
import utils.Data.{schemeName, ua}
import utils.FakeNavigator
import views.html.beforeYouStart.workingKnowledge

class WorkingKnowledgeControllerSpec extends ControllerSpecBase {
  private def onwardRoute: Call = controllers.routes.IndexController.onPageLoad()

  private val view = injector.instanceOf[workingKnowledge]

  val formProvider = new WorkingKnowledgeFormProvider()
  val form = formProvider()

  def controller(dataRetrievalAction: DataRetrievalAction = getSchemeName): WorkingKnowledgeController =
    new WorkingKnowledgeController(
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

  private def viewAsString(form: Form[_] = form) = view(form, Some(schemeName))(fakeRequest, messages).toString

  "Working knowledge Controller" must {

    "return OK and the correct view for a GET" in {

      val result = controller().onPageLoad()(fakeRequest)

      status(result) mustBe OK
      contentAsString(result) mustBe viewAsString()
    }

    "populate the view correctly on a GET when the question has previously been answered" in {

      val validData = ua.set(WorkingKnowledgeId, true).get
      val getRelevantData = new FakeDataRetrievalAction(Some(validData))

      val result = controller(getRelevantData).onPageLoad()(fakeRequest)

      contentAsString(result) mustBe viewAsString(form.fill(true))
    }

    "redirect to the next page when valid data is submitted" in {

      val postRequest = fakeRequest.withFormUrlEncodedBody(("value", "true"))

      val result = controller().onSubmit()(postRequest)

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(onwardRoute.url)
    }

    "return a Bad Request and errors" when {

      "invalid data is submitted" in {

        val postRequest = fakeRequest.withFormUrlEncodedBody(("value", "invalid value"))
        val boundForm = form.bind(Map("value" -> "invalid value"))

        val result = controller().onSubmit()(postRequest)

        status(result) mustBe BAD_REQUEST
        contentAsString(result) mustBe viewAsString(boundForm)
      }
    }
  }
}
