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

package services

import base.SpecBase
import connectors.cache.{CurrentPstrCacheConnector, LockCacheConnector}
import models.requests.AuthenticatedRequest
import org.mockito.Matchers
import org.mockito.Matchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.json.Json
import play.api.mvc.AnyContent
import play.api.mvc.Results.Ok
import play.api.test.Helpers._
import uk.gov.hmrc.domain.PsaId
import uk.gov.hmrc.http.HeaderCarrier
import utils.Data._

import scala.concurrent.{ExecutionContext, Future}

class LockingServiceSpec extends SpecBase with MockitoSugar {

  private val mockLockCacheConnector = mock[LockCacheConnector]
  private val mockSchemeCacheConnector = mock[CurrentPstrCacheConnector]
  private implicit lazy val hc: HeaderCarrier = HeaderCarrier()
  implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global

  val service=new LockingService(mockLockCacheConnector,mockSchemeCacheConnector)

  "initialLockSetupAndRedirect" must {

     " Redirect to Locked Page if scheme is locked by diffrent user" in {

       val request : AuthenticatedRequest[AnyContent] = AuthenticatedRequest(fakeRequest, "", PsaId(psaId))
       when(mockSchemeCacheConnector.save(any())(any(),any())).thenReturn(Future.successful(Json.obj()))
       when(mockLockCacheConnector.getLockOnScheme(Matchers.eq(pstr))(any(),any())).thenReturn(Future.successful(Some(migrationLock)))
       redirectLocation(service.initialLockSetupAndRedirect(pstr,request)) mustBe Some(controllers.routes.IndexController.onPageLoad().url)
       }
     }

  " Redirect to TaskList Page if scheme is locked by same user" in {

    val request : AuthenticatedRequest[AnyContent] = AuthenticatedRequest(fakeRequest, credId, PsaId(psaId))
    when(mockSchemeCacheConnector.save(any())(any(),any())).thenReturn(Future.successful(Json.obj()))
    when(mockLockCacheConnector.getLockOnScheme(Matchers.eq(pstr))(any(),any())).thenReturn(Future.successful(Some(migrationLock)))
    redirectLocation(service.initialLockSetupAndRedirect(pstr,request)) mustBe Some(controllers.routes.TaskListController.onPageLoad().url)
  }

  " Remove other locks and setLock on the scheme and Redirect to TaskList Page if scheme is not locked by any user" in {

    val request : AuthenticatedRequest[AnyContent] = AuthenticatedRequest(fakeRequest, credId, PsaId(psaId))
    when(mockLockCacheConnector.removeLockByUser(any(),any())).thenReturn(Future.successful(Ok))
    when(mockLockCacheConnector.setLock(Matchers.eq(migrationLock))(any(),any())).thenReturn(Future.successful(Json.obj()))
    redirectLocation(service.initialLockSetupAndRedirect(pstr,request)) mustBe Some(controllers.routes.TaskListController.onPageLoad().url)
  }

  "releaseLockAndRedirect" must {
    " Release the lock and redirect the user" in {
      val url=controllers.routes.TaskListController.onPageLoad().url
      when(mockLockCacheConnector.removeLockByUser(any(),any())).thenReturn(Future.successful(Ok))
      redirectLocation(service.releaseLockAndRedirect(url)) mustBe Some(url)
    }
  }

}
