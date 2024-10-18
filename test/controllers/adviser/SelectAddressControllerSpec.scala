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

import connectors.AddressLookupConnector
import controllers.ControllerSpecBase
import controllers.actions.MutableFakeDataRetrievalAction
import forms.address.AddressListFormProvider
import identifiers.adviser.EnterPostCodeId
import identifiers.beforeYouStart.SchemeNameId
import matchers.JsonMatchers
import models.{Scheme, TolerantAddress}
import org.mockito.ArgumentMatchers.any
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceableModule
import play.api.libs.json.Json
import play.api.mvc.Results.{BadRequest, Ok}
import play.api.mvc.{Result, Results}
import play.api.test.Helpers._
import services.common.address.CommonAddressListService
import utils.{Data, Enumerable, UserAnswers}
import views.html.address.AddressListView

import scala.concurrent.Future

class SelectAddressControllerSpec extends ControllerSpecBase with JsonMatchers with Enumerable.Implicits {

  private val mockAddressLookupConnector = mock[AddressLookupConnector]
  private val mockCommonAddressListService = mock[CommonAddressListService]

  val extraModules: Seq[GuiceableModule] = Seq(
    bind[AddressLookupConnector].toInstance(mockAddressLookupConnector),
    bind[CommonAddressListService].toInstance(mockCommonAddressListService)
  )
  private val mutableFakeDataRetrievalAction: MutableFakeDataRetrievalAction = new MutableFakeDataRetrievalAction()
  override def fakeApplication(): Application = applicationBuilderMutableRetrievalAction(mutableFakeDataRetrievalAction, extraModules).build()

  private val formProvider: AddressListFormProvider = new AddressListFormProvider()
  private val form = formProvider("selectAddress.required")

  private val httpPathGET: String = routes.SelectAddressController.onPageLoad.url
  private val httpPathPOST: String = routes.SelectAddressController.onSubmit.url

  private val valuesValid: Map[String, Seq[String]] = Map(
    "value" -> Seq("1")
  )

  private val valuesInvalid: Map[String, Seq[String]] = Map(
    "value" -> Seq.empty
  )
  val request = httpGETRequest(httpPathGET)

  override def beforeEach(): Unit = {
    super.beforeEach()
  }

  val addresses = Seq(
    TolerantAddress(Some("123 Test Street"), Some("Test Town"), Some("Test County"), Some("Test Postcode"), Some("Test Country"), None),
    TolerantAddress(Some("456 Example Road"), Some("Example Town"), Some("Example County"), Some("Example Postcode"), Some("Example Country"), None)
  )

  "SelectAddress Controller" must {

    "Return OK and the correct view for a GET" in {
      val ua: UserAnswers = UserAnswers()
        .setOrException(SchemeNameId, Data.schemeName)
        .setOrException(EnterPostCodeId, addresses)

      mutableFakeDataRetrievalAction.setDataToReturn(Some(ua))

      val view = app.injector.instanceOf[AddressListView]
      val expectedView = view(
        form, "messages__pension__adviser", "messages__pension__adviser",
        convertToRadioItems(addresses),
        routes.ConfirmAddressController.onPageLoad.url,
        Data.schemeName,
        routes.SelectAddressController.onSubmit,
        h1MessageKey = "addressList.title"
      )(fakeRequest, messages)

      when(mockCommonAddressListService.get(any(), any(), any())(any(), any()))
        .thenReturn(Future.successful(Ok(expectedView)))

      val result: Future[Result] = route(app, httpGETRequest(httpPathGET)).value

      status(result) mustEqual OK
      compareResultAndView(result, expectedView)
    }

    "redirect to Session Expired page for a GET when there is no data" in {
      val ua: UserAnswers = UserAnswers()

      mutableFakeDataRetrievalAction.setDataToReturn(Some(ua))

      val result: Future[Result] = route(app, request).value

      status(result) mustEqual SEE_OTHER

      redirectLocation(result).value mustBe controllers.routes.SessionExpiredController.onPageLoad().absoluteURL()(request)
    }

    "Save data to user answers and redirect to next page when valid data is submitted" in {
      val ua: UserAnswers = Data.ua.setOrException(EnterPostCodeId, addresses)
      mutableFakeDataRetrievalAction.setDataToReturn(Some(ua))

      when(mockUserAnswersCacheConnector.save(any(), any())(any(), any()))
        .thenReturn(Future.successful(Json.obj()))
      when(mockCommonAddressListService.post(any(), any(), any(), any(), any(), any())(any(), any(), any()))
        .thenReturn(Future.successful(Results.SeeOther(onwardCall.url)))

      val result = route(app, httpPOSTRequest(httpPathPOST, valuesValid)).value

      status(result) mustEqual SEE_OTHER
      redirectLocation(result) mustBe Some(onwardCall.url)
    }

    "return a BAD REQUEST when invalid data is submitted" in {
      val ua: UserAnswers = Data.ua.setOrException(EnterPostCodeId, addresses)

      mutableFakeDataRetrievalAction.setDataToReturn(Some(ua))
      when(mockCommonAddressListService.post(any(), any(), any(), any(), any(), any())(any(), any(), any()))
        .thenReturn(Future.successful(BadRequest))

      val result = route(app, httpPOSTRequest(httpPathPOST, valuesInvalid)).value

      status(result) mustEqual BAD_REQUEST
    }

    "redirect to Session Expired page for a POST when there is no data" in {
      mutableFakeDataRetrievalAction.setDataToReturn(None)

      val result = route(app, httpPOSTRequest(httpPathPOST, valuesValid)).value

      status(result) mustEqual SEE_OTHER

      redirectLocation(result).value mustBe controllers.preMigration.routes.ListOfSchemesController.onPageLoad(Scheme).url
    }
  }
}
