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

import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, _}
import models.{Items, ListOfLegacySchemes}
import org.scalatest.{BeforeAndAfterEach, OptionValues}
import org.scalatest.matchers.should.Matchers
import org.scalatest.flatspec.AsyncFlatSpec
import play.api.http.Status
import play.api.http.Status._
import play.api.libs.json.Json
import uk.gov.hmrc.http.HeaderCarrier
import utils.WireMockHelper

class ListOfSchemesConnectorSpec extends AsyncFlatSpec with Matchers with WireMockHelper with BeforeAndAfterEach {

  import ListOfSchemesConnectorSpec._

  override protected def portConfigKey: String = "microservice.services.pensions-scheme-migration.port"

  "list of schemes connector" should "return the List of Schemes for a valid request/response" in {

    server.stubFor(
      get(urlEqualTo(listOfSchemesUrl))
        .withHeader("psaId", equalTo(psaId))
        .willReturn(
          aResponse()
            .withStatus(OK)
            .withHeader("Content-Type", "application/json")
            .withBody(Json.stringify(Json.toJson(expectedResponse)))
        )
    )

    val connector = injector.instanceOf[ListOfSchemesConnector]

    connector.getListOfSchemes(psaId).map(listOfSchemes =>
      listOfSchemes.toOption.get shouldBe expectedResponse
    )

  }

  it should "throw InvalidPayloadException for a 400 INVALID_PAYLOAD response" in {

    server.stubFor(
      get(urlEqualTo(listOfSchemesUrl))
        .willReturn(
            aResponse()
            .withStatus(Status.BAD_REQUEST)
            .withHeader("Content-Type", "application/json")
            .withBody(invalidPayloadResponse)
        )
    )

    val connector = injector.instanceOf[ListOfSchemesConnector]

    connector.getListOfSchemes(psaId).map(listOfSchemes =>
      listOfSchemes.swap.toOption.get.status shouldBe BAD_REQUEST
    )
  }

  it should "throw ListOfSchemes5xxException for a 5xx response" in {

    server.stubFor(
      get(urlEqualTo(listOfSchemesUrl))
        .willReturn(
          aResponse()
            .withStatus(INTERNAL_SERVER_ERROR)
            .withHeader("Content-Type", "application/json")
            .withBody("{}")
        )
    )

    val connector = injector.instanceOf[ListOfSchemesConnector]

    recoverToSucceededIf[ListOfSchemes5xxException] {
      connector.getListOfSchemes(psaId)
    }
  }

}

object ListOfSchemesConnectorSpec extends OptionValues {

  private val listOfSchemesUrl = "/pensions-scheme-migration/list-of-schemes"

  private implicit val headerCarrier: HeaderCarrier = HeaderCarrier()

  private val psaId = "A2110001"

  private val schemeDetail = Items("10000678RE", "2020-10-10", racDac = false, "abcdefghi", "2020-12-12", None)
  private val racDacDetail = Items("10000678RF", "2020-10-10", racDac = true, "abcdefghi", "2020-12-12", Some("12345678"))

  private val expectedResponse = ListOfLegacySchemes(1, Some(List(schemeDetail, racDacDetail)))

  private val invalidPayloadResponse =
    Json.stringify(
      Json.obj(
        "code" -> "INVALID_PAYLOAD",
        "reason" -> "test-reason"
      )
    )
}
