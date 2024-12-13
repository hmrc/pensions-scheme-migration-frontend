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

package handlers

import base.SpecBase
import controllers.routes
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import play.api.http.Status.{BAD_REQUEST, INTERNAL_SERVER_ERROR}
import play.api.test.FakeRequest
import play.api.test.Helpers.{GET, _}
import views.html.{BadRequestView, InternalServerErrorView}

class ErrorHandlerSpec extends SpecBase with BeforeAndAfterEach with MockitoSugar {

  "ErrorHandlerSpec" must {

    "catch BAD_REQUEST and return the correct view" in {
      val errorHandler = app.injector.instanceOf[ErrorHandler]
      val request = FakeRequest(GET, routes.DeclarationController.onPageLoad.url)


      val result = errorHandler.onClientError(request, BAD_REQUEST, "BAD_REQUEST")

      val view = app.injector.instanceOf[BadRequestView]

      status(result) mustEqual BAD_REQUEST
      contentAsString(result) mustEqual view()(request, messages).toString
    }

    "catch INTERNAL_SERVER_ERROR and return the correct view" in {
      val errorHandler = app.injector.instanceOf[ErrorHandler]
      val request = FakeRequest(GET, routes.DeclarationController.onPageLoad.url)

      val result = errorHandler.onServerError(request, new Exception("Internal server error"))

      val view = app.injector.instanceOf[InternalServerErrorView]

      status(result) mustEqual INTERNAL_SERVER_ERROR
      contentAsString(result) mustEqual view()(request, messages).toString
    }
  }
}
