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
import forms.ReasonFormProvider
import identifiers.beforeYouStart.SchemeNameId
import identifiers.establishers.partnership.partner.PartnerNameId
import identifiers.establishers.partnership.partner.details.PartnerNoUTRReasonId
import models.requests.DataRequest
import models.{Index, Mode}
import play.api.data.Form
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.mvc.{Action, AnyContent}
import services.common.details.CommonReasonService

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class PartnerNoUTRReasonController @Inject()(val messagesApi: MessagesApi,
                                             authenticate: AuthAction,
                                             getData: DataRetrievalAction,
                                             requireData: DataRequiredAction,
                                             formProvider: ReasonFormProvider,
                                             common: CommonReasonService
                                            )(implicit val executionContext: ExecutionContext)
  extends Retrievals with I18nSupport {

  private def name(establisherIndex: Index, partnerIndex: Index)
                  (implicit request: DataRequest[AnyContent]): String =
    request
      .userAnswers
      .get(PartnerNameId(establisherIndex,partnerIndex))
      .fold("the partner")(_.fullName)

  private def form(establisherIndex: Index, partnerIndex: Index)
                  (implicit request: DataRequest[AnyContent]): Form[String] =
    formProvider(Messages("messages__reason__error_utrRequired", name(establisherIndex,partnerIndex)))

  def onPageLoad(establisherIndex: Index, partnerIndex: Index, mode: Mode): Action[AnyContent] =
    (authenticate andThen getData andThen requireData()).async {
      implicit request =>
        SchemeNameId.retrieve.map {
          schemeName =>
            common.get(
              pageTitle     = Messages("messages__whyNoUTR", Messages("messages__partner")),
              pageHeading     = Messages("messages__whyNoUTR",  name(establisherIndex,partnerIndex)),
              isPageHeading = true,
              id            = PartnerNoUTRReasonId(establisherIndex,partnerIndex),
              form          = form(establisherIndex,partnerIndex),
              schemeName    = schemeName,
              submitUrl     = routes.PartnerNoUTRReasonController.onSubmit(establisherIndex, partnerIndex, mode)
            )
        }
    }

  def onSubmit(establisherIndex: Index, partnerIndex: Index,mode: Mode): Action[AnyContent] =
    (authenticate andThen getData andThen requireData()).async {
      implicit request =>
        SchemeNameId.retrieve.map {
          schemeName =>
            common.post(
              pageTitle     = Messages("messages__whyNoUTR", Messages("messages__partner")),
              pageHeading     = Messages("messages__whyNoUTR",name(establisherIndex,partnerIndex)),
              isPageHeading = true,
              id            = PartnerNoUTRReasonId(establisherIndex,partnerIndex),
              form          = form(establisherIndex,partnerIndex),
              schemeName    = schemeName,
              mode          = mode,
              submitUrl     = routes.PartnerNoUTRReasonController.onSubmit(establisherIndex, partnerIndex, mode)
            )
        }
    }
}

