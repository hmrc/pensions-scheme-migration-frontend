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

package controllers.establishers.partnership.address

import connectors.AddressLookupConnector
import controllers.ControllerSpecBase
import controllers.actions.AuthActionSpec.app.environment
import controllers.actions.MutableFakeDataRetrievalAction
import controllers.establishers.individual.address.routes
import forms.address.AddressFormProvider
import identifiers.beforeYouStart.SchemeNameId
import identifiers.establishers.partnership.PartnershipDetailsId
import matchers.JsonMatchers
import models.{NormalMode, Scheme}
import org.mockito.ArgumentMatchers.any
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceableModule
import play.api.libs.json.Json
import play.api.mvc.Results.{BadRequest, Ok}
import play.api.mvc.{Result, Results}
import play.api.test.Helpers._
import services.common.address.CommonManualAddressService
import uk.gov.hmrc.govukfrontend.views.viewmodels.select.SelectItem
import utils.{CountryOptions, Data, Enumerable, UserAnswers}
import views.html.address.ManualAddressView

import scala.concurrent.Future

class ConfirmAddressControllerSpec extends ControllerSpecBase with JsonMatchers with Enumerable.Implicits {

  private val mockAddressLookupConnector = mock[AddressLookupConnector]
  private val mockCommonManualAddressService = mock[CommonManualAddressService]

  val extraModules: Seq[GuiceableModule] = Seq(
    bind[AddressLookupConnector].toInstance(mockAddressLookupConnector),
    bind[CommonManualAddressService].toInstance(mockCommonManualAddressService)
  )
  private val mutableFakeDataRetrievalAction: MutableFakeDataRetrievalAction = new MutableFakeDataRetrievalAction()
  override def fakeApplication(): Application = applicationBuilderMutableRetrievalAction(mutableFakeDataRetrievalAction, extraModules).build()

  private val countryOptions: CountryOptions = new CountryOptions(environment, appConfig)
  private val formProvider: AddressFormProvider = new AddressFormProvider(countryOptions)
  private val form = formProvider()
  private val mode = NormalMode
  private val index = 0

  private val ua: UserAnswers =
    Data.ua.setOrException(PartnershipDetailsId(index), Data.partnershipDetails)

  private val userAnswers: Option[UserAnswers] = Some(ua)
  private val httpPathGET: String = controllers.establishers.partnership.address.routes.ConfirmAddressController.onPageLoad(index,mode).url
  private val httpPathPOST: String = controllers.establishers.partnership.address.routes.ConfirmAddressController.onSubmit(index,mode).url

  private val valuesValid: Map[String, Seq[String]] = Map(
    "line1" -> Seq("1"),
    "line2" -> Seq("2"),
    "line3" -> Seq("3"),
    "line4" -> Seq("4"),
    "postcode" -> Seq("ZZ1 1ZZ"),
    "country" -> Seq("GB")
  )

  private val valuesInvalid: Map[String, Seq[String]] = Map(
    "value" -> Seq.empty
  )

  override def beforeEach(): Unit = {
    super.beforeEach()
    when(mockAppConfig.validCountryCodes).thenReturn(Seq("GB"))
  }

  "ConfirmAddress Controller" must {

    "Return OK and the correct view for a GET" in {
      val ua: UserAnswers = UserAnswers()
        .setOrException(SchemeNameId, Data.schemeName)
        .setOrException(PartnershipDetailsId(index), Data.partnershipDetails)
      mutableFakeDataRetrievalAction.setDataToReturn(Some(ua))

      val view = app.injector.instanceOf[ManualAddressView]

      val expectedView = view(
        form = form, pageTitle = "address.title", h1 = "address.title",
        submitUrl = routes.ConfirmAddressController.onPageLoad(index, mode),
        schemeName = Some(Data.schemeName),
        countries =  mock[Seq[SelectItem]],
        postcodeEntry = false, postcodeFirst = false
      )(fakeRequest, messages)

      when(mockCommonManualAddressService.get(any(), any(), any(), any(), any(), any(), any(), any(), any())(any(), any()))
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

      when(mockUserAnswersCacheConnector.save(any(), any())(any(), any()))
        .thenReturn(Future.successful(Json.obj()))
      when(mockCommonManualAddressService.post(any(), any(), any(), any(), any(), any(), any(), any(), any(), any())(any(), any()))
        .thenReturn(Future.successful(Results.SeeOther(onwardCall.url)))

      mutableFakeDataRetrievalAction.setDataToReturn(userAnswers)
      val result = route(app, httpPOSTRequest(httpPathPOST, valuesValid)).value

      status(result) mustEqual SEE_OTHER
      redirectLocation(result) mustBe Some(onwardCall.url)
    }

    "return a BAD REQUEST when invalid data is submitted" in {
      mutableFakeDataRetrievalAction.setDataToReturn(userAnswers)

      when(mockCommonManualAddressService.post(any(), any(), any(), any(), any(), any(), any(), any(), any(), any())(any(), any()))
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
