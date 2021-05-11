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
import models.MigrationLock
import org.scalatest.MustMatchers._
import org.scalatest.{AsyncWordSpec, OptionValues, RecoverMethods}
import play.api.http.Status
import play.api.libs.json.Json
import play.api.mvc.Results._
import uk.gov.hmrc.http.{HeaderCarrier, HttpException}
import utils.WireMockHelper

class UserAnswersCacheConnectorSpec extends AsyncWordSpec with WireMockHelper with OptionValues with RecoverMethods {

  private implicit lazy val hc: HeaderCarrier = HeaderCarrier()

  override protected def portConfigKey: String = "microservice.services.pensions-scheme-migration.port"

  private lazy val connector: UserAnswersCacheConnector = injector.instanceOf[UserAnswersCacheConnector]
  private val dataCacheUrl = "/pensions-scheme-migration/migration-data"
  private val pstr = "pstr"
  private val lock: MigrationLock = MigrationLock(pstr, "credId", "psaId")

  ".fetch" must {

    "return `None` when there is no data in the collection" in {
      server.stubFor(
        get(urlEqualTo(dataCacheUrl))
          .willReturn(
            notFound
          )
      )

      connector.fetch(pstr) map {
        result =>
          result mustNot be(defined)
      }
    }

    "return data if data is present in the collection" in {
      server.stubFor(
        get(urlEqualTo(dataCacheUrl))
          .willReturn(
            ok(Json.obj(fields = "testId" -> "data").toString())
          )
      )

      connector.fetch(pstr) map {
        result =>
          result.value mustEqual Json.obj(fields = "testId" -> "data")
      }
    }

    "return a failed future on upstream error" in {
      server.stubFor(
        get(urlEqualTo(dataCacheUrl))
          .willReturn(
            serverError
          )
      )

      recoverToExceptionIf[HttpException] {
        connector.fetch(pstr)
      } map {
        _.responseCode mustEqual Status.INTERNAL_SERVER_ERROR
      }
    }
  }

  ".save" must {
    val json = Json.obj(
      fields = "testId" -> "foobar"
    )
    "save the data in the collection" in {
      server.stubFor(
        post(urlEqualTo(dataCacheUrl))
          .withRequestBody(equalTo(Json.stringify(json)))
          .willReturn(
            aResponse.withStatus(200)
          )
      )

      connector.save(lock, json) map {
        _ mustEqual json
      }
    }

    "return a failed future on upstream error" in {

      server.stubFor(
        post(urlEqualTo(dataCacheUrl))
          .withRequestBody(equalTo(Json.stringify(json)))
          .willReturn(
            serverError()
          )
      )
      recoverToExceptionIf[HttpException] {
        connector.save(lock, json)
      } map {
        _.responseCode mustEqual Status.INTERNAL_SERVER_ERROR
      }
    }
  }

  ".remove" must {

    "return OK after removing all the data from the collection" in {
      server.stubFor(delete(urlEqualTo(dataCacheUrl)).
        willReturn(ok)
      )
      connector.remove(pstr).map {
        _ mustEqual Ok
      }
    }
  }
}
