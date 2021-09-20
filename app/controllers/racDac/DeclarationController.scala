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

package controllers.racDac

import config.AppConfig
import connectors.MinimalDetailsConnector
import controllers.actions.AuthAction
import javax.inject.Inject
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import renderer.Renderer
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import scala.concurrent.ExecutionContext

class DeclarationController @Inject()(
                                       appConfig: AppConfig,
                                       override val messagesApi: MessagesApi,
                                       authenticate: AuthAction,
                                       minimalDetailsConnector: MinimalDetailsConnector,
                                       val controllerComponents: MessagesControllerComponents,
                                       renderer: Renderer
                                     )(implicit val executionContext: ExecutionContext)
  extends FrontendBaseController
    with I18nSupport
     {

  def onPageLoad: Action[AnyContent] =
    authenticate.async {
      implicit request =>
        minimalDetailsConnector.getPSAName.flatMap {
          psaName =>
            val json = Json.obj(
              "psaName" -> psaName,
              "submitUrl" -> controllers.racDac.routes.DeclarationController.onSubmit().url,
              "returnUrl" -> appConfig.psaOverviewUrl
            )
            renderer.render("racDac/declaration.njk", json).map(Ok(_))
        }
  }

  def onSubmit: Action[AnyContent] =
    authenticate {
      Redirect(controllers.routes.IndexController.onPageLoad().url)
  }

}
