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
import forms.DOBFormProvider
import identifiers.beforeYouStart.SchemeNameId
import identifiers.establishers.partnership.partner.PartnerNameId
import identifiers.establishers.partnership.partner.details.PartnerDOBId
import models.{Index, Mode}
import play.api.data.Form
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.mvc.{Action, AnyContent}
import services.common.details.CommonDateOfBirthService

import java.time.LocalDate
import javax.inject.Inject
import scala.concurrent.ExecutionContext

class PartnerDOBController @Inject()(val messagesApi: MessagesApi,
                                     authenticate: AuthAction,
                                     getData: DataRetrievalAction,
                                     requireData: DataRequiredAction,
                                     formProvider: DOBFormProvider,
                                     common: CommonDateOfBirthService
                                    )(implicit val executionContext: ExecutionContext)
  extends Retrievals with I18nSupport {

  val form: Form[LocalDate] = formProvider()

  def onPageLoad(establisherIndex: Index, partnerIndex: Index, mode: Mode): Action[AnyContent] =
    (authenticate andThen getData andThen requireData()).async {
      implicit request =>

        SchemeNameId.retrieve.map {
          schemeName =>
            common.get(
              form = form,
              dobId = PartnerDOBId(establisherIndex, partnerIndex),
              personNameId = PartnerNameId(establisherIndex, partnerIndex),
              schemeName   = schemeName,
              entityType   = Messages("messages__partner"),
              call = routes.PartnerDOBController.onSubmit(establisherIndex, partnerIndex, mode)
            )
        }
    }

  def onSubmit(establisherIndex: Index, partnerIndex: Index, mode: Mode): Action[AnyContent] =
    (authenticate andThen getData andThen requireData()).async {
      implicit request =>
        SchemeNameId.retrieve.map {
          schemeName =>
            common.post(
              form         = form,
              dobId        = PartnerDOBId(establisherIndex, partnerIndex),
              personNameId = PartnerNameId(establisherIndex, partnerIndex),
              schemeName   = schemeName,
              entityType   = Messages("messages__partner"),
              mode         = mode,
              call = routes.PartnerDOBController.onSubmit(establisherIndex, partnerIndex, mode)
            )
        }
    }
}
