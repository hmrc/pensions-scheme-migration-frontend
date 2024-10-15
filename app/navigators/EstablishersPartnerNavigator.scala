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
import models._
import play.api.mvc.{AnyContent, Call}
import utils.{Enumerable, UserAnswers}

class EstablishersPartnerNavigator
  extends Navigator
    with Enumerable.Implicits {

  //scalastyle:off cyclomatic.complexity
  override protected def routeMap(ua: UserAnswers)
                                 (implicit request: DataRequest[AnyContent]): PartialFunction[Identifier, Call] = {
    case PartnerNameId(estIndex, partnerIndex) => PartnerDOBController.onPageLoad(estIndex,partnerIndex, NormalMode)
    case PartnerDOBId(estIndex, partnerIndex) => PartnerHasNINOController.onPageLoad(estIndex,partnerIndex, NormalMode)
    case PartnerHasNINOId(estIndex, partnerIndex) => partnerHasNino(estIndex, partnerIndex, ua, NormalMode)
    case PartnerNINOId(estIndex, partnerIndex) => hassUTR(estIndex, partnerIndex,NormalMode)
    case PartnerNoNINOReasonId(estIndex, partnerIndex) => hassUTR(estIndex, partnerIndex,NormalMode)
    case PartnerHasUTRId(estIndex, partnerIndex) => establisherHasUtr(estIndex, partnerIndex, ua, NormalMode)
    case PartnerEnterUTRId(estIndex, partnerIndex) => postcode(estIndex, partnerIndex, NormalMode)
    case PartnerNoUTRReasonId(estIndex, partnerIndex) => postcode(estIndex, partnerIndex, NormalMode)
    case AddressId(estIndex, partnerIndex) => addressYears(estIndex, partnerIndex, NormalMode)
    case AddressListId(estIndex, partnerIndex) => addressYears(estIndex, partnerIndex, NormalMode)
    case AddressYearsId(estIndex, partnerIndex) =>
      if (ua.get(AddressYearsId(estIndex, partnerIndex)).contains(true)) email(estIndex, partnerIndex, NormalMode)
      else prevPostcode(estIndex, partnerIndex, NormalMode)
    case EnterPostCodeId(estIndex, partnerIndex) => selectAddress(estIndex, partnerIndex, NormalMode)
    case EnterPreviousPostCodeId(estIndex, partnerIndex) => selectPrevAddress(estIndex, partnerIndex, NormalMode)
    case PreviousAddressId(estIndex, partnerIndex) => email(estIndex, partnerIndex, NormalMode)
    case PreviousAddressListId(estIndex, partnerIndex) => email(estIndex, partnerIndex, NormalMode)
    case EnterEmailId(estIndex, partnerIndex) => EnterPhoneNumberController.onPageLoad(estIndex, partnerIndex, NormalMode)
    case EnterPhoneId(estIndex, partnerIndex) =>cyaDetails(estIndex,partnerIndex)
    case ConfirmDeletePartnerId(estIndex) => controllers.establishers.partnership.routes.AddPartnersController.onPageLoad(estIndex,NormalMode)
  }

  override protected def editRouteMap(ua: UserAnswers)
                                     (implicit request: DataRequest[AnyContent]): PartialFunction[Identifier, Call] = {
    case PartnerNameId(estIndex, partnerIndex) => cyaDetails(estIndex,partnerIndex)
    case PartnerDOBId(estIndex,partnerIndex) => cyaDetails(estIndex,partnerIndex)
    case PartnerHasNINOId(estIndex, partnerIndex) => partnerHasNino(estIndex, partnerIndex, ua, CheckMode)
    case PartnerNINOId(estIndex, partnerIndex) => cyaDetails(estIndex,partnerIndex)
    case PartnerNoNINOReasonId(estIndex, partnerIndex) => cyaDetails(estIndex,partnerIndex)
    case PartnerHasUTRId(estIndex, partnerIndex) => establisherHasUtr(estIndex, partnerIndex, ua, CheckMode)
    case PartnerEnterUTRId(estIndex, partnerIndex) => cyaDetails(estIndex,partnerIndex)
    case PartnerNoUTRReasonId(estIndex, partnerIndex) => cyaDetails(estIndex,partnerIndex)
    case AddressId(estIndex, partnerIndex) => cyaDetails(estIndex,partnerIndex)
    case AddressListId(estIndex, partnerIndex) => cyaDetails(estIndex,partnerIndex)
    case AddressYearsId(estIndex, partnerIndex) =>
      if (ua.get(AddressYearsId(estIndex, partnerIndex)).contains(true)) cyaDetails(estIndex,partnerIndex)
      else prevPostcode(estIndex, partnerIndex, CheckMode)
    case EnterPostCodeId(estIndex, partnerIndex) => selectAddress(estIndex, partnerIndex, CheckMode)
    case EnterPreviousPostCodeId(estIndex, partnerIndex) => selectPrevAddress(estIndex, partnerIndex, CheckMode)
    case PreviousAddressId(estIndex, partnerIndex) => cyaDetails(estIndex,partnerIndex)
    case PreviousAddressListId(estIndex, partnerIndex) => cyaDetails(estIndex,partnerIndex)
    case EnterEmailId(estIndex, partnerIndex) => cyaDetails(estIndex,partnerIndex)
    case EnterPhoneId(estIndex, partnerIndex) => cyaDetails(estIndex,partnerIndex)
  }

  private def cyaDetails(estIndex:Int,partnerIndex:Int): Call = controllers.common.routes.CheckYourAnswersController.onPageLoadWithRepresentative(estIndex, entities.Establisher, entities.Partnership, partnerIndex)
  private def hassUTR(estIndex:Int,partnerIndex:Int,mode: Mode): Call = PartnerHasUTRController.onPageLoad(estIndex, partnerIndex, mode)
  private def addressYears(estIndex:Int,partnerIndex:Int,mode: Mode): Call = AddressYearsController.onPageLoad(estIndex, partnerIndex, mode)
  private def email(estIndex:Int,partnerIndex:Int,mode: Mode): Call = EnterEmailController.onPageLoad(estIndex, partnerIndex, mode)
  private def postcode(estIndex:Int,partnerIndex:Int,mode: Mode): Call = EnterPostcodeController.onPageLoad(estIndex, partnerIndex, mode)
  private def prevPostcode(estIndex:Int,partnerIndex:Int,mode: Mode): Call = EnterPreviousPostcodeController.onPageLoad(estIndex, partnerIndex, mode)
  private def selectAddress(estIndex:Int,partnerIndex:Int,mode: Mode): Call = SelectAddressController.onPageLoad(estIndex, partnerIndex, mode)
  private def selectPrevAddress(estIndex:Int,partnerIndex:Int,mode: Mode): Call = SelectPreviousAddressController.onPageLoad(estIndex, partnerIndex, mode)

  private def partnerHasNino(
                                  estIndex:Index,
                                  partnerIndex:Index,
                                  answers: UserAnswers,
                                  mode: Mode
                                ): Call =
    answers.get(PartnerHasNINOId(estIndex,partnerIndex)) match {
      case Some(true) => PartnerEnterNINOController.onPageLoad(estIndex,partnerIndex, mode)
      case Some(false) => PartnerNoNINOReasonController.onPageLoad(estIndex,partnerIndex, mode)
      case None => controllers.routes.TaskListController.onPageLoad
    }

  private def establisherHasUtr(
                                 estIndex:Index,
                                 partnerIndex:Index,
                                 answers: UserAnswers,
                                 mode: Mode
                               ): Call =
    answers.get(PartnerHasUTRId(estIndex,partnerIndex)) match {
      case Some(true) => PartnerEnterUTRController.onPageLoad(estIndex,partnerIndex, mode)
      case Some(false) => PartnerNoUTRReasonController.onPageLoad(estIndex,partnerIndex, mode)
      case None => controllers.routes.TaskListController.onPageLoad
    }

}
