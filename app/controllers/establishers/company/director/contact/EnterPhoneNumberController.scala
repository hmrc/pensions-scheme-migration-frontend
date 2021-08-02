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

package controllers.establishers.company.director.contact

import connectors.cache.UserAnswersCacheConnector
import controllers.PhoneController
import controllers.actions._
import forms.PhoneFormProvider
import identifiers.beforeYouStart.SchemeNameId
import identifiers.establishers.company.director.DirectorNameId
import identifiers.establishers.company.director.contact.EnterPhoneId
import models.requests.DataRequest
import models.{Index, Mode}
import navigators.CompoundNavigator
import play.api.data.Form
import play.api.i18n.{Messages, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import renderer.Renderer
import viewmodels.Message

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class EnterPhoneNumberController @Inject()(
                                               override val messagesApi: MessagesApi,
                                               val navigator: CompoundNavigator,
                                               authenticate: AuthAction,
                                               getData: DataRetrievalAction,
                                               requireData: DataRequiredAction,
                                               formProvider: PhoneFormProvider,
                                               val controllerComponents: MessagesControllerComponents,
                                               val userAnswersCacheConnector: UserAnswersCacheConnector,
                                               val renderer: Renderer
                                             )(implicit val executionContext: ExecutionContext)
  extends PhoneController {

  private def name(establisherIndex: Index, directorIndex: Index)
                  (implicit request: DataRequest[AnyContent]): String =
    request
      .userAnswers
      .get(DirectorNameId(establisherIndex, directorIndex))
      .fold("the director")(_.fullName)

  private def form(establisherIndex: Index, directorIndex: Index)
                  (implicit request: DataRequest[AnyContent]): Form[String] =
    formProvider(Message("messages__enterPhone__error_required", name(establisherIndex, directorIndex)))

  def onPageLoad(establisherIndex: Index, directorIndex: Index,mode: Mode): Action[AnyContent] =
    (authenticate andThen getData andThen requireData).async {
      implicit request =>
        SchemeNameId.retrieve.right.map {
          schemeName =>
            get(
              entityName = name(establisherIndex, directorIndex),
              entityType = Messages("messages__director"),
              id = EnterPhoneId(establisherIndex, directorIndex),
              form = form(establisherIndex, directorIndex),
              schemeName = schemeName,
              paragraphText = Seq(Messages("messages__contact_details__hint", name(establisherIndex, directorIndex)))
            )

        }
    }

  def onSubmit( establisherIndex: Index, directorIndex: Index, mode: Mode): Action[AnyContent] =
    (authenticate andThen getData andThen requireData).async {
      implicit request =>
        SchemeNameId.retrieve.right.map {
          schemeName =>
            post(
              entityName = name(establisherIndex, directorIndex),
              entityType = Messages("messages__director"),
              id = EnterPhoneId(establisherIndex, directorIndex),
              form = form(establisherIndex, directorIndex),
              schemeName = schemeName,
              paragraphText = Seq(Messages("messages__contact_details__hint", name(establisherIndex, directorIndex))),
              mode = mode
            )
          }
    }

}
