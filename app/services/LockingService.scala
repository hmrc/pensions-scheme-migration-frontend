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

import connectors.cache.{LockCacheConnector, SchemeCacheConnector}
import models.MigrationLock
import models.requests.AuthenticatedRequest
import play.api.libs.json.Json
import play.api.mvc.Results.Redirect
import play.api.mvc.{Call, Result}
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class LockingService @Inject()(lockCacheConnector: LockCacheConnector,
                               schemeCacheConnector: SchemeCacheConnector){


//TODO - Call on listOfSchemes when user selects a scheme
  def initialLockSetupAndRedirect(pstr: String, request: AuthenticatedRequest[_])
                      (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Result] = {
    val lock = MigrationLock(pstr, request.externalId, request.psaId.id)
    schemeCacheConnector.save(Json.toJson(lock)).flatMap { _ =>
      lockCacheConnector.getLockOnScheme(pstr) flatMap {
        case Some(lockOnScheme) if lockOnScheme.credId != lock.credId => // redirect to locked page
          Future.successful(Redirect(controllers.routes.IndexController.onPageLoad()))
        case Some(lockOnScheme) if lockOnScheme.credId == lock.credId =>
          Future.successful(Redirect(controllers.routes.TaskListController.onPageLoad()))
        case _ =>
          lockCacheConnector.removeLockByUser.flatMap {_ =>
            lockCacheConnector.setLock(lock).map {_ =>
              Redirect(controllers.routes.TaskListController.onPageLoad())
            }
          }
      }
    }
  }

  //TODO - Call on signout & returnLink on task-list page
  def releaseLockAndRedirect(url: String)
                            (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Result] = {
    lockCacheConnector.removeLockByUser.map { _ =>
      Redirect(Call("GET", url))
    }
  }
}
