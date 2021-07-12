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

package controllers.actions

import base.SpecBase
import connectors.cache.{LockCacheConnector, SchemeCacheConnector, UserAnswersCacheConnector}
import models.MigrationLock
import models.requests.{AuthenticatedRequest, OptionalDataRequest}
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.json.Json
import uk.gov.hmrc.domain.PsaId
import utils.Data.psaId

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class DataRetrievalActionSpec
  extends SpecBase
    with MockitoSugar
    with ScalaFutures {

  class Harness(dataCacheConnector: UserAnswersCacheConnector,
                schemeCacheConnector: SchemeCacheConnector,
                lockCacheConnector: LockCacheConnector)
    extends DataRetrievalImpl(dataCacheConnector, schemeCacheConnector, lockCacheConnector) {
    def callTransform[A](request: AuthenticatedRequest[A]): Future[OptionalDataRequest[A]] =
      transform(request)
  }

  private val dataCacheConnector: UserAnswersCacheConnector = mock[UserAnswersCacheConnector]
  private val schemeCacheConnector: SchemeCacheConnector = mock[SchemeCacheConnector]
  private val lockCacheConnector: LockCacheConnector = mock[LockCacheConnector]
  private val lock: MigrationLock = MigrationLock("pstr", "credId", "psaId")

  "Data Retrieval Action" when {
    "there is no data in the cache" must {
      "set userAnswers to 'None' in the request" in {
        when(dataCacheConnector.fetch(any())(any(), any())) thenReturn Future(None)
        when(schemeCacheConnector.fetch(any(), any())) thenReturn Future(Some(Json.toJson(lock)))
        val action = new Harness(dataCacheConnector, schemeCacheConnector, lockCacheConnector)

        val futureResult = action.callTransform(
          AuthenticatedRequest(
            request = fakeRequest,
            externalId = "id",
            psaId = PsaId(psaId)
          )
        )

        whenReady(futureResult) { result =>
          result.userAnswers.isEmpty mustBe true
        }
      }
    }

    "there is data in the cache" must {
      "build a userAnswers object and add it to the request" in {
        when(dataCacheConnector.fetch(any())(any(), any())) thenReturn Future.successful(Some(Json.obj()))
        when(schemeCacheConnector.fetch(any(), any())) thenReturn Future(Some(Json.toJson(lock)))
        val action = new Harness(dataCacheConnector, schemeCacheConnector, lockCacheConnector)

        val futureResult = action.callTransform(
          AuthenticatedRequest(
            request = fakeRequest,
            externalId = "id",
            psaId = PsaId(psaId)
          )
        )

        whenReady(futureResult) { result =>
          result.userAnswers.isDefined mustBe true
        }
      }
    }

    "there is no scheme data in the cache but user holds lock in locking table" must {
      "retrieve lock from current locks using credId and add it to the request" in {
        when(dataCacheConnector.fetch(any())(any(), any())) thenReturn Future.successful(Some(Json.obj()))
        when(schemeCacheConnector.fetch(any(), any())) thenReturn Future(None)
        when(schemeCacheConnector.save(any())(any(), any())) thenReturn Future(Json.toJson(lock))
        when(lockCacheConnector.getLockByUser(any(), any())) thenReturn Future(Some(lock))
        val action = new Harness(dataCacheConnector, schemeCacheConnector, lockCacheConnector)

        val futureResult = action.callTransform(
          AuthenticatedRequest(
            request = fakeRequest,
            externalId = "id",
            psaId = PsaId(psaId)
          )
        )

        whenReady(futureResult) { result =>
          result.lock.isDefined mustBe true
        }
      }
    }

    "there is no scheme data in the cache and user does not hold lock in locking table" must {
      "create request without lock" in {
        when(dataCacheConnector.fetch(any())(any(), any())) thenReturn Future.successful(Some(Json.obj()))
        when(schemeCacheConnector.fetch(any(), any())) thenReturn Future(None)
        when(lockCacheConnector.getLockByUser(any(), any())) thenReturn Future(None)
        val action = new Harness(dataCacheConnector, schemeCacheConnector, lockCacheConnector)

        val futureResult = action.callTransform(
          AuthenticatedRequest(
            request = fakeRequest,
            externalId = "id",
            psaId = PsaId(psaId)
          )
        )

        whenReady(futureResult) { result =>
          result.lock.isDefined mustBe false
        }
      }
    }
  }
}
