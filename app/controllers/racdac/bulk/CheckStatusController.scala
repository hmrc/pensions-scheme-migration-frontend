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

package controllers.racdac.bulk

import config.AppConfig
import connectors.cache.BulkMigrationQueueConnector
import connectors.{AncillaryPsaException, ListOfSchemesConnector}
import controllers.actions._
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CheckStatusController @Inject()(val appConfig: AppConfig,
                                      override val messagesApi: MessagesApi,
                                      authenticate: AuthAction,
                                      bulkMigrationQueueConnector: BulkMigrationQueueConnector,
                                      listOfSchemesConnector: ListOfSchemesConnector,
                                      val controllerComponents: MessagesControllerComponents
                                     )(implicit val executionContext: ExecutionContext) extends
  FrontendBaseController with I18nSupport {

  def onPageLoad: Action[AnyContent] = authenticate.async { implicit request =>
    bulkMigrationQueueConnector.isAllFailed(request.psaId.id).flatMap {
      case Some(true) => Future.successful(Redirect(controllers.racdac.bulk.routes.FinishedStatusController.onPageLoad()))
      case Some(false) => Future.successful(Redirect(controllers.racdac.bulk.routes.InProgressController.onPageLoad()))
      case None =>
        listOfSchemesConnector.getListOfSchemes(request.psaId.id).map {
          case Right(listSchemes) =>
            if (listSchemes.items.exists(_.exists(_.racDac))) {
              Redirect(appConfig.psaOverviewUrl)
            }
            else {
              Redirect(controllers.preMigration.routes.NoSchemeToAddController.onPageLoadRacDac())
            }
          case _ =>
            Redirect(appConfig.psaOverviewUrl)
        } recoverWith {
          case _: AncillaryPsaException =>
            Future.successful(Redirect(controllers.preMigration.routes.CannotMigrateController.onPageLoad()))
        }
    }
  }
}

