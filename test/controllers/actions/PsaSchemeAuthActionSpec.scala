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

package controllers.actions

import base.SpecBase
import config.AppConfig
import connectors.LegacySchemeDetailsConnector
import models.requests.AuthenticatedRequest
import org.mockito.ArgumentMatchers.any
import org.mockito.MockitoSugar
import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.ScalaFutures
import play.api.http.Status.NOT_FOUND
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.Results.{NotFound, Ok}
import play.api.test.Helpers._
import renderer.Renderer
import uk.gov.hmrc.domain.PsaId
import uk.gov.hmrc.http.HttpResponse

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class PsaSchemeAuthActionSpec
  extends SpecBase with BeforeAndAfterAll with ScalaFutures with MockitoSugar {

  import PsaSchemeAuthActionSpec._

  private val legacySchemeDetailsConnector = mock[LegacySchemeDetailsConnector]
  private val action = new PsaSchemeAuthAction(legacySchemeDetailsConnector, app.injector.instanceOf[Renderer], app.injector.instanceOf[AppConfig])

  override def afterAll(): Unit = reset(legacySchemeDetailsConnector)

  "PsaSchemeAuthActionSpec" must {

    "return 200 OK if getLegacySchemeDetails succeeds for given PsaId and PSTR" in {

      when(legacySchemeDetailsConnector.getLegacySchemeDetails(any(), any())(any(), any()))
        .thenReturn(Future.successful(Right(legacySchemeDetailsResponse)))

      val request = AuthenticatedRequest(fakeRequest, externalId, psaId)
      val result = action(pstr).invokeBlock(request, { _: AuthenticatedRequest[_] => Future.successful(Ok("")) })
      status(result) mustBe OK
    }

    "return 404 Not Found if getLegacySchemeDetails fails to retrieve data for a given PsaId and PSTR" in {
      when(legacySchemeDetailsConnector.getLegacySchemeDetails(any(), any())(any(), any()))
        .thenReturn(Future.successful(Left(notFoundResponse)))

      val request = AuthenticatedRequest(fakeRequest, externalId, psaId)
      val result = action(pstr).invokeBlock(request, { _: AuthenticatedRequest[_] => Future.successful(NotFound("")) })
      status(result) mustBe NOT_FOUND
      }
    }
}

object PsaSchemeAuthActionSpec {

  private val legacySchemeDetailsResponse: JsValue = Json.obj("schemeDetails" -> Json.obj("abc" -> "123"))
  private val (externalId, psaId, pstr) = ("externalId", PsaId("A0000000"), "pstr")
  private val notFoundResponse: HttpResponse = HttpResponse(NOT_FOUND, "Not found")
}
