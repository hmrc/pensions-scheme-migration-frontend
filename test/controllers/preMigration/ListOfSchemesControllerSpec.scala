/*
 * Copyright 2021 HM Revenue & Customs
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

package controllers.preMigration

import controllers.ControllerSpecBase
import controllers.actions._
import forms.ListSchemesFormProvider
import matchers.JsonMatchers
import models.Scheme
import org.mockito.ArgumentMatchers.any
import org.mockito.{ArgumentMatchers, MockitoSugar}
import org.scalatest.TryValues
import play.api.mvc.Results._
import play.api.test.Helpers.{status, _}
import services.{LockingService, SchemeSearchService}
import uk.gov.hmrc.nunjucks.NunjucksSupport

import scala.concurrent.Future
class ListOfSchemesControllerSpec extends ControllerSpecBase with NunjucksSupport with JsonMatchers with TryValues with MockitoSugar {

  private val mockSchemeSearchService: SchemeSearchService = mock[SchemeSearchService]
  private val mockLockingService: LockingService = mock[LockingService]

  private val formProvider: ListSchemesFormProvider = new ListSchemesFormProvider()

  private def controller: ListOfSchemesController =
    new ListOfSchemesController(mockAppConfig, messagesApi, new FakeAuthAction(),
      controllerComponents, formProvider, mockSchemeSearchService, mockLockingService)

  "onPageLoad" must {
    "return OK and the correct view returned by the service" in {

      when(mockSchemeSearchService.searchAndRenderView(any(), any(), any(), any())(any(), any(), any(), any()))
        .thenReturn(Future.successful(Ok("")))

      val result = controller.onPageLoad(Scheme)(fakeRequest)

      status(result) mustBe OK
      contentAsString(result) mustBe ""

    }
  }

  "onPageLoadWithPageNumber" must {
    "return OK and the correct view returned by the service" in {

      when(mockSchemeSearchService.searchAndRenderView(any(), any(), any(), any())(any(), any(), any(), any()))
        .thenReturn(Future.successful(Ok("")))

      val result = controller.onPageLoadWithPageNumber(1, Scheme)(fakeRequest)

      status(result) mustBe OK
      contentAsString(result) mustBe ""

    }
  }

  "onSearch" when {
    "return OK and the correct view when there are schemes without pagination and search on non empty string" in {
      when(mockSchemeSearchService.searchAndRenderView(any(), any(), any(), any())(any(), any(), any(), any()))
        .thenReturn(Future.successful(Ok("")))
      val searchText = "pstr1"


      val postRequest = fakeRequest.withFormUrlEncodedBody(("value", searchText))
      val result = controller.onSearch(Scheme)(postRequest)

      status(result) mustBe OK
    }

    "return BADREQUEST and error when no value is entered into search" in {
      when(mockSchemeSearchService.searchAndRenderView(any(), any(), ArgumentMatchers.eq(None), any())(any(), any(), any(), any()))
        .thenReturn(Future.successful(BadRequest("")))

      val postRequest = fakeRequest.withFormUrlEncodedBody(("value", ""))
      val result = controller.onSearch(Scheme)(postRequest)

      status(result) mustBe BAD_REQUEST

    }
  }


}
