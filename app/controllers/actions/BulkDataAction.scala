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

import com.google.inject.{Inject, ImplementedBy}
import config.AppConfig
import connectors.cache.CurrentPstrCacheConnector
import connectors._
import models.requests.{AuthenticatedRequest, BulkDataRequest}
import models.{Items, MinPSA}
import play.api.libs.json.{JsSuccess, Json}
import play.api.mvc.Results.Redirect
import play.api.mvc.{ActionRefiner, Result}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import scala.concurrent.{ExecutionContext, Future}

class BulkRetrievalImpl @Inject()(schemeCacheConnector: CurrentPstrCacheConnector,
                                  listOfSchemesConnector: ListOfSchemesConnector,
                                  minimalDetailsConnector: MinimalDetailsConnector,
                                  appConfig: AppConfig,
                                  isRequired: Boolean)(implicit val executionContext: ExecutionContext) extends BulkRetrieval {

  override protected def refine[A](request: AuthenticatedRequest[A]): Future[Either[Result, BulkDataRequest[A]]] = {
    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)
    schemeCacheConnector.fetch flatMap {
      case None if isRequired =>
        Future.successful(Left(Redirect(appConfig.psaOverviewUrl)))
      case None =>
        getBulkRacDacRequest(request)
      case Some(data) =>
        ((data \ "schemes").validate[List[Items]], (data \ "md").validate[MinPSA]) match {
          case (JsSuccess(schemes, _), JsSuccess(md, _)) =>
            Future(Right(BulkDataRequest(request, md, schemes)))
          case _ =>
            Future.successful(Left(Redirect(appConfig.psaOverviewUrl)))
        }
    }
  }

  private def getBulkRacDacRequest[A](request: AuthenticatedRequest[A]): Future[Either[Result, BulkDataRequest[A]]] = {
    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)
    (for {
      listSchemes <- listOfSchemesConnector.getListOfSchemes(request.psaId.id)
      md <- minimalDetailsConnector.getPSADetails(request.psaId.id)
    } yield {
      listSchemes match {
        case Right(listOfSchemes) =>
          val listSchemes = listOfSchemes.items.getOrElse(Nil).filter(_.racDac)
          val data = Json.obj("schemes" -> Json.toJson(listSchemes), "md" -> Json.toJson(md))
          schemeCacheConnector.save(data)
            .map { _ =>
              Right(BulkDataRequest(request, md, listSchemes))
            }
        case _ =>
          Future(Right(BulkDataRequest(request, md, Nil)))
      }
    }).flatten.recoverWith {
      case _: AncillaryPsaException =>
        Future.successful(Left(Redirect(controllers.preMigration.routes.CannotMigrateController.onPageLoad())))
      case _: ListOfSchemes5xxException =>
        Future.successful(Left(Redirect(controllers.preMigration.routes.ThereIsAProblemController.onPageLoad())))
      case _: DelimitedAdminException =>
        Future.successful(Left(Redirect(appConfig.psaDelimitedUrl)))
    }
  }
}

class BulkDataActionImpl @Inject()(schemeCacheConnector: CurrentPstrCacheConnector,
                                   listOfSchemesConnector: ListOfSchemesConnector,
                                   minimalDetailsConnector: MinimalDetailsConnector,
                                   appConfig: AppConfig)(implicit ec: ExecutionContext) extends BulkDataAction {
  override def apply(isRequired: Boolean): BulkRetrieval = {
    new BulkRetrievalImpl(schemeCacheConnector, listOfSchemesConnector, minimalDetailsConnector, appConfig, isRequired)
  }
}

@ImplementedBy(classOf[BulkRetrievalImpl])
trait BulkRetrieval extends ActionRefiner[AuthenticatedRequest, BulkDataRequest]

@ImplementedBy(classOf[BulkDataActionImpl])
trait BulkDataAction {
  def apply(isRequired: Boolean): BulkRetrieval
}
