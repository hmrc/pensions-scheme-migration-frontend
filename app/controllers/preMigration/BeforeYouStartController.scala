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

import connectors.cache.UserAnswersCacheConnector
import connectors.{LegacySchemeDetailsConnector, MinimalDetailsConnector}
import controllers.Retrievals
import controllers.actions.{AuthAction, DataRetrievalAction}
import models.Scheme
import models.requests.OptionalDataRequest
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.preMigration.BeforeYouStartView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class BeforeYouStartController @Inject()(
                                           override val messagesApi: MessagesApi,
                                           authenticate: AuthAction,
                                           getData: DataRetrievalAction,
                                           minimalDetailsConnector: MinimalDetailsConnector,
                                           userAnswersCacheConnector: UserAnswersCacheConnector,
                                           legacySchemeDetailsConnector : LegacySchemeDetailsConnector,
                                           val controllerComponents: MessagesControllerComponents,
                                           beforeYouStartView: BeforeYouStartView
                                         )(implicit val ec: ExecutionContext)
  extends FrontendBaseController
    with I18nSupport
    with Retrievals
    {

  def onPageLoad: Action[AnyContent] =
    (authenticate andThen getData).async {
      implicit request =>
        (request.userAnswers, request.lock) match {
          case (_, None) =>
            Future.successful(Redirect(controllers.preMigration.routes.ListOfSchemesController.onPageLoad(Scheme)))

          case (Some(ua), _) =>
            renderView

          case (None, Some(lock)) =>
            legacySchemeDetailsConnector.getLegacySchemeDetails(lock.psaId, lock.pstr).flatMap {
              case Right(data) =>
                userAnswersCacheConnector.save(lock, data).flatMap { _ =>
                  renderView
                }
              case _ => throw new RuntimeException("index page unavailable")
            }
        }

    }


  private def renderView(implicit request: OptionalDataRequest[?]): Future[Result]= {
    minimalDetailsConnector.getPSAName.flatMap { psaName =>
      Future.successful(Ok(beforeYouStartView(
        psaName,
        controllers.routes.TaskListController.onPageLoad,
        controllers.routes.PensionSchemeRedirectController.onPageLoad.url
      )))
    }
  }
}
