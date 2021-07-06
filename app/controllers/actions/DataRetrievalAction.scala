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


import com.google.inject.{ImplementedBy, Inject}
import connectors.cache.{LockCacheConnector, SchemeCacheConnector, UserAnswersCacheConnector}
import models.MigrationLock
import models.requests.{AuthenticatedRequest, OptionalDataRequest}
import play.api.libs.json.{JsError, JsObject, JsResultException, JsSuccess, Json}
import play.api.mvc.ActionTransformer
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import utils.UserAnswers

import scala.concurrent.{ExecutionContext, Future}

class DataRetrievalImpl @Inject()(dataConnector: UserAnswersCacheConnector,
                                  schemeCacheConnector: SchemeCacheConnector,
                                  lockCacheConnector: LockCacheConnector)
                                 (implicit val executionContext: ExecutionContext)
  extends DataRetrievalAction {

  override protected def transform[A](request: AuthenticatedRequest[A]): Future[OptionalDataRequest[A]] = {
    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)

    retrieveLock.flatMap { lock =>

      dataConnector.fetch("pstr") map {
        case None =>
          OptionalDataRequest(
            request = request.request,
            userAnswers = None,
            psaId = request.psaId,
            lock = lock
          )
        case Some(data) =>
          OptionalDataRequest(
            request = request.request,
            userAnswers = Some(UserAnswers(data.as[JsObject])),
            psaId = request.psaId,
            lock = lock
          )
      }
    }
  }

  private def retrieveLock(implicit hc: HeaderCarrier): Future[MigrationLock] =
    schemeCacheConnector.fetch flatMap {
      case Some(value) =>
        value.validate[MigrationLock] match {
          case JsSuccess(value, _) =>  Future.successful(value)
          case JsError(errors) => throw JsResultException(errors)
        }
      case _ =>
        lockCacheConnector.getLockByUser flatMap {
          case Some(lock) => schemeCacheConnector.save(Json.toJson(lock)).map(_.as[MigrationLock])
          case _ => throw NoSchemeDataWasSaved
        }

  }
}

@ImplementedBy(classOf[DataRetrievalImpl])
trait DataRetrievalAction extends ActionTransformer[AuthenticatedRequest, OptionalDataRequest]

case object NoSchemeDataWasSaved extends Exception


