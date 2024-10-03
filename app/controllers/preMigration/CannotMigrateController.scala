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
import controllers.actions.AuthAction
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, Call, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import uk.gov.hmrc.viewmodels.MessageInterpolators
import views.html.preMigration.CannotMigrateView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CannotMigrateController @Inject()(val appConfig: AppConfig,
                                        override val messagesApi: MessagesApi,
                                        authenticate: AuthAction,
                                        val controllerComponents: MessagesControllerComponents,
                                        cannotMigrateView: CannotMigrateView
                                    )(implicit val executionContext: ExecutionContext) extends
  FrontendBaseController with I18nSupport {

  def onPageLoad: Action[AnyContent] = authenticate.async { implicit request =>
    Future.successful(Ok(
      cannotMigrateView(
        msg"messages__administrator__overview".resolve,
        Call("GET", appConfig.psaOverviewUrl)
      )
    ))
  }
}
