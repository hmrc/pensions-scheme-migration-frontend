/*
 * Copyright 2023 HM Revenue & Customs
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

import com.github.tomakehurst.wiremock.client.WireMock._
import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.{BeforeAndAfterEach, OptionValues}
import play.api.http.Status._
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.http.HeaderCarrier
import utils.WireMockHelper

class LegacySchemeDetailsConnectorSpec extends AsyncFlatSpec with Matchers with WireMockHelper with BeforeAndAfterEach {

  import LegacySchemeDetailsConnectorSpec._

  override protected def portConfigKey: String = "microservice.services.pensions-scheme-migration.port"

  "legacy scheme details connector" should "return the List of Schemes for a valid request/response" in {

    server.stubFor(
      get(urlEqualTo(legacySchemeDetailsUrl))
        .withHeader("psaId", equalTo(psaId))
        .withHeader("pstr", equalTo(pstr))
        .willReturn(
          aResponse()
            .withStatus(OK)
            .withHeader("Content-Type", "application/json")
            .withBody(Json.stringify(Json.toJson(expectedResponse)))
        )
    )

    val connector = injector.instanceOf[LegacySchemeDetailsConnector]

    connector.getLegacySchemeDetails(psaId, pstr).map(listOfSchemes =>
      listOfSchemes.toOption.get shouldBe expectedResponse
    )

  }

  it should "throw InvalidPayloadException for a 400 INVALID_PAYLOAD response" in {

    server.stubFor(
      get(urlEqualTo(legacySchemeDetailsUrl))
        .willReturn(
          badRequest
            .withHeader("Content-Type", "application/json")
            .withBody(invalidPayloadResponse)
        )
    )

    val connector = injector.instanceOf[LegacySchemeDetailsConnector]

    connector.getLegacySchemeDetails(psaId, pstr).map(listOfSchemes =>
      listOfSchemes.swap.toOption.get.status shouldBe BAD_REQUEST
    )
  }

}

  object LegacySchemeDetailsConnectorSpec extends OptionValues {

    private val legacySchemeDetailsUrl = "/pensions-scheme-migration/getLegacySchemeDetails"

    private implicit val headerCarrier: HeaderCarrier = HeaderCarrier()

    private val psaId = "A2110001"

    private val pstr = "A0000030"

    private val expectedResponse : JsValue = Json.obj(
    "schemeName" -> "QROPS Rejected",
    "schemeType" -> "body-corporate",
    "currentMembers" -> "1",
    "investmentRegulated" -> true,
    "occupationalPensionScheme" -> true,
    "schemeEstablishedCountry" -> "GB",
    "securedBenefits"-> false,
    "racDac"-> false,
    "schemeOpenDate" -> "2017-04-06",
    "relationshipStartDate"-> "2017-04-06")


    private val invalidPayloadResponse =
      Json.stringify(
        Json.obj(
          "code" -> "INVALID_PAYLOAD",
          "reason" -> "test-reason"
        )
      )
  }



