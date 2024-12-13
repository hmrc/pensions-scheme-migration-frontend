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

package controllers.establishers.company.director.address

import connectors.AddressLookupConnector
import controllers.ControllerSpecBase
import controllers.actions.MutableFakeDataRetrievalAction
import forms.address.AddressYearsFormProvider
import identifiers.beforeYouStart.SchemeNameId
import identifiers.establishers.company.director.DirectorNameId
import identifiers.establishers.company.director.address.AddressYearsId
import matchers.JsonMatchers
import models.{CheckMode, NormalMode, PersonName, Scheme}
import org.mockito.ArgumentMatchers.any
import play.api.Application
import play.api.data.Form
import play.api.inject.bind
import play.api.inject.guice.GuiceableModule
import play.api.libs.json.Json
import play.api.mvc.Results.{BadRequest, Ok}
import play.api.mvc.{Result, Results}
import play.api.test.Helpers._
import services.common.address.CommonAddressYearsService
import utils.Data.{schemeName, ua}
import utils.{Data, Enumerable, UserAnswers}
import views.html.address.AddressYearsView

import scala.concurrent.Future

class AddressYearsControllerSpec extends ControllerSpecBase with JsonMatchers with Enumerable.Implicits {

  private val mockAddressLookupConnector = mock[AddressLookupConnector]
  private val mockCommonAddressYearsService = mock[CommonAddressYearsService]

  val extraModules: Seq[GuiceableModule] = Seq(
    bind[AddressLookupConnector].toInstance(mockAddressLookupConnector),
    bind[CommonAddressYearsService].toInstance(mockCommonAddressYearsService)
  )
  private val mutableFakeDataRetrievalAction: MutableFakeDataRetrievalAction = new MutableFakeDataRetrievalAction()
  override def fakeApplication(): Application = applicationBuilderMutableRetrievalAction(mutableFakeDataRetrievalAction, extraModules).build()

  private val formProvider: AddressYearsFormProvider = new AddressYearsFormProvider()
  private val form: Form[Boolean]  = formProvider("")
  private val mode = NormalMode
  private val index = 0

  private val personName: PersonName = PersonName("Jane", "Doe")
  private val userAnswers: Option[UserAnswers] = Some(ua.setOrException(DirectorNameId(index,index), personName))
  private val httpPathGET: String = controllers.establishers.company.director.address.routes.AddressYearsController.onPageLoad(index, index, mode).url
  private val httpPathGETCheck: String = controllers.establishers.company.director.address.routes.AddressYearsController.onPageLoad(index, index, CheckMode).url
  private val httpPathPOST: String = controllers.establishers.company.director.address.routes.AddressYearsController.onSubmit(index, index, mode).url

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

  "AddressYears Controller" must {

    "Return OK and the correct view for a GET" in {
      val ua: UserAnswers = UserAnswers()
        .setOrException(SchemeNameId, Data.schemeName)
        .setOrException(DirectorNameId(index, index), personName)
      mutableFakeDataRetrievalAction.setDataToReturn(Some(ua))

      val view = app.injector.instanceOf[AddressYearsView]
      val expectedView = view(
        form, "eventType", "eventName",
        utils.Radios.yesNo(form("value")),
        Some(schemeName),
        controllers.establishers.partnership.address.routes.AddressYearsController.onSubmit(index, mode)
      )(fakeRequest, messages)

      when(mockCommonAddressYearsService.get(any(), any(), any(), any(), any(), any())(any()))
        .thenReturn(Future.successful(Ok(expectedView)))

      val result: Future[Result] = route(app, request).value

      status(result) mustEqual OK
      compareResultAndView(result, expectedView)
    }

    "return OK and the correct view for a GET when the question has previously been answered" in {
      val ua: UserAnswers = UserAnswers()
        .setOrException(SchemeNameId, Data.schemeName)
        .setOrException(DirectorNameId(index, index), personName)
        .setOrException(AddressYearsId(index, index), true)

      mutableFakeDataRetrievalAction.setDataToReturn(Some(ua))

      val result: Future[Result] = route(app, httpGETRequest(httpPathGETCheck)).value

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
      when(mockCommonAddressYearsService.post(any(), any(), any(), any(), any(), any(), any(), any())(any(), any()))
        .thenReturn(Future.successful(Results.SeeOther(onwardCall.url)))

      mutableFakeDataRetrievalAction.setDataToReturn(userAnswers)

      val result = route(app, httpPOSTRequest(httpPathPOST, valuesValid)).value

      status(result) mustEqual SEE_OTHER
      redirectLocation(result) mustBe Some(onwardCall.url)
    }

    "return a BAD REQUEST when invalid data is submitted" in {
      mutableFakeDataRetrievalAction.setDataToReturn(userAnswers)
      when(mockCommonAddressYearsService.post(any(), any(), any(), any(), any(), any(), any(), any())(any(), any()))
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
