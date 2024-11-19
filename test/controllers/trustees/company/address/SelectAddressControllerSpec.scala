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

package controllers.trustees.company.address

import connectors.AddressLookupConnector
import controllers.ControllerSpecBase
import controllers.actions.MutableFakeDataRetrievalAction
import controllers.establishers.individual.address.routes
import forms.address.AddressListFormProvider
import identifiers.trustees.company.address.EnterPostCodeId
import matchers.JsonMatchers
import models.{NormalMode, Scheme, TolerantAddress}
import org.mockito.ArgumentMatchers.any
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceableModule
import play.api.libs.json.Json
import play.api.mvc.Results.{BadRequest, Ok}
import play.api.mvc.{Result, Results}
import play.api.test.Helpers._
import services.common.address.CommonAddressListService
import utils.Data.schemeName
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
  private val mode = NormalMode
  private val index = 0

  private val httpPathGET: String = controllers.trustees.company.address.routes.SelectAddressController.onPageLoad(index,mode).url
  private val httpPathPOST: String = controllers.trustees.company.address.routes.SelectAddressController.onSubmit(index,mode).url

  private val seqAddresses = Seq(
    TolerantAddress(Some("1"),Some("1"),Some("c"),Some("d"), Some("zz11zz"), Some("GB")),
    TolerantAddress(Some("2"),Some("2"),Some("c"),Some("d"), Some("zz11zz"), Some("GB"))
  )

  private val valuesValid: Map[String, Seq[String]] = Map(
    "value" -> Seq("1")
  )

  private val valuesInvalid: Map[String, Seq[String]] = Map(
    "value" -> Seq.empty
  )

  override def beforeEach(): Unit = {
    super.beforeEach()
  }

  "SelectAddress Controller" must {

    "Return OK and the correct view for a GET" in {
      val ua: UserAnswers = Data.ua
        .setOrException(EnterPostCodeId(index), seqAddresses)
      mutableFakeDataRetrievalAction.setDataToReturn(Some(ua))

      val view = app.injector.instanceOf[AddressListView]
      val expectedView = view(
        form, "entityType", "entityName",
        convertToRadioItems(seqAddresses),
        enterManuallyUrl = routes.ConfirmAddressController.onPageLoad(index,mode).url,
        schemeName = schemeName,
        submitUrl = controllers.establishers.partnership.address.routes.AddressYearsController.onSubmit(index, mode),
        h1MessageKey = "addressList.title"
      )(fakeRequest, messages)

      when(mockCommonAddressListService.get(any(), any(), any())(any()))
        .thenReturn(Future.successful(Ok(expectedView)))

      val result: Future[Result] = route(app, httpGETRequest(httpPathGET)).value

      status(result) mustEqual OK
      compareResultAndView(result, expectedView)
    }

    "redirect back to list of schemes for a GET when there is no data" in {

      mutableFakeDataRetrievalAction.setDataToReturn(None)

      val result: Future[Result] = route(app, httpGETRequest(httpPathGET)).value

      status(result) mustEqual SEE_OTHER

      redirectLocation(result).value mustBe controllers.preMigration.routes.ListOfSchemesController.onPageLoad(Scheme).url
    }

    "Save data to user answers and redirect to next page when valid data is submitted" in {
      val ua: UserAnswers = Data.ua
        .setOrException(EnterPostCodeId(index), seqAddresses)

      when(mockUserAnswersCacheConnector.save(any(), any())(any(), any()))
        .thenReturn(Future.successful(Json.obj()))
      when(mockCommonAddressListService.post(any(), any(), any(), any(), any(), any())(any(), any(), any()))
        .thenReturn(Future.successful(Results.SeeOther(onwardCall.url)))

      mutableFakeDataRetrievalAction.setDataToReturn(Some(ua))

      val result = route(app, httpPOSTRequest(httpPathPOST, valuesValid)).value

      status(result) mustEqual SEE_OTHER
      redirectLocation(result) mustBe Some(onwardCall.url)
    }

    "return a BAD REQUEST when invalid data is submitted" in {
      val ua: UserAnswers = Data.ua
        .setOrException(EnterPostCodeId(index), seqAddresses)
      mutableFakeDataRetrievalAction.setDataToReturn(Some(ua))
      when(mockCommonAddressListService.post(any(), any(), any(), any(), any(), any())(any(), any(), any()))
        .thenReturn(Future.successful(BadRequest))

      val result = route(app, httpPOSTRequest(httpPathPOST, valuesInvalid)).value

      status(result) mustEqual BAD_REQUEST
    }

    "redirect back to list of schemes for a POST when there is no data" in {
      mutableFakeDataRetrievalAction.setDataToReturn(None)

      val result = route(app, httpPOSTRequest(httpPathPOST, valuesValid)).value

      status(result) mustEqual SEE_OTHER

      redirectLocation(result).value mustBe controllers.preMigration.routes.ListOfSchemesController.onPageLoad(Scheme).url
    }
  }

}
