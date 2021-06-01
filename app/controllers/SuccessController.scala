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
import connectors.cache.UserAnswersCacheConnector
import controllers.actions._
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import play.twirl.api.Html
import renderer.Renderer
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import uk.gov.hmrc.viewmodels._

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class SuccessController @Inject()(
                                        override val messagesApi: MessagesApi,
                                        identify: AuthAction,
                                        getData: DataRetrievalAction,
                                        requireData: DataRequiredAction,
                                        val controllerComponents: MessagesControllerComponents,
                                        userAnswersCacheConnector: UserAnswersCacheConnector,
                                        renderer: Renderer,
                                        config: AppConfig
                                      )(implicit ec: ExecutionContext)
  extends FrontendBaseController
    with I18nSupport {

  def onPageLoad: Action[AnyContent] =
    (identify andThen getData andThen requireData).async {
      implicit request =>

        val confirmationPanelText: String =
          Html(s"""<span class="heading-large govuk-!-font-weight-bold">${msg"messages__complete__application_number_is".withArgs("1234567890").resolve}</span>""").toString

        val email: String = "dummy@dummy.com"  //TODO Get the correct email id once email pages are implemented

              val json = Json.obj(
                "panelHtml" -> confirmationPanelText,
                "email" -> email,
                "yourSchemesLink" -> routes.TaskListController.onPageLoad().url,
                "submitUrl" -> routes.LogoutController.onPageLoad().url,
              )

              renderer.render("success.njk", json).flatMap { viewHtml =>
              //  userAnswersCacheConnector.remove(request.lock.pstr).map { _ =>
                  Future.successful(Ok(viewHtml))
            //    }
              }
    }

}
