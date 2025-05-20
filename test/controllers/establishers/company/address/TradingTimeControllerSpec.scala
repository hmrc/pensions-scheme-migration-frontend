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

package controllers.establishers.company.address

import connectors.AddressLookupConnector
import controllers.ControllerSpecBase
import controllers.actions.MutableFakeDataRetrievalAction
import forms.address.TradingTimeFormProvider
import identifiers.beforeYouStart.SchemeNameId
import identifiers.establishers.company.CompanyDetailsId
import identifiers.establishers.company.address.TradingTimeId
import matchers.JsonMatchers
import models.{NormalMode, Scheme}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceableModule
import play.api.libs.json.Json
import play.api.mvc.Results.{BadRequest, Ok}
import play.api.mvc.{Result, Results}
import play.api.test.Helpers._
import services.common.address.CommonTradingTimeService
import utils.Data.{schemeName, ua}
import utils.{Data, Enumerable, UserAnswers}
import views.html.address.TradingTimeView

import scala.concurrent.Future

class TradingTimeControllerSpec extends ControllerSpecBase with JsonMatchers with Enumerable.Implicits {

  private val mockAddressLookupConnector = mock[AddressLookupConnector]
  private val mockCommonTradingTimeService = mock[CommonTradingTimeService]

  val extraModules: Seq[GuiceableModule] = Seq(
    bind[AddressLookupConnector].toInstance(mockAddressLookupConnector),
    bind[CommonTradingTimeService].toInstance(mockCommonTradingTimeService)
  )
  private val mutableFakeDataRetrievalAction: MutableFakeDataRetrievalAction = new MutableFakeDataRetrievalAction()

  override def fakeApplication(): Application = applicationBuilderMutableRetrievalAction(mutableFakeDataRetrievalAction, extraModules).build()

  private val formProvider: TradingTimeFormProvider = new TradingTimeFormProvider()
  private val form = formProvider("partnershipTradingTime.error.required")
  private val mode = NormalMode
  private val index = 0

  private val userAnswers: Option[UserAnswers] = Some(ua.setOrException(CompanyDetailsId(index), Data.companyDetails))
  private val httpPathGET: String = controllers.establishers.company.address.routes.TradingTimeController.onPageLoad(index,mode).url
  private val httpPathPOST: String = controllers.establishers.company.address.routes.TradingTimeController.onSubmit(index,mode).url

  private val valuesValid: Map[String, Seq[String]] = Map(
    "value" -> Seq("true")
  )

  private val valuesInvalid: Map[String, Seq[String]] = Map(
    "value" -> Seq.empty
  )
  val request = httpGETRequest(httpPathGET)

  override def beforeEach(): Unit = {
    super.beforeEach()
  }

  "TradingTime Controller" must {

    "Return OK and the correct view for a GET" in {
      val ua: UserAnswers = UserAnswers()
        .setOrException(SchemeNameId, Data.schemeName)
        .setOrException(CompanyDetailsId(index), Data.companyDetails)
      mutableFakeDataRetrievalAction.setDataToReturn(Some(ua))

      val view = app.injector.instanceOf[TradingTimeView]
      val expectedView = view(
        form, "entityType", "entityName",
        utils.Radios.yesNo(form("value")),
        Some(schemeName),
        submitUrl = controllers.trustees.partnership.address.routes.TradingTimeController.onSubmit(index, mode)
      )(fakeRequest, messages)

      when(mockCommonTradingTimeService.get(any(), any(), any(), any(), any(), any())(any()))
        .thenReturn(Future.successful(Ok(expectedView)))

      val result: Future[Result] = route(app, httpGETRequest(httpPathGET)).value

      status(result) mustEqual OK
      compareResultAndView(result, expectedView)
    }

    "return OK and the correct view for a GET when the question has previously been answered" in {
      val ua: UserAnswers = UserAnswers()
        .setOrException(SchemeNameId, Data.schemeName)
        .setOrException(CompanyDetailsId(index), Data.companyDetails)
        .setOrException(TradingTimeId(index), true)

      mutableFakeDataRetrievalAction.setDataToReturn(Some(ua))

      val result: Future[Result] = route(app, httpGETRequest(httpPathGET)).value

      status(result) mustEqual OK
    }

    "redirect to Session Expired page for a GET when there is no data" in {
      val ua: UserAnswers = UserAnswers()

      mutableFakeDataRetrievalAction.setDataToReturn(Some(ua))

      val result: Future[Result] = route(app, request).value

      status(result) mustEqual SEE_OTHER

      redirectLocation(result).value mustBe controllers.routes.SessionExpiredController.onPageLoad().absoluteURL()(request)
    }

    "Save data to user answers and redirect to next page when valid data is submitted" in {

      when(mockUserAnswersCacheConnector.save(any(), any())(any(), any()))
        .thenReturn(Future.successful(Json.obj()))
      when(mockCommonTradingTimeService.post(any(), any(), any(), any(), any(), any(), any())(any(), any(), any()))
        .thenReturn(Future.successful(Results.SeeOther(onwardCall.url)))

      mutableFakeDataRetrievalAction.setDataToReturn(userAnswers)

      val result = route(app, httpPOSTRequest(httpPathPOST, valuesValid)).value

      status(result) mustEqual SEE_OTHER
      redirectLocation(result) mustBe Some(onwardCall.url)
    }

    "return a BAD REQUEST when invalid data is submitted" in {
      mutableFakeDataRetrievalAction.setDataToReturn(userAnswers)
      when(mockCommonTradingTimeService.post(any(), any(), any(), any(), any(), any(), any())(any(), any(), any()))
        .thenReturn(Future.successful(BadRequest))

      val result = route(app, httpPOSTRequest(httpPathPOST, valuesInvalid)).value

      status(result) mustEqual BAD_REQUEST

      verify(mockUserAnswersCacheConnector, times(0)).save(any(), any())(any(), any())
    }

    "redirect back to list of schemes for a POST when there is no data" in {
      mutableFakeDataRetrievalAction.setDataToReturn(None)

      val result = route(app, httpPOSTRequest(httpPathPOST, valuesValid)).value

      status(result) mustEqual SEE_OTHER

      redirectLocation(result).value mustBe controllers.preMigration.routes.ListOfSchemesController.onPageLoad(Scheme).url
    }
  }

}
