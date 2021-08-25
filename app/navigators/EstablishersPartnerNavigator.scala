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

package navigators

import controllers.establishers.partnership.partner.address.routes._
import controllers.establishers.partnership.partner.contact.routes._
import controllers.establishers.partnership.partner.details.routes._
import identifiers._
import identifiers.establishers.partnership.partner._
import identifiers.establishers.partnership.partner.address._
import identifiers.establishers.partnership.partner.contact.{EnterEmailId, EnterPhoneId}
import identifiers.establishers.partnership.partner.details._
import models.requests.DataRequest
import models.{CheckMode, Index, Mode, NormalMode}
import play.api.mvc.{AnyContent, Call}
import utils.{Enumerable, UserAnswers}

class EstablishersPartnerNavigator
  extends Navigator
    with Enumerable.Implicits {

  //scalastyle:off cyclomatic.complexity
  override protected def routeMap(ua: UserAnswers)
                                 (implicit request: DataRequest[AnyContent]): PartialFunction[Identifier, Call] = {
    case PartnerNameId(establisherIndex, partnerIndex) => PartnerDOBController.onPageLoad(establisherIndex,partnerIndex, NormalMode)
    case PartnerDOBId(establisherIndex, partnerIndex) => PartnerHasNINOController.onPageLoad(establisherIndex,partnerIndex, NormalMode)
    case PartnerHasNINOId(establisherIndex, partnerIndex) => partnerHasNino(establisherIndex, partnerIndex, ua, NormalMode)
    case PartnerNINOId(establisherIndex, partnerIndex) => hassUTR(establisherIndex, partnerIndex,NormalMode)
    case PartnerNoNINOReasonId(establisherIndex, partnerIndex) => hassUTR(establisherIndex, partnerIndex,NormalMode)
    case PartnerHasUTRId(establisherIndex, partnerIndex) => establisherHasUtr(establisherIndex, partnerIndex, ua, NormalMode)
    case PartnerEnterUTRId(establisherIndex, partnerIndex) => postcode(establisherIndex, partnerIndex, NormalMode)
    case PartnerNoUTRReasonId(establisherIndex, partnerIndex) => postcode(establisherIndex, partnerIndex, NormalMode)
    case AddressId(establisherIndex, partnerIndex) => addressYears(establisherIndex, partnerIndex, NormalMode)
    case AddressListId(establisherIndex, partnerIndex) => addressYears(establisherIndex, partnerIndex, NormalMode)
    case AddressYearsId(establisherIndex, partnerIndex) =>
      if (ua.get(AddressYearsId(establisherIndex, partnerIndex)).contains(true)) email(establisherIndex, partnerIndex, NormalMode)
      else prevPostcode(establisherIndex, partnerIndex, NormalMode)
    case EnterPostCodeId(establisherIndex, partnerIndex) => selectAddress(establisherIndex, partnerIndex, NormalMode)
    case EnterPreviousPostCodeId(establisherIndex, partnerIndex) => selectPrevAddress(establisherIndex, partnerIndex, NormalMode)
    case PreviousAddressId(establisherIndex, partnerIndex) => email(establisherIndex, partnerIndex, NormalMode)
    case PreviousAddressListId(establisherIndex, partnerIndex) => email(establisherIndex, partnerIndex, NormalMode)
    case EnterEmailId(establisherIndex, partnerIndex) => EnterPhoneNumberController.onPageLoad(establisherIndex, partnerIndex, NormalMode)
    case EnterPhoneId(establisherIndex, partnerIndex) =>cyaDetails(establisherIndex,partnerIndex)
    case ConfirmDeletePartnerId(establisherIndex) => controllers.establishers.partnership.routes.AddPartnersController.onPageLoad(establisherIndex,NormalMode)
  }

  override protected def editRouteMap(ua: UserAnswers)
                                     (implicit request: DataRequest[AnyContent]): PartialFunction[Identifier, Call] = {
    case PartnerNameId(establisherIndex, partnerIndex) => cyaDetails(establisherIndex,partnerIndex)
    case PartnerDOBId(establisherIndex,partnerIndex) => cyaDetails(establisherIndex,partnerIndex)
    case PartnerHasNINOId(establisherIndex, partnerIndex) => partnerHasNino(establisherIndex, partnerIndex, ua, CheckMode)
    case PartnerNINOId(establisherIndex, partnerIndex) => cyaDetails(establisherIndex,partnerIndex)
    case PartnerNoNINOReasonId(establisherIndex, partnerIndex) => cyaDetails(establisherIndex,partnerIndex)
    case PartnerHasUTRId(establisherIndex, partnerIndex) => establisherHasUtr(establisherIndex, partnerIndex, ua, CheckMode)
    case PartnerEnterUTRId(establisherIndex, partnerIndex) => cyaDetails(establisherIndex,partnerIndex)
    case PartnerNoUTRReasonId(establisherIndex, partnerIndex) => cyaDetails(establisherIndex,partnerIndex)
    case AddressId(establisherIndex, partnerIndex) => cyaDetails(establisherIndex,partnerIndex)
    case AddressListId(establisherIndex, partnerIndex) => cyaDetails(establisherIndex,partnerIndex)
    case AddressYearsId(establisherIndex, partnerIndex) =>
      if (ua.get(AddressYearsId(establisherIndex, partnerIndex)).contains(true)) cyaDetails(establisherIndex,partnerIndex)
      else prevPostcode(establisherIndex, partnerIndex, CheckMode)
    case EnterPostCodeId(establisherIndex, partnerIndex) => selectAddress(establisherIndex, partnerIndex, CheckMode)
    case EnterPreviousPostCodeId(establisherIndex, partnerIndex) => selectPrevAddress(establisherIndex, partnerIndex, CheckMode)
    case PreviousAddressId(establisherIndex, partnerIndex) => cyaDetails(establisherIndex,partnerIndex)
    case PreviousAddressListId(establisherIndex, partnerIndex) => cyaDetails(establisherIndex,partnerIndex)
    case EnterEmailId(establisherIndex, partnerIndex) => cyaDetails(establisherIndex,partnerIndex)
    case EnterPhoneId(establisherIndex, partnerIndex) => cyaDetails(establisherIndex,partnerIndex)
  }

  private def cyaDetails(establisherIndex:Int,partnerIndex:Int): Call = CheckYourAnswersController.onPageLoad(establisherIndex,partnerIndex)
  private def hassUTR(establisherIndex:Int,partnerIndex:Int,mode: Mode): Call = PartnerHasUTRController.onPageLoad(establisherIndex, partnerIndex, mode)
  private def addressYears(establisherIndex:Int,partnerIndex:Int,mode: Mode): Call = AddressYearsController.onPageLoad(establisherIndex, partnerIndex, mode)
  private def email(establisherIndex:Int,partnerIndex:Int,mode: Mode): Call = EnterEmailController.onPageLoad(establisherIndex, partnerIndex, mode)
  private def postcode(establisherIndex:Int,partnerIndex:Int,mode: Mode): Call = EnterPostcodeController.onPageLoad(establisherIndex, partnerIndex, mode)
  private def prevPostcode(establisherIndex:Int,partnerIndex:Int,mode: Mode): Call = EnterPreviousPostcodeController.onPageLoad(establisherIndex, partnerIndex, mode)
  private def selectAddress(establisherIndex:Int,partnerIndex:Int,mode: Mode): Call = SelectAddressController.onPageLoad(establisherIndex, partnerIndex, mode)
  private def selectPrevAddress(establisherIndex:Int,partnerIndex:Int,mode: Mode): Call = SelectPreviousAddressController.onPageLoad(establisherIndex, partnerIndex, mode)

  private def partnerHasNino(
                                  establisherIndex:Index,
                                  partnerIndex:Index,
                                  answers: UserAnswers,
                                  mode: Mode
                                ): Call =
    answers.get(PartnerHasNINOId(establisherIndex,partnerIndex)) match {
      case Some(true) => PartnerEnterNINOController.onPageLoad(establisherIndex,partnerIndex, mode)
      case Some(false) => PartnerNoNINOReasonController.onPageLoad(establisherIndex,partnerIndex, mode)
      case None => controllers.routes.TaskListController.onPageLoad()
    }

  private def establisherHasUtr(
                                 establisherIndex:Index,
                                 partnerIndex:Index,
                                 answers: UserAnswers,
                                 mode: Mode
                               ): Call =
    answers.get(PartnerHasUTRId(establisherIndex,partnerIndex)) match {
      case Some(true) => PartnerEnterUTRController.onPageLoad(establisherIndex,partnerIndex, mode)
      case Some(false) => PartnerNoUTRReasonController.onPageLoad(establisherIndex,partnerIndex, mode)
      case None => controllers.routes.TaskListController.onPageLoad()
    }

}
