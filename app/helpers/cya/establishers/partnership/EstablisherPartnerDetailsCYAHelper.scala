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

package helpers.cya.establishers.partnership

import controllers.establishers.partnership.partner.address.{routes => addressRoutes}
import controllers.establishers.partnership.partner.contact.{routes => contactRoutes}
import controllers.establishers.partnership.partner.details.{routes => detailsRoutes}
import controllers.establishers.partnership.partner.routes
import helpers.cya.CYAHelper
import helpers.cya.CYAHelper.getName
import identifiers.establishers.partnership.partner._
import identifiers.establishers.partnership.partner.address.{AddressId, AddressYearsId, PreviousAddressId}
import identifiers.establishers.partnership.partner.contact.{EnterEmailId, EnterPhoneId}
import identifiers.establishers.partnership.partner.details._
import models.requests.DataRequest
import models.{CheckMode, Index}
import play.api.i18n.Messages
import play.api.mvc.AnyContent
import uk.gov.hmrc.govukfrontend.views.Aliases.Text
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import utils.{Enumerable, UserAnswers}

class EstablisherPartnerDetailsCYAHelper
  extends CYAHelper
    with Enumerable.Implicits {
  //scalastyle:off method.length
  def detailsRows(
                   establisherIndex: Index, partnerIndex: Index
                 )(
                   implicit request: DataRequest[AnyContent],
                   messages: Messages
                 ): Seq[SummaryListRow] = {
    implicit val ua: UserAnswers =
      request.userAnswers
    val partnerName: String =
      getName(PartnerNameId(establisherIndex,partnerIndex))

    val rowsWithoutDynamicIndices = Seq(
      Some(answerOrAddRow(
        id                 = PartnerNameId(establisherIndex, partnerIndex),
        message            = Messages("messages__partner__name"),
        url                = Some(routes.PartnerNameController.onPageLoad(establisherIndex,partnerIndex, CheckMode).url),
        visuallyHiddenText = Some(Text(Messages("messages__partner__name__cya__visuallyHidden", partnerName))),
        answerTransform    = answerPersonNameTransform
      )),
      Some(answerOrAddRow(
        id                 =PartnerDOBId(establisherIndex, partnerIndex),
        message            = Messages("messages__dob__h1", partnerName),
        url                = Some(detailsRoutes.PartnerDOBController.onPageLoad(establisherIndex,partnerIndex,CheckMode).url),
        visuallyHiddenText = Some(Text(Messages("messages__dob__cya__visuallyHidden", partnerName))),
        answerTransform = answerDateTransform
      )),
      Some(answerOrAddRow(
        id                 = PartnerHasNINOId(establisherIndex, partnerIndex),
        message            = Messages("messages__hasNINO", partnerName),
        url                = Some(detailsRoutes.PartnerHasNINOController.onPageLoad(establisherIndex, partnerIndex, CheckMode).url),
        visuallyHiddenText = Some(Text(Messages("messages__hasNINO__cya__visuallyHidden", partnerName))),
        answerTransform    = answerBooleanTransform
      )),
      ua.get(PartnerHasNINOId(establisherIndex, partnerIndex)) map {
        case true =>
          answerOrAddRow(
            id                 = PartnerNINOId(establisherIndex, partnerIndex),
            message            = Messages("messages__enterNINO__cya", partnerName),
            url                = Some(detailsRoutes.PartnerEnterNINOController.onPageLoad(establisherIndex, partnerIndex, CheckMode).url),
            visuallyHiddenText = Some(Text(Messages("messages__hasNINO__cya__visuallyHidden", partnerName))),
            answerTransform    = referenceValueTransform
          )
        case false =>
          answerOrAddRow(
            id                 = PartnerNoNINOReasonId(establisherIndex, partnerIndex),
            message            = Messages("messages__whyNoNINO", partnerName),
            url                = Some(detailsRoutes.PartnerNoNINOReasonController.onPageLoad(establisherIndex, partnerIndex, CheckMode).url),
            visuallyHiddenText = Some(Text(Messages("messages__whyNoNINO__cya__visuallyHidden", partnerName)))
          )
      },
      Some(answerOrAddRow(
        id                 = PartnerHasUTRId(establisherIndex, partnerIndex),
        message            = Messages("messages__hasUTR", partnerName),
        url                = Some(detailsRoutes.PartnerHasUTRController.onPageLoad(establisherIndex, partnerIndex,CheckMode).url),
        visuallyHiddenText = Some(Text(Messages("messages__hasUTR__cya__visuallyHidden", partnerName))),
        answerTransform    = answerBooleanTransform
      )),
      ua.get(PartnerHasUTRId(establisherIndex, partnerIndex)) map {
        case true =>
          answerOrAddRow(
            id                 = PartnerEnterUTRId(establisherIndex, partnerIndex),
            message            = Messages("messages__enterUTR__cya_label", partnerName),
            url                = Some(detailsRoutes.PartnerEnterUTRController.onPageLoad(establisherIndex, partnerIndex,CheckMode).url),
            visuallyHiddenText = Some(Text(Messages("messages__hasUTR__cya__visuallyHidden", partnerName))),
            answerTransform    = referenceValueTransform
          )
        case false =>
          answerOrAddRow(
            id                 = PartnerNoUTRReasonId(establisherIndex, partnerIndex),
            message            = Messages("messages__whyNoUTR", partnerName),
            url                = Some(detailsRoutes.PartnerNoUTRReasonController.onPageLoad(establisherIndex, partnerIndex,CheckMode).url),
            visuallyHiddenText = Some(Text(Messages("messages__whyNoUTR__cya__visuallyHidden", partnerName)))
          )
      },
      Some( answerOrAddRow(
            id                  = AddressId(establisherIndex, partnerIndex),
            message             = Messages("addressList_cya_label", partnerName),
            url                 = Some(addressRoutes.EnterPostcodeController.onPageLoad(establisherIndex, partnerIndex,  CheckMode).url),
            visuallyHiddenText  = Some(Text(Messages("messages__visuallyHidden__address", partnerName))), answerAddressTransform
          ))
     ,
      Some(
        answerOrAddRow(
            id                  = AddressYearsId(establisherIndex, partnerIndex),
            message             = Messages("addressYears.title", partnerName),
            url                 = Some(addressRoutes.AddressYearsController.onPageLoad(establisherIndex, partnerIndex, CheckMode).url),
            visuallyHiddenText  = Some(Text(Messages("messages__visuallyhidden__addressYears", partnerName))), answerBooleanTransform
          ))
      ,
      if (ua.get(AddressYearsId(establisherIndex, partnerIndex)).contains(true)) {
        None
      }else{
        Some( answerOrAddRow(
          id = PreviousAddressId(establisherIndex, partnerIndex),
          message = Messages("previousAddressList_cya_label", partnerName),
          url = Some(addressRoutes.EnterPreviousPostcodeController.onPageLoad(establisherIndex, partnerIndex, CheckMode).url),
          visuallyHiddenText = Some(Text(Messages("messages__visuallyHidden__previousAddress", partnerName))), answerAddressTransform
        ))
      },
      Some(answerOrAddRow(
        id                  = EnterEmailId(establisherIndex, partnerIndex),
        message             = Messages("messages__enterEmail_cya_label", partnerName),
        url                 = Some(contactRoutes.EnterEmailController.onPageLoad(establisherIndex, partnerIndex,CheckMode).url),
        visuallyHiddenText  = Some(Text(Messages("messages__enterEmail__cya__visuallyHidden", partnerName)))
      )),
      Some(answerOrAddRow(
        id                  = EnterPhoneId(establisherIndex, partnerIndex),
        message             = Messages("messages__enterPhone_cya_label", partnerName),
        url                 = Some(contactRoutes.EnterPhoneNumberController.onPageLoad(establisherIndex, partnerIndex, CheckMode).url),
        visuallyHiddenText  = Some(Text(Messages("messages__enterPhone__cya__visuallyHidden", partnerName)))
      ))
    ).flatten
    rowsWithDynamicIndices(rowsWithoutDynamicIndices)
  }
}
