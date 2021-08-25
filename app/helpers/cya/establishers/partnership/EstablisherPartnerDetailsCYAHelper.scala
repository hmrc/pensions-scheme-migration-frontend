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

package helpers.cya.establishers.partnership

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
import uk.gov.hmrc.viewmodels.MessageInterpolators
import uk.gov.hmrc.viewmodels.SummaryList.Row
import utils.{Enumerable, UserAnswers}
import viewmodels.Message

class EstablisherPartnerDetailsCYAHelper
  extends CYAHelper
    with Enumerable.Implicits {

  //scalastyle:off method.length
  def detailsRows(
                   establisherIndex: Index, partnerIndex: Index
                 )(
                   implicit request: DataRequest[AnyContent],
                   messages: Messages
                 ): Seq[Row] = {
    implicit val ua: UserAnswers =
      request.userAnswers
    val partnerName: String =
      getName(PartnerNameId(establisherIndex,partnerIndex))

    val rowsWithoutDynamicIndices = Seq(
      Some(answerOrAddRow(
        id                 = PartnerNameId(establisherIndex, partnerIndex),
        message            = Message("messages__partner__name").resolve,
        url                = Some(routes.PartnerNameController.onPageLoad(establisherIndex,partnerIndex, CheckMode).url),
        visuallyHiddenText = Some(msg"messages__partner__name__cya__visuallyHidden".withArgs(partnerName)),
        answerTransform    = answerPersonNameTransform
      )),

      Some(answerOrAddRow(
        id                 =PartnerDOBId(establisherIndex, partnerIndex),
        message            = Message("messages__dob__h1", partnerName).resolve,
        url                = Some(controllers.establishers.partnership.partner.details.routes.PartnerDOBController.onPageLoad(establisherIndex,partnerIndex,CheckMode).url),
        visuallyHiddenText = Some(msg"messages__dob__cya__visuallyHidden".withArgs(partnerName)),
        answerTransform = answerDateTransform
      )),
      Some(answerOrAddRow(
        id                 = PartnerHasNINOId(establisherIndex, partnerIndex),
        message            = Message("messages__hasNINO", partnerName).resolve,
        url                = Some(controllers.establishers.partnership.partner.details.routes.PartnerHasNINOController.onPageLoad(establisherIndex, partnerIndex, CheckMode).url),
        visuallyHiddenText = Some(msg"messages__hasUTR__cya__visuallyHidden".withArgs(partnerName)),
        answerTransform    = answerBooleanTransform
      )),
      ua.get(PartnerNINOId(establisherIndex, partnerIndex)) map {
        _ =>
          answerOrAddRow(
            id                 = PartnerNINOId(establisherIndex, partnerIndex),
            message            = Message("messages__hasNINO__cya", partnerName),
            url                = Some(controllers.establishers.partnership.partner.details.routes.PartnerEnterNINOController.onPageLoad(establisherIndex, partnerIndex, CheckMode).url),
            visuallyHiddenText = Some(msg"messages__hasNINO__cya__visuallyHidden".withArgs(partnerName)),
            answerTransform    = referenceValueTransform
          )
      },
      ua.get(PartnerNoNINOReasonId(establisherIndex, partnerIndex)) map {
        _ =>
          answerOrAddRow(
            id                 = PartnerNoNINOReasonId(establisherIndex, partnerIndex),
            message            = Message("messages__whyNoNINO", partnerName),
            url                = Some(controllers.establishers.partnership.partner.details.routes.PartnerNoNINOReasonController.onPageLoad(establisherIndex, partnerIndex, CheckMode).url),
            visuallyHiddenText = Some(msg"messages__whyNoNINO__cya__visuallyHidden".withArgs(partnerName))
          )
      },
      Some(answerOrAddRow(
        id                 = PartnerHasUTRId(establisherIndex, partnerIndex),
        message            = Message("messages__hasUTR", partnerName).resolve,
        url                = Some(controllers.establishers.partnership.partner.details.routes.PartnerHasUTRController.onPageLoad(establisherIndex, partnerIndex,CheckMode).url),
        visuallyHiddenText = Some(msg"messages__hasUTR__cya__visuallyHidden".withArgs(partnerName)),
        answerTransform    = answerBooleanTransform
      )),
      ua.get(PartnerEnterUTRId(establisherIndex, partnerIndex)) map {
        _ =>
          answerOrAddRow(
            id                 = PartnerEnterUTRId(establisherIndex, partnerIndex),
            message            = Message("messages__hasUTR__cya", partnerName),
            url                = Some(controllers.establishers.partnership.partner.details.routes.PartnerEnterUTRController.onPageLoad(establisherIndex, partnerIndex,CheckMode).url),
            visuallyHiddenText = Some(msg"messages__hasUTR__cya__visuallyHidden".withArgs(partnerName)),
            answerTransform    = referenceValueTransform
          )
      },
      ua.get(PartnerNoUTRReasonId(establisherIndex, partnerIndex)) map {
        _ =>
          answerOrAddRow(
            id                 = PartnerNoUTRReasonId(establisherIndex, partnerIndex),
            message            = Message("messages__whyNoUTR", partnerName),
            url                = Some(controllers.establishers.partnership.partner.details.routes.PartnerNoUTRReasonController.onPageLoad(establisherIndex, partnerIndex,CheckMode).url),
            visuallyHiddenText = Some(msg"messages__whyNoUTR__cya__visuallyHidden".withArgs(partnerName))
          )
      },
      Some( answerOrAddRow(
            id                  = AddressId(establisherIndex, partnerIndex),
            message             = Message("messages__establisherAddress__whatYouWillNeed_h1", partnerName).resolve,
            url                 = Some(controllers.establishers.partnership.partner.address.routes.EnterPostcodeController.onPageLoad(establisherIndex, partnerIndex,  CheckMode).url),
            visuallyHiddenText  = Some(msg"messages__visuallyHidden__address".withArgs(partnerName)), answerAddressTransform
          ))
     ,
      Some(
        answerOrAddRow(
            id                  = AddressYearsId(establisherIndex, partnerIndex),
            message             = Message("addressYears.title", partnerName).resolve,
            url                 = Some(controllers.establishers.partnership.partner.address.routes.AddressYearsController.onPageLoad(establisherIndex, partnerIndex, CheckMode).url),
            visuallyHiddenText  = Some(msg"messages__visuallyhidden__addressYears".withArgs(partnerName)), answerBooleanTransform
          ))
      ,
      if (ua.get(AddressYearsId(establisherIndex, partnerIndex)).contains(true)) {
        None
      }else{
        Some( answerOrAddRow(
          id = PreviousAddressId(establisherIndex, partnerIndex),
          message = Message("messages__establisherPreviousAddress").resolve,
          url = Some(controllers.establishers.partnership.partner.address.routes.EnterPreviousPostcodeController.onPageLoad(establisherIndex, partnerIndex, CheckMode).url),
          visuallyHiddenText = Some(msg"messages__visuallyHidden__previousAddress".withArgs(partnerName)), answerAddressTransform
        ))
      },
      Some(answerOrAddRow(
        id                  = EnterEmailId(establisherIndex, partnerIndex),
        message             = Message("messages__enterEmail", partnerName).resolve,
        url                 = Some(controllers.establishers.partnership.partner.contact.routes.EnterEmailController.onPageLoad(establisherIndex, partnerIndex,CheckMode).url),
        visuallyHiddenText  = Some(msg"messages__enterEmail__cya__visuallyHidden".withArgs(partnerName))
      )),
      Some(answerOrAddRow(
        id                  = EnterPhoneId(establisherIndex, partnerIndex),
        message             = Message("messages__enterPhone", partnerName).resolve,
        url                 = Some(controllers.establishers.partnership.partner.contact.routes.EnterPhoneNumberController.onPageLoad(establisherIndex, partnerIndex, CheckMode).url),
        visuallyHiddenText  = Some(msg"messages__enterPhone__cya__visuallyHidden".withArgs(partnerName))
      ))
    ).flatten
    rowsWithDynamicIndices(rowsWithoutDynamicIndices)
  }
}
