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

package controllers

import config.AppConfig
import models.Scheme
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.AddingSchemeView

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class AddingSchemeController @Inject()(val appConfig: AppConfig,
                                       override val messagesApi: MessagesApi,
                                       val controllerComponents: MessagesControllerComponents,
                                       addingSchemeView: AddingSchemeView
                                      )(implicit val executionContext: ExecutionContext) extends
  FrontendBaseController with I18nSupport {

  def onPageLoad: Action[AnyContent] = Action { implicit request =>
    val returnUrl = controllers.preMigration.routes.ListOfSchemesController.onPageLoad(Scheme).url

    Ok(addingSchemeView(
      returnUrl,
      appConfig.yourPensionSchemesUrl,
      appConfig.contactHmrcUrl
    ))
  }

}
