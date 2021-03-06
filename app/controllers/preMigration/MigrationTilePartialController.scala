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

package controllers.preMigration

import config.AppConfig
import connectors.cache.FeatureToggleConnector
import controllers.actions._
import models.FeatureToggle.Enabled
import models.FeatureToggleName.MigrationTransfer
import models.PageLink
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import renderer.Renderer
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import uk.gov.hmrc.viewmodels.NunjucksSupport

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class MigrationTilePartialController @Inject()(
                                            appConfig: AppConfig,
                                            override val messagesApi: MessagesApi,
                                            authenticate: AuthAction,
                                            featureToggleConnector: FeatureToggleConnector,
                                            val controllerComponents: MessagesControllerComponents,
                                            renderer: Renderer
                                          )(implicit ec: ExecutionContext)
  extends FrontendBaseController
    with I18nSupport
    with NunjucksSupport {

  def migrationPartial: Action[AnyContent] = authenticate.async { implicit request =>
    val links: Future[Seq[PageLink]] = featureToggleConnector.get(MigrationTransfer.asString).map {
      case Enabled(_) =>
        Seq(
          PageLink("add-pension-schemes", appConfig.schemesMigrationTransfer, msg"messages__migrationLink__addSchemesLink"),
          PageLink("add-rac-dacs", appConfig.racDacMigrationTransfer, msg"messages__migrationLink__addRacDacsLink")
        )
      case _ =>
        Seq(
          PageLink("view-pension-schemes", appConfig.schemesMigrationViewOnly, msg"messages__migrationLink__viewSchemesLink"),
          PageLink("view-rac-dacs", appConfig.racDacMigrationViewOnly, msg"messages__migrationLink__viewRacDacsLink")
        )
    }

    links.flatMap { migrationLinks =>
      renderer.render(
        template = "preMigration/migrationLinksPartial.njk",
        ctx = Json.obj("links" -> Json.toJson(migrationLinks))
      ).map(Ok(_))
    }
  }

}
