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

package controllers.establishers.company.director

import controllers.Retrievals
import controllers.actions._
import identifiers.establishers.company.director.DirectorNameId
import models.requests.DataRequest
import models.{Index, NormalMode}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.Enumerable
import views.html.AlreadyDeletedView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class AlreadyDeletedController @Inject()(override val messagesApi: MessagesApi,
                                         authenticate: AuthAction,
                                         getData: DataRetrievalAction,
                                         requireData: DataRequiredAction,
                                         val controllerComponents: MessagesControllerComponents,
                                         alreadyDeletedView: AlreadyDeletedView,
                                        )(implicit val executionContext: ExecutionContext) extends
  FrontendBaseController with Retrievals with I18nSupport with Enumerable.Implicits {

  def onPageLoad(establisherIndex: Index, directorIndex: Index): Action[AnyContent] =
    (authenticate andThen getData andThen requireData()).async {
      implicit request =>
        directorName(establisherIndex, directorIndex) match {
          case Right(directorName) =>
            Future.successful(Ok(alreadyDeletedView(
              "messages__alreadyDeleted__director_title",
              directorName,
              existingSchemeName,
              controllers.establishers.company.routes.AddCompanyDirectorsController.onPageLoad(establisherIndex, NormalMode).url
            )))
          case Left(result) => result
        }

    }

  private def directorName(establisherIndex: Index, directorIndex: Index)(implicit
                                                                          dataRequest: DataRequest[AnyContent])
  : Either[Future[Result], String] = {
    DirectorNameId(establisherIndex, directorIndex).retrieve.map(_.fullName)
  }

}
