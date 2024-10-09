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
import controllers.actions.{DataRequiredActionImpl, DataRetrievalAction, FakeAuthAction, FakeDataRetrievalAction}
import forms.dataPrefill.DataPrefillRadioFormProvider
import identifiers.establishers.company.CompanyDetailsId
import matchers.JsonMatchers
import models.prefill.IndividualDetails
import models.{CompanyDetails, DataPrefillRadio, entities}
import org.mockito.ArgumentMatchers.any
import org.scalatest.{BeforeAndAfterEach, TryValues}
import play.api.libs.json.Json
import play.api.mvc.{AnyContentAsFormUrlEncoded, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.nunjucks.NunjucksSupport
import utils.Data.ua
import utils.{Data, FakeNavigator, TwirlMigration, UserAnswers}
import views.html.DataPrefillRadioView

import scala.concurrent.Future

class TrusteeAlsoDirectorControllerSpec extends ControllerSpecBase
  with NunjucksSupport
  with JsonMatchers
  with TryValues
  with BeforeAndAfterEach {

  private val companyDetails: CompanyDetails = CompanyDetails("test company")
  private val formProvider: DataPrefillRadioFormProvider = new DataPrefillRadioFormProvider()
  private val form = formProvider("")
  private val seqTrustee: Seq[IndividualDetails] = Seq(IndividualDetails("Jane", "Doe", false, None, None, 0, true, None))

  private val userAnswers: UserAnswers = ua.set(CompanyDetailsId(0), companyDetails).success.value
  override def beforeEach(): Unit = {
    reset(
      mockUserAnswersCacheConnector,
      mockDataPrefillService
    )
    when(mockDataPrefillService.getListOfTrusteesToBeCopied(any())(any())).thenReturn(Nil)
  }

  private def controller(
                          dataRetrievalAction: DataRetrievalAction
                        ): TrusteeAlsoDirectorController =
    new TrusteeAlsoDirectorController(
      messagesApi = messagesApi,
      navigator = new FakeNavigator(desiredRoute = onwardCall),
      authenticate = new FakeAuthAction(),
      getData = dataRetrievalAction,
      requireData = new DataRequiredActionImpl,
      formProvider = formProvider,
      dataPrefillService = mockDataPrefillService,
      controllerComponents = controllerComponents,
      userAnswersCacheConnector = mockUserAnswersCacheConnector,
      dataPrefillRadioView = app.injector.instanceOf[DataPrefillRadioView]
    )

  "TrusteeAlsoDirectorController" must {
    "return OK and the correct view for a GET" in {
      when(mockDataPrefillService.getListOfTrusteesToBeCopied(any())(any())).thenReturn(seqTrustee)
      val getData = new FakeDataRetrievalAction(Some(userAnswers))

      val result: Future[Result] = controller(getData).onPageLoad(0)(fakeDataRequest(userAnswers))

      status(result) mustBe OK

      val view = app.injector.instanceOf[DataPrefillRadioView].apply(
        form,
        messages("messages__directors__prefill__title"),
        messages("messages__directors__prefill__heading", companyDetails.companyName),
        TwirlMigration.toTwirlRadios(DataPrefillRadio.radios(form, seqTrustee)),
        Data.schemeName,
        routes.TrusteeAlsoDirectorController.onSubmit(0)
      )(fakeRequest, messages)
      compareResultAndView(result, view)
    }

    "redirect to spoke task list page for a GET when there are no trustees to be copied" in {
      when(mockDataPrefillService.getListOfTrusteesToBeCopied(any())(any())).thenReturn(Nil)
      val getData = new FakeDataRetrievalAction(Some(userAnswers))

      val result: Future[Result] = controller(getData).onPageLoad(0)(fakeDataRequest(userAnswers))

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(controllers.common.routes.SpokeTaskListController.onPageLoad(0, entities.Establisher, entities.Company).url)
    }

    "copy the directors and redirect to the next page when valid data is submitted with valid index" in {
      when(mockDataPrefillService.copyAllTrusteesToDirectors(any(), any(), any())).thenReturn(ua)
      when(mockUserAnswersCacheConnector.save(any(), any())(any(), any())).thenReturn(Future.successful(Json.obj()))
      val request: FakeRequest[AnyContentAsFormUrlEncoded] = fakeRequest.withFormUrlEncodedBody("value" -> "0")

      val getData = new FakeDataRetrievalAction(Some(userAnswers))
      val result: Future[Result] = controller(getData).onSubmit(0)(request)

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(onwardCall.url)
      verify(mockUserAnswersCacheConnector, times(1)).save(any(), any())(any(), any())
    }

    "don't copy the directors and redirect to the next page when the value is none of the above" in {
      when(mockUserAnswersCacheConnector.save(any(), any())(any(), any())).thenReturn(Future.successful(Json.obj()))
      val request: FakeRequest[AnyContentAsFormUrlEncoded] = fakeRequest.withFormUrlEncodedBody("value" -> "-1")

      val getData = new FakeDataRetrievalAction(Some(userAnswers))
      val result: Future[Result] = controller(getData).onSubmit(0)(request)

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(onwardCall.url)
      verify(mockUserAnswersCacheConnector, times(1)).save(any(), any())(any(), any())
      verify(mockDataPrefillService, never).copyAllTrusteesToDirectors(any(), any(), any())
    }

    "return a Bad Request and errors when invalid data is submitted" in {
      val request: FakeRequest[AnyContentAsFormUrlEncoded] = fakeRequest.withFormUrlEncodedBody("value" -> "invalid value")
      val getData = new FakeDataRetrievalAction(Some(userAnswers))

      val result: Future[Result] = controller(getData).onSubmit(0)(request)

      status(result) mustBe BAD_REQUEST
    }
  }
}
