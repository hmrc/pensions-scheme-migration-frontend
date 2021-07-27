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

import controllers.establishers.company.director.address.routes._
import controllers.establishers.company.director.details.routes._
import controllers.establishers.company.director.contact.routes._
import identifiers._
import identifiers.establishers.company.director._
import identifiers.establishers.company.director.address._
import models.requests.DataRequest
import models.{Index, Mode, NormalMode}
import play.api.mvc.{AnyContent, Call}
import utils.{Enumerable, UserAnswers}

class EstablishersCompanyDirectorNavigator
  extends Navigator
    with Enumerable.Implicits {

  //scalastyle:off cyclomatic.complexity
  override protected def routeMap(ua: UserAnswers)
                                 (implicit request: DataRequest[AnyContent]): PartialFunction[Identifier, Call] = {
    case DirectorNameId(establisherIndex, directorIndex) => DirectorDOBController.onPageLoad(establisherIndex,directorIndex, NormalMode)
    case DirectorDOBId(establisherIndex, directorIndex) => DirectorHasNINOController.onPageLoad(establisherIndex,directorIndex, NormalMode)
    case DirectorHasNINOId(establisherIndex, directorIndex) => directorHasNino(establisherIndex, directorIndex, ua, NormalMode)
    case DirectorNINOId(establisherIndex, directorIndex) => hassUTR(establisherIndex, directorIndex,NormalMode)
    case DirectorNoNINOReasonId(establisherIndex, directorIndex) => hassUTR(establisherIndex, directorIndex,NormalMode)
    case DirectorHasUTRId(establisherIndex, directorIndex) => establisherHasUtr(establisherIndex, directorIndex, ua, NormalMode)
    case DirectorEnterUTRId(establisherIndex, directorIndex) => postcode(establisherIndex, directorIndex)
    case DirectorNoUTRReasonId(establisherIndex, directorIndex) => postcode(establisherIndex, directorIndex)
    case AddressId(establisherIndex, directorIndex) => addressYears(establisherIndex, directorIndex)
    case AddressListId(establisherIndex, directorIndex) => addressYears(establisherIndex, directorIndex)
    case AddressYearsId(establisherIndex, directorIndex) =>
      if (ua.get(AddressYearsId(establisherIndex, directorIndex)).contains(true)) email(establisherIndex, directorIndex, NormalMode)
      else prevPostcode(establisherIndex, directorIndex)
    case EnterPostCodeId(establisherIndex, directorIndex) => selectAddress(establisherIndex, directorIndex)
    case EnterPreviousPostCodeId(establisherIndex, directorIndex) => selectPrevAddress(establisherIndex, directorIndex)
    case PreviousAddressId(establisherIndex, directorIndex) => email(establisherIndex, directorIndex, NormalMode)
    case PreviousAddressListId(establisherIndex, directorIndex) => email(establisherIndex, directorIndex, NormalMode)
    case DirectorEmailId(establisherIndex, directorIndex) => EnterPhoneNumberController.onPageLoad(establisherIndex, directorIndex, NormalMode)
    case DirectorPhoneNumberId(establisherIndex, directorIndex) =>cyaDetails(establisherIndex,directorIndex)
    case ConfirmDeleteDirectorId(establisherIndex) => controllers.establishers.company.routes.AddCompanyDirectorsController.onPageLoad(establisherIndex,NormalMode)
  }

  override protected def editRouteMap(ua: UserAnswers)
                                     (implicit request: DataRequest[AnyContent]): PartialFunction[Identifier, Call] = {
    case DirectorNameId(establisherIndex, directorIndex) => cyaDetails(establisherIndex,directorIndex)
    case DirectorDOBId(establisherIndex,directorIndex) => cyaDetails(establisherIndex,directorIndex)
    case DirectorHasNINOId(establisherIndex, directorIndex) => directorHasNino(establisherIndex, directorIndex, ua, NormalMode)
    case DirectorNINOId(establisherIndex, directorIndex) => cyaDetails(establisherIndex,directorIndex)
    case DirectorNoNINOReasonId(establisherIndex, directorIndex) => cyaDetails(establisherIndex,directorIndex)
    case DirectorHasUTRId(establisherIndex, directorIndex) => establisherHasUtr(establisherIndex, directorIndex, ua, NormalMode)
    case DirectorEnterUTRId(establisherIndex, directorIndex) => cyaDetails(establisherIndex,directorIndex)
    case DirectorNoUTRReasonId(establisherIndex, directorIndex) => cyaDetails(establisherIndex,directorIndex)
    case AddressId(establisherIndex, directorIndex) => addressYears(establisherIndex, directorIndex)
    case AddressListId(establisherIndex, directorIndex) => addressYears(establisherIndex, directorIndex)
    case AddressYearsId(establisherIndex, directorIndex) =>
      if (ua.get(AddressYearsId(establisherIndex, directorIndex)).contains(true)) cyaDetails(establisherIndex,directorIndex)
      else prevPostcode(establisherIndex, directorIndex)
    case EnterPostCodeId(establisherIndex, directorIndex) => selectAddress(establisherIndex, directorIndex)
    case EnterPreviousPostCodeId(establisherIndex, directorIndex) => selectPrevAddress(establisherIndex, directorIndex)
    case PreviousAddressId(establisherIndex, directorIndex) => email(establisherIndex, directorIndex, NormalMode)
    case PreviousAddressListId(establisherIndex, directorIndex) => email(establisherIndex, directorIndex, NormalMode)
    case DirectorEmailId(establisherIndex, directorIndex) => cyaDetails(establisherIndex,directorIndex)
    case DirectorPhoneNumberId(establisherIndex, directorIndex) => cyaDetails(establisherIndex,directorIndex)
  }

  private def cyaDetails(establisherIndex:Int,directorIndex:Int): Call = CheckYourAnswersController.onPageLoad(establisherIndex,directorIndex)
  private def hassUTR(establisherIndex:Int,directorIndex:Int,mode: Mode): Call = DirectorHasUTRController.onPageLoad(establisherIndex, directorIndex, mode)
  private def addressYears(establisherIndex:Int,directorIndex:Int): Call = AddressYearsController.onPageLoad(establisherIndex, directorIndex)
  private def email(establisherIndex:Int,directorIndex:Int,mode: Mode): Call = EnterEmailController.onPageLoad(establisherIndex, directorIndex, mode)
  private def postcode(establisherIndex:Int,directorIndex:Int): Call = EnterPostcodeController.onPageLoad(establisherIndex, directorIndex)
  private def prevPostcode(establisherIndex:Int,directorIndex:Int): Call = EnterPreviousPostcodeController.onPageLoad(establisherIndex, directorIndex)
  private def selectAddress(establisherIndex:Int,directorIndex:Int): Call = SelectAddressController.onPageLoad(establisherIndex, directorIndex)
  private def selectPrevAddress(establisherIndex:Int,directorIndex:Int): Call = SelectPreviousAddressController.onPageLoad(establisherIndex, directorIndex)

  private def directorHasNino(
                                  establisherIndex:Index,
                                  directorIndex:Index,
                                  answers: UserAnswers,
                                  mode: Mode
                                ): Call =
    answers.get(DirectorHasNINOId(establisherIndex,directorIndex)) match {
      case Some(true) => DirectorEnterNINOController.onPageLoad(establisherIndex,directorIndex, mode)
      case Some(false) => DirectorNoNINOReasonController.onPageLoad(establisherIndex,directorIndex, mode)
      case None => controllers.routes.TaskListController.onPageLoad()
    }

  private def establisherHasUtr(
                                 establisherIndex:Index,
                                 directorIndex:Index,
                                 answers: UserAnswers,
                                 mode: Mode
                               ): Call =
    answers.get(DirectorHasUTRId(establisherIndex,directorIndex)) match {
      case Some(true) => DirectorEnterUTRController.onPageLoad(establisherIndex,directorIndex, mode)
      case Some(false) => DirectorNoUTRReasonController.onPageLoad(establisherIndex,directorIndex, mode)
      case None => controllers.routes.TaskListController.onPageLoad()
    }

}
