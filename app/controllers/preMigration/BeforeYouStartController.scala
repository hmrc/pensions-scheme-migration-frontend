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
import connectors.MinimalDetailsConnector
import controllers.Retrievals
import controllers.actions.{AuthAction, DataRetrievalAction}
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import renderer.Renderer
import uk.gov.hmrc.nunjucks.NunjucksSupport
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class BeforeYouStartController @Inject()(
                                           appConfig : AppConfig,
                                           override val messagesApi: MessagesApi,
                                           authenticate: AuthAction,
                                           getData: DataRetrievalAction,
                                           minimalDetailsConnector: MinimalDetailsConnector,
                                           val controllerComponents: MessagesControllerComponents,
                                           val renderer: Renderer
                                         )(implicit val ec: ExecutionContext)
  extends FrontendBaseController
    with I18nSupport
    with Retrievals
    with NunjucksSupport {

  def onPageLoad: Action[AnyContent] =
    (authenticate andThen getData).async {
      implicit request =>
        minimalDetailsConnector.getPSAName.flatMap { psaName =>
            renderer.render(
              template = "preMigration/beforeYouStart.njk",
              ctx = Json.obj(
                "pageTitle" -> Messages("messages__BeforeYouStart__title"),
                "continueUrl" -> controllers.routes.TaskListController.onPageLoad().url,
                "psaName" -> psaName,
                "returnUrl" -> appConfig.psaOverviewUrl
              )
            ).map(Ok(_))
        }

    }

}
