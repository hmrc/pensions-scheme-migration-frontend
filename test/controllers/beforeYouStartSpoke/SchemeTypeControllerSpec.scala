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

package controllers.beforeYouStartSpoke

import controllers.ControllerSpecBase
import controllers.actions._
import forms.beforeYouStart.SchemeTypeFormProvider
import identifiers.beforeYouStart.SchemeTypeId
import matchers.JsonMatchers
import models.{Scheme, SchemeType}
import org.mockito.ArgumentMatchers.any
import org.scalatest.{BeforeAndAfterEach, TryValues}
import org.mockito.Mockito.{when, verify, reset, times}
import play.api.Application
import play.api.libs.json.Json
import play.api.mvc.{AnyContentAsFormUrlEncoded, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import utils.Data.ua
import utils.{Data, FakeNavigator}
import views.html.beforeYouStart.SchemeTypeView

import scala.concurrent.Future

class SchemeTypeControllerSpec extends ControllerSpecBase
  with JsonMatchers
  with TryValues
  with BeforeAndAfterEach {

  private val mutableFakeDataRetrievalAction: MutableFakeDataRetrievalAction = new MutableFakeDataRetrievalAction()
  private val application: Application = applicationBuilderMutableRetrievalAction(mutableFakeDataRetrievalAction).build()

  private val formProvider: SchemeTypeFormProvider = new SchemeTypeFormProvider()
  private val form = formProvider()

  private val valuesValid: Map[String, Seq[String]] = Map(
    "schemeType" -> Seq("single")
  )

  override def beforeEach(): Unit = {
    reset(
      mockUserAnswersCacheConnector
    )
  }

  private def controller(
                          dataRetrievalAction: DataRetrievalAction
                        ): SchemeTypeController =
    new SchemeTypeController(
      messagesApi = messagesApi,
      navigator = new FakeNavigator(desiredRoute = onwardCall),
      authenticate = new FakeAuthAction(),
      getData = dataRetrievalAction,
      requireData = new DataRequiredActionImpl,
      formProvider = formProvider,
      controllerComponents = controllerComponents,
      userAnswersCacheConnector = mockUserAnswersCacheConnector,
      schemeTypeView = app.injector.instanceOf[SchemeTypeView]
    )

  private def httpPathGET: String = routes.SchemeTypeController.onPageLoad.url
  private def httpPathPOST: String = routes.SchemeTypeController.onSubmit.url

  "SchemeTypeController" must {

    "return OK and the correct view for a GET" in {
      val getData = new FakeDataRetrievalAction(Some(ua))

      val result: Future[Result] = controller(getData).onPageLoad(fakeDataRequest(ua))

      val view = app.injector.instanceOf[SchemeTypeView].apply(
        form,
        Data.schemeName,
        routes.SchemeTypeController.onSubmit,
        SchemeType.radios(form)
      )(fakeRequest, messages)

      status(result) mustBe OK
      contentAsString(result) mustBe view.toString
    }

    "populate the view correctly on a GET when the question has previously been answered" in {
      val answers = ua.set(SchemeTypeId, SchemeType.SingleTrust).success.value
      val getData = new FakeDataRetrievalAction(Some(answers))

      val result: Future[Result] = controller(getData).onPageLoad(fakeDataRequest(answers))

      status(result) mustBe OK
      contentAsString(result) must include(messages("messages__scheme_type__title"))
      contentAsString(result) must include("single")
    }

    "redirect to the next page when valid data is submitted" in {
      when(mockUserAnswersCacheConnector.save(any(), any())(any(), any())).thenReturn(Future.successful(Json.obj()))
      val request: FakeRequest[AnyContentAsFormUrlEncoded] = fakeRequest.withFormUrlEncodedBody("schemeType.type" -> "single")

      val getData = new FakeDataRetrievalAction(Some(ua))
      val result: Future[Result] = controller(getData).onSubmit(request)

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(onwardCall.url)
      verify(mockUserAnswersCacheConnector, times(1)).save(any(), any())(any(), any())
    }


    "return a Bad Request and errors when invalid data is submitted" in {
      val request: FakeRequest[AnyContentAsFormUrlEncoded] = fakeRequest.withFormUrlEncodedBody("schemeType" -> "")
      val getData = new FakeDataRetrievalAction(Some(ua))

      val result: Future[Result] = controller(getData).onSubmit(request)

      status(result) mustBe BAD_REQUEST
      contentAsString(result) must include(messages("messages__scheme_type__title"))
      contentAsString(result) must include(messages("messages__scheme_type__error__required"))
      verify(mockUserAnswersCacheConnector, times(0)).save(any(), any())(any(), any())
    }

    "redirect to Session Expired page for a GET when there is no data" in {
      mutableFakeDataRetrievalAction.setDataToReturn(None)

      val result = route(application, httpGETRequest(httpPathGET)).value

      status(result) mustEqual SEE_OTHER

      redirectLocation(result).value mustBe controllers.preMigration.routes.ListOfSchemesController.onPageLoad(Scheme).url
    }

    "redirect back to list of schemes for a POST when there is no data" in {
      mutableFakeDataRetrievalAction.setDataToReturn(None)

      val result = route(application, httpPOSTRequest(httpPathPOST, valuesValid)).value

      status(result) mustEqual SEE_OTHER

      redirectLocation(result).value mustBe controllers.preMigration.routes.ListOfSchemesController.onPageLoad(Scheme).url
    }
  }
}