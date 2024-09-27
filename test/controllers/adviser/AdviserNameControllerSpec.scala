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

package controllers.adviser

import controllers.ControllerSpecBase
import controllers.actions.{DataRequiredActionImpl, DataRetrievalAction, FakeAuthAction, FakeDataRetrievalAction, MutableFakeDataRetrievalAction}
import forms.adviser.AdviserNameFormProvider
import identifiers.adviser.AdviserNameId
import matchers.JsonMatchers
import models.{CheckMode, NormalMode}
import org.mockito.ArgumentMatchers.any
import org.scalatest.{BeforeAndAfterEach, TryValues}
import play.api.Application
import play.api.libs.json.Json
import play.api.mvc.{AnyContentAsFormUrlEncoded, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import utils.Data.ua
import utils.{Data, FakeNavigator}
import views.html.adviser.AdviserNameView

import scala.concurrent.Future

class AdviserNameControllerSpec extends ControllerSpecBase
  with JsonMatchers
  with TryValues
  with BeforeAndAfterEach {

  private val mutableFakeDataRetrievalAction: MutableFakeDataRetrievalAction = new MutableFakeDataRetrievalAction()
  private val application: Application = applicationBuilderMutableRetrievalAction(mutableFakeDataRetrievalAction).build()

  private val adviserName = "test"
  private val formProvider: AdviserNameFormProvider = new AdviserNameFormProvider()
  private val form = formProvider()
  private val mode = NormalMode

  override def beforeEach(): Unit = {
    reset(
      mockUserAnswersCacheConnector
    )
  }

  private def controller(
                          dataRetrievalAction: DataRetrievalAction
                        ): AdviserNameController =
    new AdviserNameController(
      messagesApi = messagesApi,
      navigator = new FakeNavigator(desiredRoute = onwardCall),
      authenticate = new FakeAuthAction(),
      getData = dataRetrievalAction,
      requireData = new DataRequiredActionImpl,
      formProvider = formProvider,
      controllerComponents = controllerComponents,
      userAnswersCacheConnector = mockUserAnswersCacheConnector,
      adviserNameView = app.injector.instanceOf[AdviserNameView]
    )

  "AdviserNameController" must {
    "return OK and the correct view for a GET" in {
      val getData = new FakeDataRetrievalAction(Some(ua))

      val result: Future[Result] = controller(getData).onPageLoad(NormalMode)(fakeDataRequest(ua))

      status(result) mustBe OK

      val view = app.injector.instanceOf[AdviserNameView].apply(
        form,
        Some(Data.schemeName),
        routes.AdviserNameController.onSubmit(mode),
        Some("messages__adviserName__hint")
      )(fakeRequest, messages)

      compareResultAndView(result, view)
    }

    "populate the view correctly on a GET when the question has previously been answered" in {
      val answers = ua.set(AdviserNameId, adviserName).success.value
      val getData = new FakeDataRetrievalAction(Some(answers))

      val result: Future[Result] = controller(getData).onPageLoad(CheckMode)(fakeDataRequest(answers))

      status(result) mustBe OK
      contentAsString(result) must include(messages("messages__adviserName__title"))
      contentAsString(result) must include(adviserName)
    }

    "redirect to the next page when valid data is submitted" in {
      when(mockUserAnswersCacheConnector.save(any(), any())(any(), any())).thenReturn(Future.successful(Json.obj()))
      val request: FakeRequest[AnyContentAsFormUrlEncoded] = fakeRequest.withFormUrlEncodedBody("adviserName" -> adviserName)

      val getData = new FakeDataRetrievalAction(Some(ua))
      val result: Future[Result] = controller(getData).onSubmit(NormalMode)(request)

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(onwardCall.url)
      verify(mockUserAnswersCacheConnector, times(1)).save(any(), any())(any(), any())
    }

    "return a Bad Request and errors when invalid data is submitted" in {
      val request: FakeRequest[AnyContentAsFormUrlEncoded] = fakeRequest.withFormUrlEncodedBody("adviserName" -> "")
      val getData = new FakeDataRetrievalAction(Some(ua))

      val result: Future[Result] = controller(getData).onSubmit(NormalMode)(request)

      status(result) mustBe BAD_REQUEST
      contentAsString(result) must include(messages("messages__adviserName__title"))
      contentAsString(result) must include(messages("messages__adviserName__blank"))
      verify(mockUserAnswersCacheConnector, times(0)).save(any(), any())(any(), any())
    }
  }
}