/*
 * Copyright 2023 HM Revenue & Customs
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
import controllers.actions.{DataRequiredActionImpl, DataRetrievalAction, FakeAuthAction, FakeDataRetrievalAction}
import forms.adviser.AdviserNameFormProvider
import identifiers.adviser.AdviserNameId
import matchers.JsonMatchers
import models.{CheckMode, NormalMode}
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.scalatest.{BeforeAndAfterEach, TryValues}
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.{AnyContentAsFormUrlEncoded, Call, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.twirl.api.Html
import renderer.Renderer
import uk.gov.hmrc.nunjucks.NunjucksSupport
import utils.Data.ua
import utils.{Data, FakeNavigator}

import scala.concurrent.Future

class AdviserNameControllerSpec extends ControllerSpecBase
  with NunjucksSupport
  with JsonMatchers
  with TryValues
  with BeforeAndAfterEach {

  private val adviserName = "test"
  private val formProvider: AdviserNameFormProvider = new AdviserNameFormProvider()
  private val form = formProvider()
  private val onwardRoute: Call = controllers.routes.IndexController.onPageLoad
  private val templateToBeRendered: String = "adviser/adviserName.njk"

  private val commonJson: JsObject = Json.obj("schemeName" -> Data.schemeName)


  override def beforeEach(): Unit = {
    reset(
      mockRenderer,
      mockUserAnswersCacheConnector
    )
    when(mockRenderer.render(any(), any())(any())).thenReturn(Future.successful(Html("")))
  }

  private def controller(
                          dataRetrievalAction: DataRetrievalAction
                        ): AdviserNameController =
    new AdviserNameController(
      messagesApi = messagesApi,
      navigator = new FakeNavigator(desiredRoute = onwardRoute),
      authenticate = new FakeAuthAction(),
      getData = dataRetrievalAction,
      requireData = new DataRequiredActionImpl,
      formProvider = formProvider,
      controllerComponents = controllerComponents,
      userAnswersCacheConnector = mockUserAnswersCacheConnector,
      renderer = new Renderer(mockAppConfig, mockRenderer)
    )

  private val templateCaptor = ArgumentCaptor.forClass(classOf[String])
  private val jsonCaptor = ArgumentCaptor.forClass(classOf[JsObject])

  "AdviserNameController" must {
    "return OK and the correct view for a GET" in {
      val getData = new FakeDataRetrievalAction(Some(ua))

      val result: Future[Result] = controller(getData).onPageLoad(NormalMode)(fakeDataRequest(ua))

      status(result) mustBe OK

      verify(mockRenderer, times(1)).render(templateCaptor.capture(), jsonCaptor.capture())(any())
      templateCaptor.getValue mustEqual templateToBeRendered
      val json: JsObject = Json.obj("form" -> form)
      jsonCaptor.getValue must containJson(commonJson ++ json)
    }

    "populate the view correctly on a GET when the question has previously been answered" in {
      val answers = ua.set(AdviserNameId, adviserName).success.value
      val getData = new FakeDataRetrievalAction(Some(answers))

      val result: Future[Result] = controller(getData).onPageLoad(CheckMode)(fakeDataRequest(answers))

      status(result) mustBe OK
      verify(mockRenderer, times(1)).render(templateCaptor.capture(), jsonCaptor.capture())(any())
      templateCaptor.getValue mustEqual templateToBeRendered
      val json: JsObject = Json.obj("form" -> form.fill(adviserName))
      jsonCaptor.getValue must containJson(commonJson ++ json)
    }

    "redirect to the next page when valid data is submitted" in {
      when(mockUserAnswersCacheConnector.save(any(), any())(any(), any())).thenReturn(Future.successful(Json.obj()))
      val request: FakeRequest[AnyContentAsFormUrlEncoded] = fakeRequest.withFormUrlEncodedBody("adviserName" -> adviserName)

      val getData = new FakeDataRetrievalAction(Some(ua))
      val result: Future[Result] = controller(getData).onSubmit(NormalMode)(request)

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(onwardRoute.url)
      verify(mockUserAnswersCacheConnector, times(1)).save(any(), any())(any(), any())
    }

    "return a Bad Request and errors when invalid data is submitted" in {
      val request: FakeRequest[AnyContentAsFormUrlEncoded] = fakeRequest.withFormUrlEncodedBody("adviserName" -> "")
      val getData = new FakeDataRetrievalAction(Some(ua))

      val result: Future[Result] = controller(getData).onSubmit(NormalMode)(request)
      val boundForm = form.bind(Map("adviserName" -> ""))

      status(result) mustBe BAD_REQUEST
      verify(mockRenderer, times(1)).render(templateCaptor.capture(), jsonCaptor.capture())(any())
      templateCaptor.getValue mustEqual templateToBeRendered

      val json: JsObject = Json.obj("form" -> Json.toJson(boundForm))

      jsonCaptor.getValue must containJson(commonJson ++ json)
      verify(mockUserAnswersCacheConnector, times(0)).save(any(), any())(any(), any())
    }
  }
}
