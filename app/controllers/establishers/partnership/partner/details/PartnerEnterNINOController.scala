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

package controllers.establishers.partnership.partner.details

import controllers.Retrievals
import controllers.actions._
import forms.NINOFormProvider
import identifiers.beforeYouStart.SchemeNameId
import identifiers.establishers.partnership.partner.PartnerNameId
import identifiers.establishers.partnership.partner.details.PartnerNINOId
import models.requests.DataRequest
import models.{Index, Mode, ReferenceValue}
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent}
import services.common.details.CommonEnterReferenceValueService
import viewmodels.Message

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class PartnerEnterNINOController @Inject()(val messagesApi: MessagesApi,
                                           authenticate: AuthAction,
                                           getData: DataRetrievalAction,
                                           requireData: DataRequiredAction,
                                           formProvider: NINOFormProvider,
                                           common: CommonEnterReferenceValueService
                                          )(implicit val executionContext: ExecutionContext)
  extends Retrievals with I18nSupport {

  private def name(establisherIndex: Index, partnerIndex: Index)
                  (implicit request: DataRequest[AnyContent]): String =
    request
      .userAnswers
      .get(PartnerNameId(establisherIndex, partnerIndex))
      .fold(Message("messages__partner"))(_.fullName)

  private def form(establisherIndex: Index, partnerIndex: Index)
                  (implicit request: DataRequest[AnyContent]): Form[ReferenceValue] =
    formProvider(name(establisherIndex, partnerIndex))

  def onPageLoad(establisherIndex: Index, partnerIndex: Index, mode: Mode): Action[AnyContent] =
    (authenticate andThen getData andThen requireData()).async {
      implicit request =>
        SchemeNameId.retrieve.map {
          schemeName =>
            common.get(
              pageTitle     = Message("messages__enterNINO_title", Message("messages__partner")),
              pageHeading     = Message("messages__enterNINO_title", name(establisherIndex, partnerIndex)),
              isPageHeading = true,
              id            = PartnerNINOId(establisherIndex, partnerIndex),
              form          = form(establisherIndex, partnerIndex),
              schemeName    = schemeName,
              hintText      = Some(Message("messages__enterNINO__hint")),
              legendClass   = "govuk-label--xl",
              submitCall = routes.PartnerEnterNINOController.onSubmit(establisherIndex, partnerIndex, mode)
            )
        }
    }

  def onSubmit(establisherIndex: Index, partnerIndex: Index, mode: Mode): Action[AnyContent] =
    (authenticate andThen getData andThen requireData()).async {
      implicit request =>
        SchemeNameId.retrieve.map {
          schemeName =>
            common.post(
              pageTitle     = Message("messages__enterNINO_title", Message("messages__partner")),
              pageHeading     = Message("messages__enterNINO_title", name(establisherIndex, partnerIndex)),
              isPageHeading = true,
              id            = PartnerNINOId(establisherIndex, partnerIndex),
              form          = form(establisherIndex, partnerIndex),
              schemeName    = schemeName,
              hintText      = Some(Message("messages__enterNINO__hint")),
              legendClass   = "govuk-label--xl",
              mode          = mode,
              submitCall = routes.PartnerEnterNINOController.onSubmit(establisherIndex, partnerIndex, mode)
            )
        }
    }
}
