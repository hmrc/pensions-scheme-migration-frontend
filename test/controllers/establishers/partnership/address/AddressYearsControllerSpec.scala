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

import controllers.ControllerSpecBase
import controllers.actions._
import forms.address.AddressYearsFormProvider
import identifiers.establishers.partnership.address.AddressYearsId
import models.{Index, NormalMode}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{times, verify, when}
import org.scalatest.{BeforeAndAfterEach, TryValues}
import play.api.libs.json.Json
import play.api.mvc.Results.{BadRequest, Ok, Redirect}
import play.api.mvc.{AnyContentAsFormUrlEncoded, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.common.address.CommonAddressYearsService
import utils.{FakeNavigator, UserAnswers}
import views.html.address.AddressYearsView

import scala.concurrent.Future

class AddressYearsControllerSpec extends ControllerSpecBase with TryValues with BeforeAndAfterEach {

  private val formProvider = new AddressYearsFormProvider()
  private val form = formProvider("partnershipAddressYears.error.required")
  private val index = Index(0)
  private val mode = NormalMode
  private val schemeName = Some("Test Scheme")
  private val partnershipName = "Test Partnership"
  private val entityType = "establisherEntityTypePartnership"

  private val mockCommonAddressYearsService = mock[CommonAddressYearsService]
  private val addressYearsView = app.injector.instanceOf[AddressYearsView]

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockCommonAddressYearsService)
  }

  private def controller(dataRetrievalAction: DataRetrievalAction) =
    new AddressYearsController(
      messagesApi = messagesApi,
      authenticate = new FakeAuthAction(),
      getData = dataRetrievalAction,
      requireData = new DataRequiredActionImpl,
      formProvider = formProvider,
      common = mockCommonAddressYearsService
    )(ec = scala.concurrent.ExecutionContext.global)

  "AddressYearsController" must {

    "return OK and the correct view for a GET" in {
      val getData = new FakeDataRetrievalAction(Some(UserAnswers().setOrException(AddressYearsId(index), true)))

      when(mockCommonAddressYearsService.get(any(), any(), any(), any(), any(), any())(any(), any()))
        .thenReturn(Future.successful(Ok(addressYearsView(form, entityType, partnershipName, Seq.empty, schemeName,
          routes.AddressYearsController.onSubmit(index, mode))(fakeRequest, messages))))

      val result: Future[Result] = controller(getData).onPageLoad(index, mode)(fakeRequest)

      status(result) mustBe OK
      verify(mockCommonAddressYearsService, times(1)).get(any(), any(), any(), any(), any(), any())(any(), any())
    }

//    "redirect to the next page when valid data is submitted" in {
//      val request: FakeRequest[AnyContentAsFormUrlEncoded] = fakeRequest.withFormUrlEncodedBody("value" -> "true")
//
//      when(mockCommonAddressYearsService.post(any(), any(), any(), any(), any(), any(), any(), any())(any(), any()))
//        .thenReturn(Future.successful(Redirect(routes.AddressYearsController.onPageLoad(index, mode))))
//
//      val result: Future[Result] = controller(new FakeDataRetrievalAction(Some(UserAnswers()))).onSubmit(index, mode)(request)
//
//      status(result) mustBe SEE_OTHER
//      redirectLocation(result) mustBe Some(routes.AddressYearsController.onPageLoad(index, mode).url)
//      verify(mockCommonAddressYearsService, times(1)).post(any(), any(), any(), any(), any(), any(), any(), any())(any(), any())
//    }

//    "return a BAD REQUEST when invalid data is submitted" in {
//      val request: FakeRequest[AnyContentAsFormUrlEncoded] = fakeRequest.withFormUrlEncodedBody("value" -> "")
//
//      when(mockCommonAddressYearsService.post(any(), any(), any(), any(), any(), any(), any(), any())(any(), any()))
//        .thenReturn(Future.successful(BadRequest(addressYearsView(form.withError("value", "error.required"), entityType, partnershipName, Seq.empty, schemeName, routes.AddressYearsController.onSubmit(index, mode)))))
//
//      val result: Future[Result] = controller(new FakeDataRetrievalAction(Some(UserAnswers()))).onSubmit(index, mode)(request)
//
//      status(result) mustBe BAD_REQUEST
//      verify(mockCommonAddressYearsService, times(1)).post(any(), any(), any(), any(), any(), any(), any(), any())(any(), any())
//    }
//
//    "redirect to Session Expired page for a GET when there is no data" in {
//      val result: Future[Result] = controller(dontGetAnyData).onPageLoad(index, mode)(fakeRequest)
//
//      status(result) mustBe SEE_OTHER
//      redirectLocation(result).value mustBe controllers.routes.SessionExpiredController.onPageLoad().url
//    }
//
//    "redirect to Session Expired page for a POST when there is no data" in {
//      val request: FakeRequest[AnyContentAsFormUrlEncoded] = fakeRequest.withFormUrlEncodedBody("value" -> "true")
//
//      val result: Future[Result] = controller(dontGetAnyData).onSubmit(index, mode)(request)
//
//      status(result) mustBe SEE_OTHER
//      redirectLocation(result).value mustBe controllers.routes.SessionExpiredController.onPageLoad().url
//    }
  }
}
