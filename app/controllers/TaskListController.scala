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

import connectors.LegacySchemeDetailsConnector
import connectors.cache.UserAnswersCacheConnector
import controllers.actions.{AuthAction, DataRetrievalAction}
import identifiers.trustees.AnyTrusteesId
import models.Scheme
import models.requests.OptionalDataRequest
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import renderer.Renderer
import services.TaskListService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.UserAnswers

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class TaskListController @Inject()(
                                        override val messagesApi: MessagesApi,
                                        authenticate: AuthAction,
                                        getData: DataRetrievalAction,
                                        taskListService: TaskListService,
                                        userAnswersCacheConnector: UserAnswersCacheConnector,
                                        legacySchemeDetailsConnector : LegacySchemeDetailsConnector,
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
          Future.successful(Redirect(controllers.preMigration.routes.ListOfSchemesController.onPageLoad(Scheme)))

        case (Some(ua), _) =>
          implicit val userAnswers: UserAnswers = ua
          if(userAnswers.allTrusteesAfterDelete.nonEmpty) {
            val updatedUa = userAnswers.setOrException(AnyTrusteesId, true)
            renderView(updatedUa, implicitly)
          }
          else
            renderView

        case (None, Some(lock)) =>
          legacySchemeDetailsConnector.getLegacySchemeDetails(lock.psaId, lock.pstr).flatMap {
            case Right(data) =>
              val userAnswers: UserAnswers = UserAnswers(data.as[JsObject])
              val updatedUa = userAnswers.setOrException(AnyTrusteesId, userAnswers.allTrusteesAfterDelete.nonEmpty)
              userAnswersCacheConnector.save(lock, updatedUa.data).flatMap(_ => renderView(updatedUa, implicitly))
            case _ => throw new RuntimeException("index page unavailable")
          }
      }
  }

  private def renderView(implicit userAnswers: UserAnswers, request: OptionalDataRequest[_]): Future[Result] = {
    val json = Json.obj(
        "schemeStatus" -> taskListService.schemeCompletionStatus,
        "schemeStatusDescription" -> taskListService.schemeCompletionDescription,
        "expiryDate" -> taskListService.getExpireAt,
        "taskSections" -> taskListService.taskSections,
        "schemeName" -> taskListService.getSchemeName,
        "declarationEnabled" -> taskListService.declarationEnabled,
        "declaration" -> taskListService.declarationSection,
        "returnUrl" -> controllers.routes.PensionSchemeRedirectController.onPageLoad.url
    )
    renderer.render("taskList.njk", json).map(Ok(_))
  }

}
