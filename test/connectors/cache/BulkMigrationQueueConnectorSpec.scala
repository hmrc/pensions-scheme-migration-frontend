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

package connectors.cache

import com.github.tomakehurst.wiremock.client.WireMock._
import org.scalatest.matchers.must.Matchers._
import org.scalatest.wordspec.AsyncWordSpec
import org.scalatest.{OptionValues, RecoverMethods}
import play.api.http.Status
import play.api.http.Status.ACCEPTED
import play.api.libs.json.Json
import uk.gov.hmrc.http.{HeaderCarrier, HttpException}
import utils.WireMockHelper

class BulkMigrationQueueConnectorSpec extends AsyncWordSpec with WireMockHelper with OptionValues with RecoverMethods {

  private implicit lazy val hc: HeaderCarrier = HeaderCarrier()

  override protected def portConfigKey: String = "microservice.services.pensions-scheme-migration.port"

  private lazy val connector: BulkMigrationQueueConnector = injector.instanceOf[BulkMigrationQueueConnector]
  private val bulkMigrationUrl = "/pensions-scheme-migration/bulk-migration"
  private val psaId = "test-psaId"

  ".pushAll" must {

    "save the lock in the collection" in {
      server.stubFor(
        post(urlEqualTo(bulkMigrationUrl))
          .withRequestBody(equalTo(Json.stringify(Json.obj())))
          .willReturn(
            aResponse.withStatus(ACCEPTED)
          )
      )

      connector.pushAll(psaId, Json.obj()) map { res =>
        res mustBe Json.obj()
      }
    }

    "return a failed future on upstream error" in {

      server.stubFor(
        post(urlEqualTo(bulkMigrationUrl))
          .withRequestBody(equalTo(Json.stringify(Json.obj())))
          .willReturn(
            serverError()
          )
      )
      recoverToExceptionIf[HttpException] {
        connector.pushAll(psaId, Json.obj())
      } map {
        _.responseCode mustEqual Status.INTERNAL_SERVER_ERROR
      }
    }
  }
}
