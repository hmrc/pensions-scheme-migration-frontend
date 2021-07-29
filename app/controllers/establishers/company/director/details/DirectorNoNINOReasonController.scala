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

package controllers.establishers.company.director.details

import connectors.cache.UserAnswersCacheConnector
import controllers.ReasonController
import controllers.actions._
import forms.ReasonFormProvider
import identifiers.beforeYouStart.SchemeNameId
import identifiers.establishers.company.director.{DirectorNameId, DirectorNoNINOReasonId}
import models.requests.DataRequest
import models.{Index, Mode}
import navigators.CompoundNavigator
import play.api.data.Form
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import renderer.Renderer
import viewmodels.Message

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class DirectorNoNINOReasonController @Inject()(
                                                override val messagesApi: MessagesApi,
                                                val navigator: CompoundNavigator,
                                                authenticate: AuthAction,
                                                getData: DataRetrievalAction,
                                                requireData: DataRequiredAction,
                                                formProvider: ReasonFormProvider,
                                                val controllerComponents: MessagesControllerComponents,
                                                val userAnswersCacheConnector: UserAnswersCacheConnector,
                                                val renderer: Renderer
                                              )(implicit val executionContext: ExecutionContext)
  extends ReasonController {

  private def name(establisherIndex: Index, directorIndex: Index)
                  (implicit request: DataRequest[AnyContent]): String =
    request
      .userAnswers
      .get(DirectorNameId(establisherIndex, directorIndex))
      .fold(Message("messages__director"))(_.fullName)

  private def form(establisherIndex: Index, directorIndex: Index)
                  (implicit request: DataRequest[AnyContent]): Form[String] =
    formProvider(Message("messages__reason__error_ninoRequired", name(establisherIndex, directorIndex)))

  def onPageLoad(establisherIndex: Index, directorIndex: Index, mode: Mode): Action[AnyContent] =
    (authenticate andThen getData andThen requireData).async {
      implicit request =>
        SchemeNameId.retrieve.right.map {
          schemeName =>
            get(
              pageTitle     = Message("messages__whyNoNINO", Message("messages__director")),
              pageHeading     = Message("messages__whyNoNINO", name(establisherIndex, directorIndex)),
              isPageHeading = true,
              id            = DirectorNoNINOReasonId(establisherIndex, directorIndex),
              form          = form(establisherIndex, directorIndex),
              schemeName    = schemeName
            )
        }
    }

  def onSubmit(establisherIndex: Index, directorIndex: Index, mode: Mode): Action[AnyContent] =
    (authenticate andThen getData andThen requireData).async {
      implicit request =>
        SchemeNameId.retrieve.right.map {
          schemeName =>
            post(
              pageTitle     = Message("messages__whyNoNINO", Message("messages__director")),
              pageHeading     = Message("messages__whyNoNINO", name(establisherIndex, directorIndex)),
              isPageHeading = true,
              id            = DirectorNoNINOReasonId(establisherIndex, directorIndex),
              form          = form(establisherIndex, directorIndex),
              schemeName    = schemeName,
              mode          = mode
            )
        }
    }
}