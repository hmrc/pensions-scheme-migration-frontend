/*
 * Copyright 2023 HM Revenue & Customs
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
import connectors.ListOfSchemesConnector
import connectors.cache.CurrentPstrCacheConnector
import controllers.actions._
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import renderer.Renderer
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ConfirmationController @Inject()(appConfig: AppConfig,
                                       override val messagesApi: MessagesApi,
                                       authenticate: AuthAction,
                                       currentPstrCacheConnector: CurrentPstrCacheConnector,
                                       val controllerComponents: MessagesControllerComponents,
                                       listOfSchemesConnector: ListOfSchemesConnector,
                                       renderer: Renderer
                                      )(implicit ec: ExecutionContext)
  extends FrontendBaseController
    with I18nSupport {

  def onPageLoad: Action[AnyContent] =
    authenticate.async {
      implicit request => {
        currentPstrCacheConnector.fetch.flatMap {
          case None => Future.successful(Redirect(appConfig.psaOverviewUrl))
          case Some(jsValue) =>
            val optEmail = (jsValue \ "confirmationData" \ "email").asOpt[String]
            val optPsaId = (jsValue \ "confirmationData" \ "psaId").asOpt[String]
            (optEmail, optPsaId) match {
              case (Some(email), Some(psaId)) =>
                val json = Json.obj(
                  "email" -> email,
                  "finishUrl" -> appConfig.psaOverviewUrl
                )
                listOfSchemesConnector.removeCache(psaId).flatMap { _ =>
                  currentPstrCacheConnector.remove.flatMap { _ =>
                    renderer.render("racdac/confirmation.njk", json).map(Ok(_))
                  }
                }
              case _ =>
                Future.successful(Redirect(appConfig.psaOverviewUrl))
            }
        }
      }
    }
}
