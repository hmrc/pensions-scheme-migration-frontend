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
import play.api.libs.json.{JsBoolean, Json}
import uk.gov.hmrc.http.{HeaderCarrier, HttpException}
import utils.WireMockHelper

class BulkMigrationQueueConnectorSpec extends AsyncWordSpec with WireMockHelper with OptionValues with RecoverMethods {

  private implicit lazy val hc: HeaderCarrier = HeaderCarrier()

  override protected def portConfigKey: String = "microservice.services.pensions-scheme-migration.port"

  private lazy val connector: BulkMigrationQueueConnector = injector.instanceOf[BulkMigrationQueueConnector]
  private val bulkMigrationUrl = "/pensions-scheme-migration/bulk-migration"
  private val bulkMigrationIsInProgressUrl = "/pensions-scheme-migration/bulk-migration/isRequestInProgress"
  private val bulkMigrationAllFailedUrl = "/pensions-scheme-migration/bulk-migration/isAllFailed"
  private val bulkMigrationDeleteAllUrl = "/pensions-scheme-migration/bulk-migration/deleteAll"
  private val psaId = "test-psaId"

  ".pushAll" must {

    "post the requests to the queue" in {
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

  ".isRequestInProgress" must {

    "return true if requests are in progress/todo" in {
      server.stubFor(
        get(urlEqualTo(bulkMigrationIsInProgressUrl))
          .willReturn(ok(JsBoolean(true).toString()))
      )

      connector.isRequestInProgress(psaId) map { res =>
        res mustBe true
      }
    }

    "return false for upstream error" in {
      server.stubFor(
        get(urlEqualTo(bulkMigrationIsInProgressUrl))
          .willReturn(
            serverError
          )
      )
      connector.isRequestInProgress(psaId) map { res =>
        res mustBe false
      }
    }
  }

  ".isAllFailed" must {

    "return true if all requests are failed" in {
      server.stubFor(
        get(urlEqualTo(bulkMigrationAllFailedUrl))
          .willReturn(ok(JsBoolean(true).toString()))
      )

      connector.isAllFailed(psaId) map { res =>
        res mustBe Some(true)
      }
    }

    "return None if nothing in the queue" in {
      server.stubFor(
        get(urlEqualTo(bulkMigrationAllFailedUrl))
          .willReturn(aResponse.withStatus(204))
      )

      connector.isAllFailed(psaId) map { res =>
        res mustBe None
      }
    }

    "throw http exception for upstream error" in {
      server.stubFor(
        get(urlEqualTo(bulkMigrationAllFailedUrl))
          .willReturn(
            serverError
          )
      )
      recoverToExceptionIf[HttpException] {
        connector.isAllFailed(psaId)
      } map {
        _.responseCode mustEqual Status.INTERNAL_SERVER_ERROR
      }
    }
  }

  ".deleteAll" must {

    "return true if all requests are deleted" in {
      server.stubFor(
        delete(urlEqualTo(bulkMigrationDeleteAllUrl))
          .willReturn(ok(JsBoolean(true).toString()))
      )

      connector.deleteAll(psaId) map { res =>
        res mustBe true
      }
    }

    "throw http exception for upstream error" in {
      server.stubFor(
        delete(urlEqualTo(bulkMigrationDeleteAllUrl))
          .willReturn(
            serverError
          )
      )
      recoverToExceptionIf[HttpException] {
        connector.deleteAll(psaId)
      } map {
        _.responseCode mustEqual Status.INTERNAL_SERVER_ERROR
      }
    }
  }
}
