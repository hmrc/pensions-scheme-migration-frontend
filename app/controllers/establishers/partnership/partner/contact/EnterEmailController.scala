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

package controllers.establishers.partnership.partner.contact

import connectors.cache.UserAnswersCacheConnector
import controllers.EmailAddressController
import controllers.actions._
import forms.EmailFormProvider
import identifiers.beforeYouStart.SchemeNameId
import identifiers.establishers.partnership.partner.PartnerNameId
import identifiers.establishers.partnership.partner.contact.EnterEmailId
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

class EnterEmailController @Inject()(
                                        override val messagesApi: MessagesApi,
                                        val navigator: CompoundNavigator,
                                        authenticate: AuthAction,
                                        getData: DataRetrievalAction,
                                        requireData: DataRequiredAction,
                                        formProvider: EmailFormProvider,
                                        val userAnswersCacheConnector: UserAnswersCacheConnector,
                                        val controllerComponents: MessagesControllerComponents,
                                        val renderer: Renderer
                                       )(implicit val executionContext: ExecutionContext)
  extends EmailAddressController {

  private def name(establisherIndex: Index, partnerIndex: Index)
                  (implicit request: DataRequest[AnyContent]): String =
    request
      .userAnswers
      .get(PartnerNameId(establisherIndex, partnerIndex))
      .fold("the partner")(_.fullName)

  private def form(establisherIndex: Index, partnerIndex: Index)
                  (implicit request: DataRequest[AnyContent]): Form[String] =
    formProvider(Message("messages__enterEmail__error_required", name(establisherIndex, partnerIndex)))

  def onPageLoad(establisherIndex: Index, partnerIndex: Index, mode: Mode): Action[AnyContent] =
    (authenticate andThen getData andThen requireData()).async {
      implicit request =>
        SchemeNameId.retrieve.map {
          schemeName =>
            get(
              entityName = name(establisherIndex, partnerIndex),
              entityType = Messages("messages__partner"),
              id = EnterEmailId(establisherIndex, partnerIndex),
              form = form(establisherIndex, partnerIndex),
              schemeName = schemeName,
              paragraphText = Seq(Messages("messages__contact_details__hint", name(establisherIndex, partnerIndex)))
            )
        }
    }

  def onSubmit(establisherIndex: Index, partnerIndex: Index, mode: Mode): Action[AnyContent] =
    (authenticate andThen getData andThen requireData()).async {
      implicit request =>
        SchemeNameId.retrieve.map {
          schemeName =>
            post(
              entityName = name(establisherIndex, partnerIndex),
              entityType = Messages("messages__partner"),
              id = EnterEmailId(establisherIndex, partnerIndex),
              form = form(establisherIndex, partnerIndex),
              schemeName = schemeName,
              paragraphText = Seq(Messages("messages__contact_details__hint", name(establisherIndex, partnerIndex))),
              mode = mode
            )
        }
    }
}
