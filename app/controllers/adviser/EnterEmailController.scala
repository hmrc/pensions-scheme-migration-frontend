/*
 * Copyright 2022 HM Revenue & Customs
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

package controllers.adviser

import connectors.cache.UserAnswersCacheConnector
import controllers.EmailAddressController
import controllers.actions.{AuthAction, DataRequiredAction, DataRetrievalAction}
import forms.EmailFormProvider
import helpers.cya.MandatoryAnswerMissingException
import identifiers.adviser.{AdviserNameId, EnterEmailId}
import identifiers.beforeYouStart.SchemeNameId
import models.Mode
import models.requests.DataRequest
import navigators.CompoundNavigator
import play.api.data.Form
import play.api.i18n.{Messages, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import renderer.Renderer
import viewmodels.Message

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class EnterEmailController @Inject()(
                                      override val messagesApi: MessagesApi,
                                      val navigator: CompoundNavigator,
                                      authenticate: AuthAction,
                                      getData: DataRetrievalAction,
                                      requireData: DataRequiredAction,
                                      formProvider: EmailFormProvider,
                                      val controllerComponents: MessagesControllerComponents,
                                      val userAnswersCacheConnector: UserAnswersCacheConnector,
                                      val renderer: Renderer
                                    )(implicit val executionContext: ExecutionContext)
  extends EmailAddressController {

  private def name
  (implicit request: DataRequest[AnyContent]): String = {
    request.userAnswers.get(AdviserNameId).getOrElse(throw MandatoryAnswerMissingException(AdviserNameId.toString))
  }

  private def form(implicit request: DataRequest[AnyContent]): Form[String] =
    formProvider(Messages("messages__error__common__email__required"))

  def onPageLoad(mode: Mode): Action[AnyContent] =
    (authenticate andThen getData andThen requireData()).async {
      implicit request =>
        SchemeNameId.retrieve.map {
          schemeName =>
            get(
              entityName = name,
              entityType = Message("messages__pension__adviser"),
              id = EnterEmailId,
              form = form,
              schemeName = schemeName,
              paragraphText = Seq(Message("messages__contact_details__email__hint", name, schemeName))
            )
        }
    }

  def onSubmit(mode: Mode): Action[AnyContent] =
    (authenticate andThen getData andThen requireData()).async {
      implicit request =>
        SchemeNameId.retrieve.map {
          schemeName =>
            post(
              entityName = name,
              entityType = Message("messages__pension__adviser"),
              id = EnterEmailId,
              form = form,
              schemeName = schemeName,
              paragraphText = Seq(Message("messages__contact_details__email__hint", name, schemeName)),
              mode = mode
            )
        }
    }
}
