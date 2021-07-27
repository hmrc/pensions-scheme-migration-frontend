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
import controllers.HasReferenceValueController
import controllers.actions._
import forms.HasReferenceNumberFormProvider
import identifiers.establishers.company.director.{DirectorHasUTRId, DirectorNameId}
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

class DirectorHasUTRController @Inject()(
                                         override val messagesApi: MessagesApi,
                                         val navigator: CompoundNavigator,
                                         authenticate: AuthAction,
                                         getData: DataRetrievalAction,
                                         requireData: DataRequiredAction,
                                         formProvider: HasReferenceNumberFormProvider,
                                         val controllerComponents: MessagesControllerComponents,
                                         val userAnswersCacheConnector: UserAnswersCacheConnector,
                                         val renderer: Renderer
                                        )(implicit val executionContext: ExecutionContext) extends
  HasReferenceValueController {

  private def name(establisherIndex: Index, directorIndex: Index)
                  (implicit request: DataRequest[AnyContent]): String =
    request
      .userAnswers
      .get(DirectorNameId(establisherIndex,directorIndex))
      .fold("the director")(_.fullName)

  private def form(establisherIndex: Index, directorIndex: Index)
                  (implicit request: DataRequest[AnyContent]): Form[Boolean] =
    formProvider(
      errorMsg = Message("messages__genericHasUtr__error__required", name(establisherIndex,directorIndex))
    )

  def onPageLoad( establisherIndex: Index, directorIndex: Index,mode: Mode): Action[AnyContent] =
    (authenticate andThen getData andThen requireData).async {
      implicit request =>
        DirectorNameId(establisherIndex, directorIndex).retrieve.right.map {
          details =>
            get(
              pageTitle     = Message("messages__hasUTR", Message("messages__director")),
              pageHeading     = Message("messages__hasUTR", name(establisherIndex,directorIndex)),
              isPageHeading = false,
              id            = DirectorHasUTRId(establisherIndex, directorIndex),
              form          = form(establisherIndex,directorIndex),
              schemeName    = existingSchemeName.toString,
              paragraphText = Seq(Message("messages__UTR__p")),
              legendClass   = "govuk-visually-hidden"
            )
        }
    }

  def onSubmit(establisherIndex: Index, directorIndex: Index,mode: Mode): Action[AnyContent] =
    (authenticate andThen getData andThen requireData).async {
      implicit request =>
        DirectorNameId(establisherIndex, directorIndex).retrieve.right.map {
          details =>
            post(
              pageTitle     = Message("messages__hasUTR", Message("messages__director")),
              pageHeading     = Message("messages__hasUTR", name(establisherIndex,directorIndex)),
              isPageHeading = false,
              id            = DirectorHasUTRId(establisherIndex, directorIndex),
              form          = form(establisherIndex,directorIndex),
              schemeName    = existingSchemeName.toString,
              paragraphText = Seq(Message("messages__UTR__p")),
              legendClass   = "govuk-visually-hidden",
              mode          = mode
            )
        }
    }
}
