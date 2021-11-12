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

import controllers.routes._
import controllers.trustees.routes._
import helpers.routes.TrusteesIndividualRoutes
import identifiers._
import identifiers.trustees.{AnyTrusteesId, _}
import identifiers.trustees.individual.TrusteeNameId
import identifiers.trustees.individual.address._
import identifiers.trustees.individual.contact.{EnterEmailId, EnterPhoneId}
import identifiers.trustees.individual.details._
import models.requests.DataRequest
import models.trustees.TrusteeKind
import models.{CheckMode, Index, Mode, NormalMode}
import play.api.mvc.{AnyContent, Call}
import utils.{Enumerable, UserAnswers}

class TrusteesNavigator
  extends Navigator
    with Enumerable.Implicits {

  //scalastyle:off cyclomatic.complexity
  override protected def routeMap(ua: UserAnswers)
                                 (implicit request: DataRequest[AnyContent]): PartialFunction[Identifier, Call] = {
    case TrusteeKindId(index) => trusteeKindRoutes(index, ua)
    case AnyTrusteesId(value) => anyTrusteesRoutes(value, ua)
    case TrusteeNameId(_) => AddTrusteeController.onPageLoad()
    case AddTrusteeId(value) => addTrusteeRoutes(value, ua)
    case ConfirmDeleteTrusteeId => AddTrusteeController.onPageLoad()
    case TrusteeDOBId(index) => TrusteesIndividualRoutes.haveNationalInsuranceNumberRoute(index, NormalMode)
    case TrusteeHasNINOId(index) => trusteeHasNino(index, ua, NormalMode)
    case TrusteeNINOId(index) => TrusteesIndividualRoutes.haveUniqueTaxpayerReferenceRoute(index, NormalMode)
    case TrusteeNoNINOReasonId(index) => TrusteesIndividualRoutes.haveUniqueTaxpayerReferenceRoute(index, NormalMode)
    case TrusteeHasUTRId(index) => trusteeHasUtr(index, ua, NormalMode)
    case TrusteeUTRId(index) => cyaDetails(index)
    case TrusteeNoUTRReasonId(index) => cyaDetails(index)
    case EnterPostCodeId(index) => TrusteesIndividualRoutes.selectAddressRoute(index, NormalMode)
    case AddressListId(index) => addressYears(index, NormalMode)
    case AddressId(index) => addressYears(index, NormalMode)
    case AddressYearsId(index) =>
      if (ua.get(AddressYearsId(index)).contains(true)) cyaAddress(index) else TrusteesIndividualRoutes.enterPreviousPostcodeRoute(index, NormalMode)
    case EnterPreviousPostCodeId(index) => TrusteesIndividualRoutes.previousAddressResultsRoute(index, NormalMode)
    case PreviousAddressListId(index) => cyaAddress(index)
    case PreviousAddressId(index) => cyaAddress(index)
    case EnterEmailId(index) => TrusteesIndividualRoutes.phoneNumberRoute(index, NormalMode)
    case EnterPhoneId(index) => cyaContactDetails(index)
    case OtherTrusteesId => TaskListController.onPageLoad()
  }

  override protected def editRouteMap(ua: UserAnswers)
                                     (implicit request: DataRequest[AnyContent]): PartialFunction[Identifier, Call] = {
    case TrusteeDOBId(index) => cyaDetails(index)
    case TrusteeHasNINOId(index) => trusteeHasNino(index, ua, CheckMode)
    case TrusteeNINOId(index) => cyaDetails(index)
    case TrusteeNoNINOReasonId(index) => cyaDetails(index)
    case TrusteeHasUTRId(index) => trusteeHasUtr(index, ua, CheckMode)
    case TrusteeUTRId(index) => cyaDetails(index)
    case TrusteeNoUTRReasonId(index) => cyaDetails(index)
    case EnterEmailId(index) => cyaContactDetails(index)
    case EnterPhoneId(index) => cyaContactDetails(index)
  }

  private def cyaDetails(index: Int): Call = TrusteesIndividualRoutes.cyaDetailsRoute(index, NormalMode)

  private def cyaAddress(index: Int): Call = TrusteesIndividualRoutes.cyaAddressRoute(index, NormalMode)

  private def addressYears(index: Int, mode: Mode): Call = TrusteesIndividualRoutes.timeAtAddressRoute(index, NormalMode)

  private def cyaContactDetails(index: Int): Call = TrusteesIndividualRoutes.cyaContactRoute(index, NormalMode)

  private def trusteeKindRoutes(
                                 index: Index,
                                 ua: UserAnswers
                               ): Call =
    ua.get(TrusteeKindId(index)) match {
      case Some(TrusteeKind.Individual) => TrusteesIndividualRoutes.nameRoute(index, NormalMode)
      case Some(TrusteeKind.Company) => controllers.trustees.company.routes.CompanyDetailsController.onPageLoad(index)
      case Some(TrusteeKind.Partnership) => controllers.trustees.partnership.routes.PartnershipDetailsController.onPageLoad(index)
      case _ => IndexController.onPageLoad()
    }

  private def addTrusteeRoutes(
                                value: Option[Boolean],
                                answers: UserAnswers
                              ): Call =
    value match {
      case Some(false) => TaskListController.onPageLoad()
      case Some(true) => TrusteeKindController.onPageLoad(answers.trusteesCount)
      case None => controllers.trustees.routes.OtherTrusteesController.onPageLoad
    }

  private def anyTrusteesRoutes(
                                 value: Option[Boolean],
                                 answers: UserAnswers,
                               ): Call = {

  println("\n\n\n\n\n\n\n\nAnyTrustees" +value)
    value match {
    case Some(false) => TaskListController.onPageLoad()
    case Some(true) => TrusteeKindController.onPageLoad(answers.trusteesCount)
    case None => IndexController.onPageLoad()
  }
}

  private def trusteeHasNino(
                                  index: Index,
                                  answers: UserAnswers,
                                  mode: Mode
                                ): Call =
    answers.get(TrusteeHasNINOId(index)) match {
      case Some(true) => TrusteesIndividualRoutes.enterNationaInsuranceNumberRoute(index, mode)
      case Some(false) => TrusteesIndividualRoutes.reasonForNoNationalInsuranceNumberRoute(index, mode)
      case None => controllers.routes.TaskListController.onPageLoad()
    }

  private def trusteeHasUtr(
                                 index: Index,
                                 answers: UserAnswers,
                                 mode: Mode
                               ): Call =
    answers.get(TrusteeHasUTRId(index)) match {
      case Some(true) => TrusteesIndividualRoutes.enterUniqueTaxpayerReferenceRoute(index, mode)
      case Some(false) => TrusteesIndividualRoutes.reasonForNoUniqueTaxpayerReferenceRoute(index, mode)
      case None => controllers.routes.TaskListController.onPageLoad()
    }
}
