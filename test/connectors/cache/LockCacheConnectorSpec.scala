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

package connectors.cache

import com.github.tomakehurst.wiremock.client.WireMock._
import models.MigrationLock
import org.scalatest.matchers.must.Matchers._
import org.scalatest.wordspec.AsyncWordSpec
import org.scalatest.{OptionValues, RecoverMethods}
import play.api.http.Status
import play.api.libs.json.Json
import play.api.mvc.Results._
import uk.gov.hmrc.http.{HeaderCarrier, HttpException}
import utils.WireMockHelper

class LockCacheConnectorSpec extends AsyncWordSpec with WireMockHelper with OptionValues with RecoverMethods {

  private implicit lazy val hc: HeaderCarrier = HeaderCarrier()

  override protected def portConfigKey: String = "microservice.services.pensions-scheme-migration.port"

  private lazy val connector: LockCacheConnector = injector.instanceOf[LockCacheConnector]
  private val lockUrl = "/pensions-scheme-migration/lock"
  private val lockByUserUrl = "/pensions-scheme-migration/lock-by-user"
  private val lockOnSchemeUrl = "/pensions-scheme-migration/lock-on-scheme"
  private val pstr = "pstr"
  private val lock: MigrationLock = MigrationLock(pstr, "credId", "psaId")

  ".setLock" must {

    "save the lock in the collection" in {
      server.stubFor(
        post(urlEqualTo(lockUrl))
          .withRequestBody(equalTo(Json.stringify(Json.obj())))
          .willReturn(
            aResponse.withStatus(200)
          )
      )

      connector.setLock(lock) map {
        _ mustEqual Json.toJson(lock)
      }
    }

    "return a duplicate " in {
      server.stubFor(
        post(urlEqualTo(lockUrl))
          .withRequestBody(equalTo(Json.stringify(Json.obj())))
          .willReturn(
            aResponse.withStatus(409)
          )
      )

      recoverToExceptionIf[HttpException] {
        connector.setLock(lock)
      } map {
        _.responseCode mustEqual Status.CONFLICT
      }
    }

    "return a failed future on upstream error" in {

      server.stubFor(
        post(urlEqualTo(lockUrl))
          .withRequestBody(equalTo(Json.stringify(Json.obj())))
          .willReturn(
            serverError()
          )
      )
      recoverToExceptionIf[HttpException] {
        connector.setLock(lock)
      } map {
        _.responseCode mustEqual Status.INTERNAL_SERVER_ERROR
      }
    }
  }

  ".getLock" must {

    "return `None` when there is no data in the collection" in {
      server.stubFor(
        get(urlEqualTo(lockUrl))
          .willReturn(
            notFound
          )
      )

      connector.getLock(lock) map {
        result =>
          result mustNot be(defined)
      }
    }

    "return data if data is present in the collection" in {
      server.stubFor(
        get(urlEqualTo(lockUrl))
          .willReturn(
            ok(Json.toJson(lock).toString())
          )
      )

      connector.getLock(lock) map {
        result =>
          result.value mustEqual lock
      }
    }

    "return a failed future on upstream error" in {
      server.stubFor(
        get(urlEqualTo(lockUrl))
          .willReturn(
            serverError
          )
      )

      recoverToExceptionIf[HttpException] {
        connector.getLock(lock)
      } map {
        _.responseCode mustEqual Status.INTERNAL_SERVER_ERROR
      }
    }
  }

  ".getLockOnScheme" must {

    "return `None` when there is no data in the collection" in {
      server.stubFor(
        get(urlEqualTo(lockOnSchemeUrl))
          .willReturn(
            notFound
          )
      )

      connector.getLockOnScheme(pstr) map {
        result =>
          result mustNot be(defined)
      }
    }

    "return data if data is present in the collection" in {
      server.stubFor(
        get(urlEqualTo(lockOnSchemeUrl))
          .willReturn(
            ok(Json.toJson(lock).toString())
          )
      )

      connector.getLockOnScheme(pstr) map {
        result =>
          result.value mustEqual lock
      }
    }

    "return a failed future on upstream error" in {
      server.stubFor(
        get(urlEqualTo(lockOnSchemeUrl))
          .willReturn(
            serverError
          )
      )

      recoverToExceptionIf[HttpException] {
        connector.getLockOnScheme(pstr)
      } map {
        _.responseCode mustEqual Status.INTERNAL_SERVER_ERROR
      }
    }
  }

  ".getLockByUser" must {

    "return `None` when there is no data in the collection" in {
      server.stubFor(
        get(urlEqualTo(lockByUserUrl))
          .willReturn(
            notFound
          )
      )

      connector.getLockByUser map {
        result =>
          result mustNot be(defined)
      }
    }

    "return data if data is present in the collection" in {
      server.stubFor(
        get(urlEqualTo(lockByUserUrl))
          .willReturn(
            ok(Json.toJson(lock).toString())
          )
      )

      connector.getLockByUser map {
        result =>
          result.value mustEqual lock
      }
    }

    "return a failed future on upstream error" in {
      server.stubFor(
        get(urlEqualTo(lockByUserUrl))
          .willReturn(
            serverError
          )
      )

      recoverToExceptionIf[HttpException] {
        connector.getLockByUser
      } map {
        _.responseCode mustEqual Status.INTERNAL_SERVER_ERROR
      }
    }
  }

  ".removeLock" must {

    "return OK after removing all the data from the collection" in {
      server.stubFor(delete(urlEqualTo(lockUrl)).
        willReturn(ok)
      )
      connector.removeLock(lock).map {
        _ mustEqual Ok
      }
    }
  }

  ".removeLockOnScheme" must {

    "return OK after removing all the data from the collection" in {
      server.stubFor(delete(urlEqualTo(lockOnSchemeUrl)).
        willReturn(ok)
      )
      connector.removeLockOnScheme(pstr).map {
        _ mustEqual Ok
      }
    }
  }

  ".removeLockByUser" must {

    "return OK after removing all the data from the collection" in {
      server.stubFor(delete(urlEqualTo(lockByUserUrl)).
        willReturn(ok)
      )
      connector.removeLockByUser.map {
        _ mustEqual Ok
      }
    }
  }
}
