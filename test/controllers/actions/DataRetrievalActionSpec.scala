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

package controllers.actions

import base.SpecBase
import connectors.cache.{CurrentPstrCacheConnector, LockCacheConnector, UserAnswersCacheConnector}
import models.MigrationLock
import models.requests.{AuthenticatedRequest, OptionalDataRequest}
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito.when
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.libs.json.{JsResultException, Json}
import uk.gov.hmrc.domain.PsaId
import utils.Data.psaId

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class DataRetrievalActionSpec
  extends SpecBase
    with ScalaFutures {

  class Harness(dataCacheConnector: UserAnswersCacheConnector,
                schemeCacheConnector: CurrentPstrCacheConnector,
                lockCacheConnector: LockCacheConnector)
    extends DataRetrievalImpl(dataCacheConnector, schemeCacheConnector, lockCacheConnector) {
    def callTransform[A](request: AuthenticatedRequest[A]): Future[OptionalDataRequest[A]] =
      transform(request)
  }

  private val dataCacheConnector: UserAnswersCacheConnector = mock[UserAnswersCacheConnector]
  private val schemeCacheConnector: CurrentPstrCacheConnector = mock[CurrentPstrCacheConnector]
  private val lockCacheConnector: LockCacheConnector = mock[LockCacheConnector]
  private val lock: MigrationLock = MigrationLock("pstr", "credId", "psaId")

  "Data Retrieval Action" when {

    "there is no scheme data in the cache but user holds lock in locking table on the current scheme and migration data exists" must {
      "retrieve lock from current locks using credId and add it to the request and return current migration data with viewOnly false" in {
        when(dataCacheConnector.fetch(any())(any(), any())) thenReturn Future.successful(Some(Json.obj()))
        when(schemeCacheConnector.fetch(any(), any())) thenReturn Future(None)
        when(schemeCacheConnector.save(any())(any(), any())) thenReturn Future(Json.toJson(lock))
        when(lockCacheConnector.getLockByUser(any(), any())) thenReturn Future(Some(lock))
        when(lockCacheConnector.getLockOnScheme(any())(any(), any())) thenReturn Future(Some(lock))
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
          result.lock.isDefined mustBe true
          result.viewOnly mustBe false
        }
      }
    }

    "there is no scheme data in the cache and user does not hold lock in locking table" must {
      "create request without lock and userAnswers" in {
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
          result.userAnswers.isDefined mustBe false
          result.lock.isDefined mustBe false
        }
      }
    }

    "there is scheme data in the cache and user holds lock the lock but scheme is locked by another user" must {
      "return lock from scheme data and return current migration data with viewOnly true" in {
        when(dataCacheConnector.fetch(any())(any(), any())) thenReturn Future.successful(Some(Json.obj()))
        when(schemeCacheConnector.fetch(any(), any())) thenReturn Future.successful(Some(Json.toJson(lock)))
        when(lockCacheConnector.getLockOnScheme(any())(any(), any())) thenReturn Future(Some(lock.copy(credId = "non-matching-id")))
        val action = new Harness(dataCacheConnector, schemeCacheConnector, lockCacheConnector)

        val futureResult = action.callTransform(
          AuthenticatedRequest(
            request = fakeRequest,
            externalId = "non-matching-id",
            psaId = PsaId(psaId)
          )
        )

        whenReady(futureResult) { result =>
          result.userAnswers.isDefined mustBe true
          result.lock.isDefined mustBe true
          result.viewOnly mustBe true
        }
      }
    }

    "there is scheme data in the cache but data is in incorrect format" must {
      "return JsResultException" in {
        when(schemeCacheConnector.fetch(any(), any())) thenReturn Future.successful(Some(Json.toJson("invalid-key" -> "invalid-value")))
        val action = new Harness(dataCacheConnector, schemeCacheConnector, lockCacheConnector)

        val futureResult = action.callTransform(
          AuthenticatedRequest(
            request = fakeRequest,
            externalId = "id",
            psaId = PsaId(psaId)
          )
        )

        whenReady(futureResult.failed) { result =>
          result mustBe a[JsResultException]
        }
      }
    }

    "there is no lock on the scheme which user is trying to access but no data exists in migration-data" must {
      "set lock for current user and return request with empty userAnswers but viewOnly false" in {
        when(dataCacheConnector.fetch(any())(any(), any())) thenReturn Future.successful(None)
        when(schemeCacheConnector.fetch(any(), any())) thenReturn Future.successful(Some(Json.toJson(lock)))
        when(lockCacheConnector.getLockOnScheme(any())(any(), any())) thenReturn Future(None)
        when(lockCacheConnector.setLock(any())(any(), any())) thenReturn Future(Json.obj())
        val action = new Harness(dataCacheConnector, schemeCacheConnector, lockCacheConnector)

        val futureResult = action.callTransform(
          AuthenticatedRequest(
            request = fakeRequest,
            externalId = "id",
            psaId = PsaId(psaId)
          )
        )

        whenReady(futureResult) { result =>
          result.userAnswers.isDefined mustBe false
          result.lock.isDefined mustBe true
          result.viewOnly mustBe false
        }
      }
    }

    "there is no lock on the scheme which user is trying to access and data exists in migration-data" must {
      "set lock for current user and return request with userAnswers but viewOnly false" in {
        when(dataCacheConnector.fetch(any())(any(), any())) thenReturn Future.successful(Some(Json.obj()))
        when(schemeCacheConnector.fetch(any(), any())) thenReturn Future.successful(Some(Json.toJson(lock)))
        when(lockCacheConnector.getLockOnScheme(any())(any(), any())) thenReturn Future(None)
        when(lockCacheConnector.setLock(any())(any(), any())) thenReturn Future(Json.obj())
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
          result.lock.isDefined mustBe true
          result.viewOnly mustBe false
        }
      }
    }

  }
}
