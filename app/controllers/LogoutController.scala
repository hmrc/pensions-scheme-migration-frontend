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

package controllers

import config.AppConfig
import connectors.cache.LockCacheConnector
import controllers.actions.AuthAction
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.auth.core.{AuthConnector, AuthorisedFunctions, MissingBearerToken}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class LogoutController @Inject()(
                                  override val authConnector: AuthConnector,
                                  appConfig: AppConfig,
                                  val controllerComponents: MessagesControllerComponents,
                                  authenticate: AuthAction,
                                  lockCacheConnector: LockCacheConnector
                                )(implicit val ec: ExecutionContext)
  extends FrontendBaseController
    with I18nSupport with AuthorisedFunctions {

  def onPageLoad(): Action[AnyContent] = Action.async {
    implicit request =>
      authorised().retrieve(uk.gov.hmrc.auth.core.retrieve.v2.Retrievals.externalId) {
        case Some(id) =>
          lockCacheConnector.removeLockByUser.map { _ =>
              Redirect(appConfig.serviceSignOut).withNewSession
            }
        case _ =>
          Future.successful(Redirect(routes.IndexController.onPageLoad()))
      } recover {
        case _: MissingBearerToken =>
          Redirect(appConfig.serviceSignOut).withNewSession
      }
  }

  def keepAlive: Action[AnyContent] = Action.async {
  Future successful Ok("OK")
  }
}
