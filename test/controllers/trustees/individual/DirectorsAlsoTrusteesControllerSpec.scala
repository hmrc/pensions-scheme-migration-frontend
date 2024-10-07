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
import controllers.actions.{DataRequiredActionImpl, DataRetrievalAction, FakeAuthAction, FakeDataRetrievalAction, MutableFakeDataRetrievalAction}
import forms.dataPrefill.DataPrefillCheckboxFormProvider
import identifiers.establishers.individual.EstablisherNameId
import matchers.JsonMatchers
import models.establishers.EstablisherKind
import models.{DataPrefillCheckbox, Index, PersonName}
import models.prefill.IndividualDetails
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.scalatest.{BeforeAndAfterEach, TryValues}
import play.api.Application
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.{AnyContentAsJson, Result}
import play.api.test.CSRFTokenHelper.CSRFFRequestHeader
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.twirl.api.{Html, HtmlFormat}
import renderer.Renderer
import services.DataPrefillService
import uk.gov.hmrc.nunjucks.NunjucksSupport
import utils.Data.{schemeName, ua}
import utils.{Data, FakeNavigator, TwirlMigration, UserAnswers}
import views.html.DataPrefillCheckboxView

import scala.concurrent.Future
class DirectorsAlsoTrusteesControllerSpec extends ControllerSpecBase
  with NunjucksSupport
  with JsonMatchers
  with TryValues
  with BeforeAndAfterEach {

  private val index: Index = Index(0)
  private val mutableFakeDataRetrievalAction: MutableFakeDataRetrievalAction = new MutableFakeDataRetrievalAction()
  private val application: Application = applicationBuilderMutableRetrievalAction(mutableFakeDataRetrievalAction).build()

  private val personName: PersonName = PersonName("Jane", "Doe")
  private val formProvider: DataPrefillCheckboxFormProvider = new DataPrefillCheckboxFormProvider()
  private val form = formProvider(6,"", "", "")


  private val commonJson: JsObject =
    Json.obj(
      "form" -> form,
      "schemeName" -> Data.schemeName
    )
  val request = httpGETRequest(routes.DirectorsAlsoTrusteesController.onPageLoad(0).url)

  val view = application.injector.instanceOf[DataPrefillCheckboxView]


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
                        ): DirectorsAlsoTrusteesController =
    new DirectorsAlsoTrusteesController(
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
      renderer = new Renderer(mockAppConfig, mockRenderer),
      view = view
    )


  "DirectorsAlsoTrusteesController" must {
    "return OK and the correct view for a GET" in {
      when(mockDataPrefillService.getListOfDirectorsToBeCopied(any())).thenReturn(Seq(IndividualDetails("", "", false, None, None, 0, true, None)))
      val individualName = PersonName("Jane", "Doe")
      val getData = new FakeDataRetrievalAction(Some(ua))
      val userAnswers: Option[UserAnswers] = ua.set(EstablisherNameId(0), individualName).toOption
      val seqCheckBox = DataPrefillCheckbox.checkboxes(form, Seq(IndividualDetails("", "", false, None, None, 0, true, None)))
      mutableFakeDataRetrievalAction.setDataToReturn(userAnswers)

      val request = httpGETRequest(routes.DirectorsAlsoTrusteesController.onPageLoad(index).url)
      val result: Future[Result] = controller(getData).onPageLoad(0)(request)

      val view = application.injector.instanceOf[DataPrefillCheckboxView]
        .apply(form, Data.schemeName, "messages__trustees__prefill__heading", "messages__trustees__prefill__title", seqCheckBox,
          routes.DirectorsAlsoTrusteesController.onSubmit(Index(0)))(request, messages)

      status(result) mustBe OK
      compareResultAndView(result, view)
    }

    "redirect to task list page for a GET when there are no directors to be copied" in {
      when(mockDataPrefillService.getListOfDirectorsToBeCopied(any())).thenReturn(Nil)
      val getData = new FakeDataRetrievalAction(Some(ua))

      val result: Future[Result] = controller(getData).onPageLoad(0)(fakeDataRequest(ua))

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(controllers.routes.TaskListController.onPageLoad.url)
    }

    "copy the directors and redirect to the next page when valid data is submitted with value less than max trustees" in {
      when(mockDataPrefillService.copyAllDirectorsToTrustees(any(), any(), any())).thenReturn(ua)
      when(mockDataPrefillService.getListOfDirectorsToBeCopied(any())).thenReturn(Seq(IndividualDetails("", "", false, None, None, 0, true, None)))
      when(mockUserAnswersCacheConnector.save(any(), any())(any(), any())).thenReturn(Future.successful(Json.obj()))
      val request: FakeRequest[AnyContentAsJson] = fakeRequest.withJsonBody(Json.obj("value" -> Seq("0")))

      val getData = new FakeDataRetrievalAction(Some(ua))
      val result: Future[Result] = controller(getData).onSubmit(0)(request)

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(onwardCall.url)
      verify(mockUserAnswersCacheConnector, times(1)).save(any(), any())(any(), any())
      verify(mockDataPrefillService, times(1)).copyAllDirectorsToTrustees(any(), any(), any())
    }

    "don't copy the directors and redirect to the next page when the value is none of the above" in {
      when(mockUserAnswersCacheConnector.save(any(), any())(any(), any())).thenReturn(Future.successful(Json.obj()))
      val request: FakeRequest[AnyContentAsJson] = fakeRequest.withJsonBody(Json.obj("value" -> Seq("-1")))

      val getData = new FakeDataRetrievalAction(Some(ua))
      val result: Future[Result] = controller(getData).onSubmit(0)(request)

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(onwardCall.url)
      verify(mockUserAnswersCacheConnector, times(1)).save(any(), any())(any(), any())
      verify(mockDataPrefillService, never).copyAllDirectorsToTrustees(any(), any(), any())
    }

    "return a Bad Request and errors when invalid data is submitted" in {
      val request: FakeRequest[AnyContentAsJson] = fakeRequest.withJsonBody(Json.obj("value" -> Seq("invalid")))
      val getData = new FakeDataRetrievalAction(Some(ua))

      val result: Future[Result] = controller(getData).onSubmit(0)(request)

      status(result) mustBe BAD_REQUEST
    }
  }
}