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

package controllers.establishers.company.director

import controllers.ControllerSpecBase
import controllers.actions._
import forms.dataPrefill.DataPrefillCheckboxFormProvider
import identifiers.establishers.company.CompanyDetailsId
import matchers.JsonMatchers
import models._
import models.prefill.IndividualDetails
import org.mockito.ArgumentMatchers.any
import org.scalatest.{BeforeAndAfterEach, TryValues}
import org.mockito.Mockito._
import play.api.Application
import play.api.i18n.Messages
import play.api.libs.json.Json
import play.api.mvc.{AnyContentAsJson, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import utils.Data.ua
import utils.{Data, FakeNavigator, UserAnswers}
import views.html.DataPrefillCheckboxView

import scala.concurrent.Future

class TrusteesAlsoDirectorsControllerSpec extends ControllerSpecBase

  with JsonMatchers
  with TryValues
  with BeforeAndAfterEach {


  private val index: Index = Index(0)
  private val mutableFakeDataRetrievalAction: MutableFakeDataRetrievalAction = new MutableFakeDataRetrievalAction()
  private val application: Application = applicationBuilderMutableRetrievalAction(mutableFakeDataRetrievalAction).build()
  private val formProvider: DataPrefillCheckboxFormProvider = new DataPrefillCheckboxFormProvider()
  private val form = formProvider(6,"", "", "")
  private val companyDetails: CompanyDetails = CompanyDetails("test company")
  private val userAnswerss: UserAnswers = ua.set(CompanyDetailsId(0), companyDetails).success.value
  val view = application.injector.instanceOf[DataPrefillCheckboxView]


  override def beforeEach(): Unit = {
    reset(mockUserAnswersCacheConnector)
    reset(mockDataPrefillService)

    when(mockDataPrefillService.getListOfTrusteesToBeCopied(any)(any)).thenReturn(Nil)

  }

  private def controller(
                          dataRetrievalAction: DataRetrievalAction
                        ): TrusteesAlsoDirectorsController =
    new TrusteesAlsoDirectorsController(
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
      view = view
    )


  "TrusteesAlsoDirectorsController" must {
    "return OK and the correct view for a GET" in {
      when(mockDataPrefillService.getListOfTrusteesToBeCopied(any)(any)).thenReturn(Seq(IndividualDetails("", "", false, None, None, 0, true, None)))

      val getData = new FakeDataRetrievalAction(Some(userAnswerss))
      val seqCheckBox = DataPrefillCheckbox.checkboxes(form, Seq(IndividualDetails("", "", false, None, None, 0, true, None)))
      mutableFakeDataRetrievalAction.setDataToReturn(Some(userAnswerss))

      val request = httpGETRequest(controllers.establishers.company.director.routes.TrusteesAlsoDirectorsController.onPageLoad(index).url)
      val result: Future[Result] = controller(getData).onPageLoad(0)(request)

      val view = application.injector.instanceOf[DataPrefillCheckboxView]
        .apply(form, Data.schemeName, Messages("messages__directors__prefill__heading", companyDetails.companyName), "messages__directors__prefill__title", seqCheckBox,
          controllers.establishers.company.director.routes.TrusteesAlsoDirectorsController.onSubmit(Index(0)))(request, messages)
      verify(mockDataPrefillService, times(1)).getListOfTrusteesToBeCopied(any())(any())


      status(result) mustBe OK
      compareResultAndView(result, view)
    }

    "redirect to spoke task list page for a GET when there are no trustees to be copied" in {
      when(mockDataPrefillService.getListOfTrusteesToBeCopied(any())(any())).thenReturn(Nil)
      val getData = new FakeDataRetrievalAction(Some(userAnswerss))

      val result: Future[Result] = controller(getData).onPageLoad(0)(fakeDataRequest(userAnswerss))

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(controllers.common.routes.SpokeTaskListController.onPageLoad(0, entities.Establisher, entities.Company).url)
    }

    "copy the directors and redirect to the next page when valid data is submitted with value less than max directors" in {
      when(mockDataPrefillService.copyAllTrusteesToDirectors(any(), any(), any())).thenReturn(userAnswerss)
      when(mockUserAnswersCacheConnector.save(any(), any())(any(), any())).thenReturn(Future.successful(Json.obj()))
      val request: FakeRequest[AnyContentAsJson] = fakeRequest.withJsonBody(Json.obj("value" -> Seq("0")))

      val getData = new FakeDataRetrievalAction(Some(userAnswerss))
      val result: Future[Result] = controller(getData).onSubmit(0)(request)

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(onwardCall.url)
      verify(mockUserAnswersCacheConnector, times(1)).save(any(), any())(any(), any())
      verify(mockDataPrefillService, times(1)).copyAllTrusteesToDirectors(any(), any(), any())
    }

    "don't copy the trustees and redirect to the next page when the value is none of the above" in {
      when(mockUserAnswersCacheConnector.save(any(), any())(any(), any())).thenReturn(Future.successful(Json.obj()))
      val request: FakeRequest[AnyContentAsJson] = fakeRequest.withJsonBody(Json.obj("value" -> Seq("-1")))

      val getData = new FakeDataRetrievalAction(Some(userAnswerss))
      val result: Future[Result] = controller(getData).onSubmit(0)(request)

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(onwardCall.url)
      verify(mockUserAnswersCacheConnector, times(1)).save(any(), any())(any(), any())
      verify(mockDataPrefillService, never).copyAllTrusteesToDirectors(any(), any(), any())
    }

    "return a Bad Request and errors when invalid data is submitted" in {
      when(mockDataPrefillService.getListOfTrusteesToBeCopied(any())(any())).thenReturn(Nil)
      val request: FakeRequest[AnyContentAsJson] = fakeRequest.withJsonBody(Json.obj("value" -> Seq("invalid")))
      val getData = new FakeDataRetrievalAction(Some(userAnswerss))

      val result: Future[Result] = controller(getData).onSubmit(0)(request)

      status(result) mustBe BAD_REQUEST

    }
  }
}
