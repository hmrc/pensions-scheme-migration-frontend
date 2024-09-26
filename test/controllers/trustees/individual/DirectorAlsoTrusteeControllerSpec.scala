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

package controllers.trustees.individual

import controllers.ControllerSpecBase
import controllers.actions.{DataRequiredActionImpl, DataRetrievalAction, FakeAuthAction, FakeDataRetrievalAction}
import forms.dataPrefill.DataPrefillRadioFormProvider
import identifiers.trustees.individual.TrusteeNameId
import matchers.JsonMatchers
import models.PersonName
import models.prefill.IndividualDetails
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.scalatest.{BeforeAndAfterEach, TryValues}
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.{AnyContentAsFormUrlEncoded, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.twirl.api.Html
import renderer.Renderer
import services.DataPrefillService
import uk.gov.hmrc.nunjucks.NunjucksSupport
import utils.Data.ua
import utils.{Data, FakeNavigator, UserAnswers}

import scala.concurrent.Future
class DirectorAlsoTrusteeControllerSpec extends ControllerSpecBase
  with NunjucksSupport
  with JsonMatchers
  with TryValues
  with BeforeAndAfterEach {

  private val personName: PersonName = PersonName("Jane", "Doe")
  private val formProvider: DataPrefillRadioFormProvider = new DataPrefillRadioFormProvider()
  private val form = formProvider("")

  private val userAnswers: UserAnswers = ua.set(TrusteeNameId(0), personName).success.value
  private val templateToBeRendered: String = "dataPrefillRadio.njk"

  private val commonJson: JsObject =
    Json.obj(
      "form" -> form,
      "schemeName" -> Data.schemeName
    )
  override def beforeEach(): Unit = {
    reset(
      mockRenderer,
      mockUserAnswersCacheConnector,
      mockDataPrefillService
    )
    when(mockRenderer.render(any(), any())(any())).thenReturn(Future.successful(Html("")))
    when(mockDataPrefillService.getListOfDirectorsToBeCopied(any())).thenReturn(Nil)
  }

  private def controller(
                          dataRetrievalAction: DataRetrievalAction
                        ): DirectorAlsoTrusteeController =
    new DirectorAlsoTrusteeController(
      messagesApi = messagesApi,
      navigator = new FakeNavigator(desiredRoute = onwardCall),
      authenticate = new FakeAuthAction(),
      getData = dataRetrievalAction,
      requireData = new DataRequiredActionImpl,
      formProvider = formProvider,
      dataPrefillService = mockDataPrefillService,
      config = appConfig,
      controllerComponents = controllerComponents,
      userAnswersCacheConnector = mockUserAnswersCacheConnector,
      renderer = new Renderer(mockAppConfig, mockRenderer)
    )

  private val templateCaptor : ArgumentCaptor[String] = ArgumentCaptor.forClass(classOf[String])
  private val jsonCaptor: ArgumentCaptor[JsObject] = ArgumentCaptor.forClass(classOf[JsObject])

  "DirectorAlsoTrusteeController" must {
    "return OK and the correct view for a GET" in {
      when(mockDataPrefillService.getListOfDirectorsToBeCopied(any())).thenReturn(Seq(IndividualDetails("", "", false, None, None, 0, true, None)))
      val getData = new FakeDataRetrievalAction(Some(userAnswers))

      val result: Future[Result] = controller(getData).onPageLoad(0)(fakeDataRequest(userAnswers))

      status(result) mustBe OK

      verify(mockRenderer, times(1)).render(templateCaptor.capture(), jsonCaptor.capture())(any())
      templateCaptor.getValue mustEqual templateToBeRendered
      val json: JsObject = Json.obj("form" -> form)
      jsonCaptor.getValue must containJson(commonJson ++ json)
    }

    "redirect to task list page for a GET when there are no directors to be copied" in {
      when(mockDataPrefillService.getListOfDirectorsToBeCopied(any())).thenReturn(Nil)
      val getData = new FakeDataRetrievalAction(Some(userAnswers))

      val result: Future[Result] = controller(getData).onPageLoad(0)(fakeDataRequest(userAnswers))

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(controllers.routes.TaskListController.onPageLoad.url)
    }

    "copy the directors and redirect to the next page when valid data is submitted with value less than max trustees" in {
      when(mockDataPrefillService.copyAllDirectorsToTrustees(any(), any(), any())).thenReturn(ua)
      when(mockUserAnswersCacheConnector.save(any(), any())(any(), any())).thenReturn(Future.successful(Json.obj()))
      val request: FakeRequest[AnyContentAsFormUrlEncoded] = fakeRequest.withFormUrlEncodedBody("value" -> "0")

      val getData = new FakeDataRetrievalAction(Some(userAnswers))
      val result: Future[Result] = controller(getData).onSubmit(0)(request)

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(onwardCall.url)
      verify(mockUserAnswersCacheConnector, times(1)).save(any(), any())(any(), any())
      verify(mockDataPrefillService, times(1)).copyAllDirectorsToTrustees(any(), any(), any())
    }

    "don't copy the directors and redirect to the next page when the value is none of the above" in {
      when(mockUserAnswersCacheConnector.save(any(), any())(any(), any())).thenReturn(Future.successful(Json.obj()))
      val request: FakeRequest[AnyContentAsFormUrlEncoded] = fakeRequest.withFormUrlEncodedBody("value" -> "-1")

      val getData = new FakeDataRetrievalAction(Some(userAnswers))
      val result: Future[Result] = controller(getData).onSubmit(0)(request)

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(onwardCall.url)
      verify(mockUserAnswersCacheConnector, times(1)).save(any(), any())(any(), any())
      verify(mockDataPrefillService, never).copyAllDirectorsToTrustees(any(), any(), any())
    }

    "return a Bad Request and errors when invalid data is submitted" in {
      val request: FakeRequest[AnyContentAsFormUrlEncoded] = fakeRequest.withFormUrlEncodedBody("value" -> "invalid value")
      val getData = new FakeDataRetrievalAction(Some(userAnswers))

      val result: Future[Result] = controller(getData).onSubmit(0)(request)
      val boundForm = form.bind(Map("value" -> "invalid value"))

      status(result) mustBe BAD_REQUEST
      verify(mockRenderer, times(1)).render(templateCaptor.capture(), jsonCaptor.capture())(any())
      templateCaptor.getValue mustEqual templateToBeRendered

      val json: JsObject = Json.obj("form" -> Json.toJson(boundForm))

      jsonCaptor.getValue must containJson(commonJson ++ json)
    }
  }
}
