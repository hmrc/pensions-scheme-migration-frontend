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

package controllers.establishers.partnership.partner.details

import connectors.cache.UserAnswersCacheConnector
import controllers.HasReferenceValueController
import controllers.actions._
import forms.HasReferenceNumberFormProvider
import identifiers.beforeYouStart.SchemeNameId
import identifiers.establishers.partnership.partner.PartnerNameId
import identifiers.establishers.partnership.partner.details.PartnerHasNINOId
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

class PartnerHasNINOController @Inject()(
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

  private def name(establisherIndex: Index, partnerIndex: Index)
                  (implicit request: DataRequest[AnyContent]): String =
    request
      .userAnswers
      .get(PartnerNameId(establisherIndex, partnerIndex))
      .fold(Message("messages__partner"))(_.fullName)

  private def form(establisherIndex: Index, partnerIndex: Index)
                  (implicit request: DataRequest[AnyContent]): Form[Boolean] = {
    formProvider(
      errorMsg = Message("messages__genericHasNino__error__required", name(establisherIndex, partnerIndex))
    )
  }

  def onPageLoad(establisherIndex: Index, partnerIndex: Index, mode: Mode): Action[AnyContent] =
    (authenticate andThen getData andThen requireData).async {
      implicit request =>

        SchemeNameId.retrieve.right.map {
          schemeName =>
            get(
              pageTitle     = Message("messages__hasNINO", Message("messages__partner")),
              pageHeading     = Message("messages__hasNINO", name(establisherIndex, partnerIndex)),
              isPageHeading = true,
              id            = PartnerHasNINOId(establisherIndex, partnerIndex),
              form          = form(establisherIndex, partnerIndex),
              schemeName    = schemeName,
              legendClass   = "govuk-label--xl"
            )
        }
    }

  def onSubmit(establisherIndex: Index, partnerIndex: Index, mode: Mode): Action[AnyContent] =
    (authenticate andThen getData andThen requireData).async {
      implicit request =>
        SchemeNameId.retrieve.right.map {
          schemeName =>
            post(
              pageTitle     = Message("messages__hasNINO", Message("messages__partner")),
              pageHeading     = Message("messages__hasNINO", name(establisherIndex, partnerIndex)),
              isPageHeading = true,
              id            = PartnerHasNINOId(establisherIndex, partnerIndex),
              form          = form(establisherIndex, partnerIndex),
              schemeName    = schemeName,
              legendClass   = "govuk-label--xl",
              mode          = mode
            )
        }
    }
}