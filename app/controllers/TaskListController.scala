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

import connectors.cache.UserAnswersCacheConnector
import controllers.actions.{DataRetrievalAction, AuthAction}
import helpers.TaskListHelper
import models.requests.OptionalDataRequest
import play.api.i18n.{MessagesApi, I18nSupport}
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import renderer.Renderer
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.UserAnswers

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class TaskListController @Inject()(
                                    override val messagesApi: MessagesApi,
                                    authenticate: AuthAction,
                                    getData: DataRetrievalAction,
                                    taskListHelper: TaskListHelper,
                                    userAnswersCacheConnector: UserAnswersCacheConnector,
                                    val controllerComponents: MessagesControllerComponents,
                                    renderer: Renderer
                                  )(implicit val executionContext: ExecutionContext)
  extends FrontendBaseController
    with I18nSupport
    with Retrievals {

  def onPageLoad: Action[AnyContent] = (authenticate andThen getData).async {
    implicit request =>

      (request.userAnswers, request.lock) match {
        case (_, None) =>
          Future.successful(Redirect(controllers.routes.IndexController.onPageLoad())) //TODO Change to list of schemes page once implemented

        case (Some(ua), _) =>
          implicit val userAnswers: UserAnswers = ua
          renderView

        case (None, Some(lock)) =>
          implicit val userAnswers: UserAnswers = UserAnswers(Json.obj()) //TODO once getSchemeDetails API is implemented, fetch data from API
          userAnswersCacheConnector.save(lock, userAnswers.data).flatMap { _ =>
            renderView
          }
      }
  }

  private def renderView(implicit userAnswers: UserAnswers, request: OptionalDataRequest[_]): Future[Result] = {
    val json = Json.obj(
      "taskSections" -> taskListHelper.taskList(request.viewOnly),
      "schemeName" -> taskListHelper.getSchemeName
    )
    renderer.render("taskList.njk", json).map(Ok(_))
  }

}
