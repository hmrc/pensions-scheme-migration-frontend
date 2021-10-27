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
import connectors.cache.CurrentPstrCacheConnector
import controllers.actions._
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import renderer.Renderer
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class ConfirmationController @Inject()(appConfig: AppConfig,
                                       override val messagesApi: MessagesApi,
                                       authenticate: AuthAction,
                                       currentPstrCacheConnector: CurrentPstrCacheConnector,
                                       getData: BulkDataAction,
                                       val controllerComponents: MessagesControllerComponents,
                                       renderer: Renderer
                                      )(implicit ec: ExecutionContext)
  extends FrontendBaseController
    with I18nSupport {

  def onPageLoad: Action[AnyContent] =
    (authenticate andThen getData(true)).async {
      implicit request =>
        val json = Json.obj(
          "email" -> request.md.email,
          "finishUrl" -> appConfig.psaOverviewUrl
        )
        currentPstrCacheConnector.remove.flatMap { _ =>
          renderer.render("racdac/confirmation.njk", json).map(Ok(_))
        }
    }
}