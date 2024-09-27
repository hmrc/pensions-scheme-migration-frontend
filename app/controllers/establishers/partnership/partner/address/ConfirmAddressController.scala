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

package controllers.establishers.partnership.partner.address

import controllers.Retrievals
import controllers.actions._
import forms.address.AddressFormProvider
import identifiers.beforeYouStart.SchemeNameId
import identifiers.establishers.partnership.partner.PartnerNameId
import identifiers.establishers.partnership.partner.address.{AddressId, AddressListId}
import models.{Address, AddressConfiguration, Index, Mode}
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent}
import services.common.address.CommonManualAddressService
import uk.gov.hmrc.nunjucks.NunjucksSupport

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class ConfirmAddressController @Inject()(
   val messagesApi: MessagesApi,
   authenticate: AuthAction,
   getData: DataRetrievalAction,
   requireData: DataRequiredAction,
   formProvider: AddressFormProvider,
   common: CommonManualAddressService
)(implicit ec: ExecutionContext) extends Retrievals with I18nSupport with NunjucksSupport {

  private val pageTitleEntityTypeMessageKey: Option[String] = Some("messages__partner")
  private def form: Form[Address] = formProvider()

  def onPageLoad(establisherIndex: Index, partnerIndex: Index, mode: Mode): Action[AnyContent] =
    (authenticate andThen getData andThen requireData()).async { implicit request =>
      (PartnerNameId(establisherIndex, partnerIndex) and SchemeNameId).retrieve.map { case partnerName ~ schemeName =>
        common.get(
          Some(schemeName),
          partnerName.fullName,
          AddressId(establisherIndex, partnerIndex),
          AddressListId(establisherIndex, partnerIndex),
          AddressConfiguration.PostcodeFirst,
          form,
          pageTitleEntityTypeMessageKey,
          submitUrl = routes.ConfirmAddressController.onSubmit(establisherIndex, partnerIndex, mode)
        )
      }
    }

  def onSubmit(establisherIndex: Index, partnerIndex: Index, mode: Mode): Action[AnyContent] =
    (authenticate andThen getData andThen requireData()).async { implicit request =>
      (PartnerNameId(establisherIndex, partnerIndex) and SchemeNameId).retrieve.map { case partnerName ~ schemeName =>
        common.post(
          Some(schemeName),
          partnerName.fullName,
          AddressId(establisherIndex, partnerIndex),
          AddressConfiguration.PostcodeFirst,
          Some(mode),
          form,
          pageTitleEntityTypeMessageKey,
          submitUrl = routes.ConfirmAddressController.onSubmit(establisherIndex, partnerIndex, mode)
        )
      }
    }
}
