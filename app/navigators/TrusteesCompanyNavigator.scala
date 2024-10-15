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

import controllers.trustees.company.address.routes.{EnterPreviousPostcodeController, SelectAddressController, SelectPreviousAddressController, TradingTimeController}
import controllers.trustees.company.contacts.routes._
import controllers.trustees.company.details.{routes => detailsRoutes}
import identifiers._
import identifiers.trustees.company.CompanyDetailsId
import identifiers.trustees.company.address._
import identifiers.trustees.company.contacts.{EnterEmailId, EnterPhoneId}
import identifiers.trustees.company.details._
import models._
import models.requests.DataRequest
import play.api.mvc.{AnyContent, Call}
import utils.{Enumerable, UserAnswers}

class TrusteesCompanyNavigator
  extends Navigator
    with Enumerable.Implicits {

  private def cyaDetails(index: Index) = controllers.common.routes.CheckYourAnswersController.onPageLoad(index: Index, entities.Trustee, entities.Company, entities.Details)
  //scalastyle:off cyclomatic.complexity
  override protected def routeMap(ua: UserAnswers)
                                 (implicit request: DataRequest[AnyContent]): PartialFunction[Identifier, Call] = {
    case CompanyDetailsId(index) => controllers.common.routes.SpokeTaskListController.onPageLoad(index:Index, entities.Trustee, entities.Company)
    case HaveCompanyNumberId(index) => companyNumberRoutes(index, ua, NormalMode)
    case CompanyNumberId(index) => detailsRoutes.HaveUTRController.onPageLoad(index, NormalMode)
    case NoCompanyNumberReasonId(index) => detailsRoutes.HaveUTRController.onPageLoad(index, NormalMode)
    case HaveUTRId(index) => utrRoutes(index, ua, NormalMode)
    case CompanyUTRId(index) => detailsRoutes.HaveVATController.onPageLoad(index, NormalMode)
    case NoUTRReasonId(index) => detailsRoutes.HaveVATController.onPageLoad(index, NormalMode)
    case HaveVATId(index) => vatRoutes(index, ua, NormalMode)
    case VATId(index) => detailsRoutes.HavePAYEController.onPageLoad(index, NormalMode)
    case HavePAYEId(index) => payeRoutes(index, ua, NormalMode)
    case PAYEId(index) => cyaDetails(index)
    case EnterPostCodeId(index) => SelectAddressController.onPageLoad(index, NormalMode)
    case AddressListId(index) => addressYears(index, NormalMode)
    case AddressId(index) => addressYears(index, NormalMode)

    case AddressYearsId(index) =>
      if (ua.get(AddressYearsId(index)).contains(true)) cyaAddress(index) else TradingTimeController.onPageLoad(index, NormalMode)
    case TradingTimeId(index) =>
      if (ua.get(TradingTimeId(index)).contains(true)) EnterPreviousPostcodeController.onPageLoad(index, NormalMode) else cyaAddress(index)

    case EnterPreviousPostCodeId(index) => SelectPreviousAddressController.onPageLoad(index, NormalMode)
    case PreviousAddressListId(index) => cyaAddress(index)
    case PreviousAddressId(index) => cyaAddress(index)

    case EnterEmailId(index) => EnterPhoneController.onPageLoad(index, NormalMode)
    case EnterPhoneId(index) => cyaContactDetails(index)

  }

  override protected def editRouteMap(ua: UserAnswers)
                                     (implicit request: DataRequest[AnyContent]): PartialFunction[Identifier, Call] = {
    case CompanyDetailsId(_) => throw new RuntimeException("index page unavailable")
    case HaveCompanyNumberId(index) => companyNumberRoutes(index, ua, CheckMode)
    case CompanyNumberId(index) => cyaDetails(index)
    case NoCompanyNumberReasonId(index) => cyaDetails(index)
    case HaveUTRId(index) => utrRoutes(index, ua, CheckMode)
    case CompanyUTRId(index) => cyaDetails(index)
    case NoUTRReasonId(index) => cyaDetails(index)
    case HaveVATId(index) => vatRoutes(index, ua, CheckMode)
    case VATId(index) => cyaDetails(index)
    case HavePAYEId(index) => payeRoutes(index, ua, CheckMode)
    case PAYEId(index) => cyaDetails(index)
    case EnterEmailId(index) => EnterPhoneController.onPageLoad(index, NormalMode)
    case EnterPhoneId(index) => cyaContactDetails(index)
    case AddressId(index) => cyaAddress(index)
    case AddressListId(index) => cyaAddress(index)
    case AddressYearsId(index) =>
      if (ua.get(AddressYearsId(index)).contains(true)) cyaAddress(index)
      else EnterPreviousPostcodeController.onPageLoad(index, CheckMode)
    case TradingTimeId(index) =>
      if (ua.get(TradingTimeId(index)).contains(true)) EnterPreviousPostcodeController.onPageLoad(index, CheckMode) else cyaAddress(index)
    case EnterPostCodeId(index) => SelectAddressController.onPageLoad(index, CheckMode)
    case EnterPreviousPostCodeId(index) => SelectPreviousAddressController.onPageLoad(index, CheckMode)
    case PreviousAddressId(index) => cyaAddress(index)
    case PreviousAddressListId(index) => cyaAddress(index)
  }

  private def companyNumberRoutes(
                                   index: Index,
                                   answers: UserAnswers,
                                   mode: Mode
                                 ): Call =
    answers.get(HaveCompanyNumberId(index)) match {
      case Some(true) => detailsRoutes.CompanyNumberController.onPageLoad(index, mode)
      case Some(false) => detailsRoutes.NoCompanyNumberReasonController.onPageLoad(index, mode)
      case None => controllers.routes.TaskListController.onPageLoad
    }

  private def utrRoutes(
                         index: Index,
                         answers: UserAnswers,
                         mode: Mode
                       ): Call =
    answers.get(HaveUTRId(index)) match {
      case Some(true) => detailsRoutes.UTRController.onPageLoad(index, mode)
      case Some(false) => detailsRoutes.NoUTRReasonController.onPageLoad(index, mode)
      case None => controllers.routes.TaskListController.onPageLoad
    }

  private def vatRoutes(
                         index: Index,
                         answers: UserAnswers,
                         mode: Mode
                       ): Call =
    answers.get(HaveVATId(index)) match {
      case Some(true) => detailsRoutes.VATController.onPageLoad(index, mode)
      case Some(false) if mode == NormalMode => detailsRoutes.HavePAYEController.onPageLoad(index, mode)
      case Some(false) => cyaDetails(index)
      case None => controllers.routes.TaskListController.onPageLoad
    }

  private def payeRoutes(
                          index: Index,
                          answers: UserAnswers,
                          mode: Mode
                        ): Call =
    answers.get(HavePAYEId(index)) match {
      case Some(true) => detailsRoutes.PAYEController.onPageLoad(index, mode)
      case Some(false) => cyaDetails(index)
      case None => controllers.routes.TaskListController.onPageLoad
    }

  private def cyaAddress(index:Int): Call = controllers.common.routes.CheckYourAnswersController.onPageLoad(index, entities.Trustee, entities.Company, entities.Address)
  private def addressYears(index:Int, mode:Mode): Call = controllers.trustees.company.address.routes.AddressYearsController.onPageLoad(index,mode)

  private def cyaContactDetails(index:Int): Call = controllers.common.routes.CheckYourAnswersController.onPageLoad(index, entities.Trustee, entities.Company, entities.Contacts)
}
