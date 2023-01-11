/*
 * Copyright 2023 HM Revenue & Customs
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
import controllers.PhoneController
import controllers.actions.{AuthAction, DataRequiredAction, DataRetrievalAction}
import forms.PhoneFormProvider
import helpers.cya.MandatoryAnswerMissingException
import identifiers.adviser.{AdviserNameId, EnterPhoneId}
import identifiers.beforeYouStart.SchemeNameId
import models.Mode
import models.requests.DataRequest
import navigators.CompoundNavigator
import play.api.data.Form
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import renderer.Renderer
import viewmodels.Message

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class EnterPhoneController @Inject()(
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

  private def name
  (implicit request: DataRequest[AnyContent]): String = {
    request.userAnswers.get(AdviserNameId).getOrElse(throw MandatoryAnswerMissingException(AdviserNameId.toString))
  }

  private def form(implicit request: DataRequest[AnyContent]): Form[String] =
    formProvider(Message("messages__error__common__phone__required"),Some(Message("messages__phone__invalid")))

  def onPageLoad(mode: Mode): Action[AnyContent] =
    (authenticate andThen getData andThen requireData()).async {
      implicit request =>
        SchemeNameId.retrieve.map {
          schemeName =>
            get(
              entityName = name,
              entityType = Message("messages__pension__adviser"),
              id = EnterPhoneId,
              form = form,
              schemeName = schemeName,
              paragraphText = Seq(Message("messages__contact_details__phone__hint", name, schemeName))
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
              id = EnterPhoneId,
              form = form,
              schemeName = schemeName,
              paragraphText = Seq(Message("messages__contact_details__phone__hint", name, schemeName)),
              mode = mode
            )
        }
    }
}
