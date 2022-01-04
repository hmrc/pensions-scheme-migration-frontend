/*
 * Copyright 2022 HM Revenue & Customs
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

package connectors

import com.fasterxml.jackson.core.JsonParseException
import com.github.tomakehurst.wiremock.client.WireMock._
import models.RacDac
import org.scalatest.matchers.must.Matchers._
import org.scalatest.wordspec.AsyncWordSpec
import org.scalatest.{OptionValues, RecoverMethods}
import play.api.http.Status
import play.api.http.Status.OK
import play.api.libs.json.{JsResultException, Json}
import uk.gov.hmrc.http.{BadRequestException, HeaderCarrier, UpstreamErrorResponse}
import utils.{Data, WireMockHelper}

class PensionsSchemeConnectorSpec extends AsyncWordSpec with WireMockHelper with OptionValues with RecoverMethods {

  private implicit lazy val hc: HeaderCarrier = HeaderCarrier()

  override protected def portConfigKey: String = "microservice.services.pensions-scheme-migration.port"

  private lazy val connector: PensionsSchemeConnector = injector.instanceOf[PensionsSchemeConnector]
  private val registerSchemeUrl = "/pensions-scheme-migration/register-scheme/racdac"
  private val psaId = "test-psaId"
  private val schemeId = "test-scheme-id"
  val json = Json.obj(
    fields = "schemeName" -> "Test scheme name"
  )

  private val invalidPayloadResponse =
    Json.stringify(
      Json.obj(
        "code" -> "INVALID_PAYLOAD",
        "reason" -> "test-reason"
      )
    )
  "registerScheme" must {

    "return right schemeReferenceNumber for a valid request/response" in {
      server.stubFor(
        post(urlEqualTo(registerSchemeUrl))
          .withHeader("Content-Type", equalTo("application/json"))
          .withHeader("psaId", equalTo(psaId))
          .withRequestBody(equalToJson(Json.stringify(Data.ua.data)))
          .willReturn(
            aResponse.withStatus(OK)
              .withHeader("Content-Type", "application/json")
              .withBody(Json.obj("schemeReferenceNumber" -> schemeId).toString())
          )
      )

      connector.registerScheme(Data.ua,psaId,RacDac) map { res =>
        res mustBe  schemeId
      }
    }

    "return exception for a 500 response" in {

      server.stubFor(
        post(urlEqualTo(registerSchemeUrl))
          .withRequestBody(equalTo(Json.stringify(Json.toJson(Data.ua.data))))
          .willReturn(
            aResponse()
          .withStatus(Status.INTERNAL_SERVER_ERROR)
          .withHeader("Content-Type", "application/json")
          .withBody(invalidPayloadResponse)
          )
      )
      recoverToSucceededIf[UpstreamErrorResponse] {
        connector.registerScheme(Data.ua,psaId,RacDac)
      }
    }

    "return exception for a 400 response" in {

      server.stubFor(
        post(urlEqualTo(registerSchemeUrl))
          .withRequestBody(equalTo(Json.stringify(Json.toJson(Data.ua.data))))
          .willReturn(
            aResponse()
              .withStatus(Status.BAD_REQUEST)
              .withHeader("Content-Type", "application/json")
              .withBody(invalidPayloadResponse)
          )
      )
      recoverToSucceededIf[BadRequestException] {
        connector.registerScheme(Data.ua,psaId,RacDac)
      }
    }

    "throw JsonParseException if there are JSON parse errors" in {

      server.stubFor(
        post(urlEqualTo(registerSchemeUrl))
          .withRequestBody(equalTo(Json.stringify(Json.toJson(Data.ua.data))))
          .willReturn(
            aResponse()
              .withStatus(Status.OK)
              .withHeader("Content-Type", "application/json")
              .withBody("this-is-not-valid-json")
          )
      )
      recoverToSucceededIf[JsonParseException] {
        connector.registerScheme(Data.ua,psaId,RacDac)
      }
    }

    "throw JsResultException if the JSON is not valid" in {

      server.stubFor(
        post(urlEqualTo(registerSchemeUrl))
          .withRequestBody(equalTo(Json.stringify(Json.toJson(Data.ua.data))))
          .willReturn(
            aResponse()
              .withStatus(Status.OK)
              .withHeader("Content-Type", "application/json")
              .withBody("{}")
          )
      )
      recoverToSucceededIf[JsResultException] {
        connector.registerScheme(Data.ua,psaId,RacDac)
      }
    }

  }
}
