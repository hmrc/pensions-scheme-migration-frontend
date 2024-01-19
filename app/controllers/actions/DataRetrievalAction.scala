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

package controllers.actions


import com.google.inject.{ImplementedBy, Inject}
import connectors.cache.{CurrentPstrCacheConnector, LockCacheConnector, UserAnswersCacheConnector}
import models.MigrationLock
import models.requests.{AuthenticatedRequest, OptionalDataRequest}
import play.api.libs.json._
import play.api.mvc.ActionTransformer
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import utils.UserAnswers

import scala.concurrent.{ExecutionContext, Future}

class DataRetrievalImpl @Inject()(dataConnector: UserAnswersCacheConnector,
                                  schemeCacheConnector: CurrentPstrCacheConnector,
                                  lockCacheConnector: LockCacheConnector)
                                 (implicit val executionContext: ExecutionContext)
  extends DataRetrievalAction {

  override protected def transform[A](request: AuthenticatedRequest[A]): Future[OptionalDataRequest[A]] = {

    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)

    retrieveLock.flatMap {

      case Some(lock) =>
        lockCacheConnector.getLockOnScheme(lock.pstr).flatMap {

          case None =>
            lockCacheConnector.setLock(lock).flatMap { _ =>
              fetchDataAndFormRequest(lock, viewOnly = false, request)
            }

          case Some(lockOnScheme) =>
            fetchDataAndFormRequest(lock, lock.credId != lockOnScheme.credId, request)
        }

      case _ =>
        Future.successful(OptionalDataRequest(request.request,None,request.psaId,None))

    }
  }

    private def fetchDataAndFormRequest[A](lock: MigrationLock, viewOnly: Boolean, request: AuthenticatedRequest[A])
                                          (implicit hc: HeaderCarrier): Future[OptionalDataRequest[A]] =
      dataConnector.fetch(lock.pstr) map {

      case None =>
        OptionalDataRequest(request.request, None, request.psaId, Some(lock), viewOnly)

      case Some(data) =>
        OptionalDataRequest(request.request, Some(UserAnswers(data.as[JsObject])), request.psaId, Some(lock), viewOnly)
    }

  private def retrieveLock(implicit hc: HeaderCarrier): Future[Option[MigrationLock]] =
    schemeCacheConnector.fetch flatMap {
      case Some(value) =>
        value.validate[MigrationLock] match {
          case JsSuccess(value, _) => Future.successful(Some(value))
          case JsError(errors) => throw JsResultException(errors)
        }
      case _ =>
        lockCacheConnector.getLockByUser flatMap {
          case Some(lock) => schemeCacheConnector.save(Json.toJson(lock)).map(_.asOpt[MigrationLock])
          case _ => Future.successful(None)
        }
    }
}

@ImplementedBy(classOf[DataRetrievalImpl])
trait DataRetrievalAction extends ActionTransformer[AuthenticatedRequest, OptionalDataRequest]

case class NoSchemeDataWasSaved(e: String) extends Exception(e)


