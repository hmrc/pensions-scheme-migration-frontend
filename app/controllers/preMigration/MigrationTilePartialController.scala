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

package controllers.preMigration

import config.AppConfig
import connectors.cache.BulkMigrationQueueConnector
import controllers.actions._
import models.PageLink
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.Text
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class MigrationTilePartialController @Inject()(
                                                appConfig: AppConfig,
                                                override val messagesApi: MessagesApi,
                                                authenticate: AuthAction,
                                                bulkMigrationQueueConnector: BulkMigrationQueueConnector,
                                                val controllerComponents: MessagesControllerComponents
                                              )(implicit ec: ExecutionContext)
  extends FrontendBaseController
    with I18nSupport
    {

  def migrationPartial: Action[AnyContent] = authenticate.async { implicit request =>
    val links: Future[Seq[PageLink]] = bulkMigrationQueueConnector.isRequestInProgress(request.psaId.id).map {
      case false =>
        Seq(PageLink("add-pension-schemes", appConfig.schemesMigrationTransfer, Text(Messages("messages__migrationLink__addSchemesLink"))),
          PageLink("add-rac-dacs", appConfig.racDacMigrationTransfer, Text(Messages("messages__migrationLink__addRacDacsLink"))))
      case true =>
        Seq(PageLink("add-pension-schemes", appConfig.schemesMigrationTransfer, Text(Messages("messages__migrationLink__addSchemesLink"))),
          PageLink("check-rac-dacs", appConfig.racDacMigrationCheckStatus, Text(Messages("messages__migrationLink__checkStatusRacDacsLink"))))
    }

    links.flatMap { migrationLinks =>
      Future.successful(Ok(
        views.html.preMigration.MigrationLinksPartialView(migrationLinks)
      ))
    }
  }
}
