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

import config.AppConfig
import controllers.establishers.partnership.address.routes._
import controllers.establishers.routes.AddEstablisherController
import controllers.routes.IndexController
import controllers.establishers.partnership.details.{routes => detailsRoutes}
import controllers.establishers.partnership.contact.routes.EnterPhoneController
import identifiers._
import identifiers.establishers.partnership.PartnershipDetailsId
import identifiers.establishers.partnership.address._
import identifiers.establishers.partnership.contact._
import identifiers.establishers.partnership.details.{HavePAYEId, HaveUTRId, HaveVATId, NoUTRReasonId, PAYEId, PartnershipUTRId, VATId}
import models.requests.DataRequest
import models.{CheckMode, Index, Mode, NormalMode}
import play.api.mvc.{AnyContent, Call}
import utils.{Enumerable, UserAnswers}

import javax.inject.Inject

class EstablishersPartnershipNavigator@Inject()(config: AppConfig)
  extends Navigator
    with Enumerable.Implicits {

  //scalastyle:off cyclomatic.complexity
  override protected def routeMap(ua: UserAnswers)
                                 (implicit request: DataRequest[AnyContent]): PartialFunction[Identifier, Call] = {
    case PartnershipDetailsId(index) => AddEstablisherController.onPageLoad()
    case EnterEmailId(index) => EnterPhoneController.onPageLoad(index, NormalMode)
    case EnterPhoneId(index) => cyaContactDetails(index)
    case HaveUTRId(index) => utrRoutes(index, ua, NormalMode)
    case PartnershipUTRId(index) => detailsRoutes.HaveVATController.onPageLoad(index, NormalMode)
    case NoUTRReasonId(index) => detailsRoutes.HaveVATController.onPageLoad(index, NormalMode)
    case HaveVATId(index) => vatRoutes(index, ua, NormalMode)
    case VATId(index) => detailsRoutes.HavePAYEController.onPageLoad(index, NormalMode)
    case HavePAYEId(index) => payeRoutes(index, ua, NormalMode)
    case PAYEId(index) => detailsRoutes.CheckYourAnswersController.onPageLoad(index)
    case EnterPostCodeId(index) => SelectAddressController.onPageLoad(index)
    case AddressListId(index) => addressYears(index, NormalMode)
    case AddressId(index) => addressYears(index, NormalMode)
    case AddressYearsId(index) =>
      if (ua.get(AddressYearsId(index)).contains(true)) cyaAddress(index) else TradingTimeController.onPageLoad(index)
    case TradingTimeId(index) =>
      if (ua.get(TradingTimeId(index)).contains(true)) EnterPreviousPostcodeController.onPageLoad(index) else cyaAddress(index)
    case EnterPreviousPostCodeId(index) => SelectPreviousAddressController.onPageLoad(index)
    case PreviousAddressListId(index) => cyaAddress(index)
    case PreviousAddressId(index) => cyaAddress(index)
  }

 override protected def editRouteMap(ua: UserAnswers)(implicit request: DataRequest[AnyContent]): PartialFunction[Identifier, Call] = {
   case PartnershipDetailsId(_) => IndexController.onPageLoad()
   case HaveUTRId(index) => utrRoutes(index, ua, CheckMode)
   case PartnershipUTRId(index) => detailsRoutes.CheckYourAnswersController.onPageLoad(index)
   case NoUTRReasonId(index) => detailsRoutes.CheckYourAnswersController.onPageLoad(index)
   case HaveVATId(index) => vatRoutes(index, ua, CheckMode)
   case VATId(index) => detailsRoutes.CheckYourAnswersController.onPageLoad(index)
   case HavePAYEId(index) => payeRoutes(index, ua, CheckMode)
   case PAYEId(index) => detailsRoutes.CheckYourAnswersController.onPageLoad(index)
   case EnterEmailId(index) => cyaContactDetails(index)
   case EnterPhoneId(index) => cyaContactDetails(index)
  }

  private def cyaAddress(index:Int): Call = controllers.establishers.partnership.address.routes.CheckYourAnswersController.onPageLoad(index)
  private def addressYears(index:Int, mode:Mode): Call = controllers.establishers.partnership.address.routes.AddressYearsController.onPageLoad(index)

  private def utrRoutes(
                         index: Index,
                         answers: UserAnswers,
                         mode: Mode
                       ): Call =
    answers.get(HaveUTRId(index)) match {
      case Some(true) => detailsRoutes.UTRController.onPageLoad(index, mode)
      case Some(false) => detailsRoutes.NoUTRReasonController.onPageLoad(index, mode)
      case None => controllers.routes.TaskListController.onPageLoad()
    }

  private def cyaContactDetails(index:Int): Call = controllers.establishers.partnership.contact.routes.CheckYourAnswersController.onPageLoad(index)

  private def vatRoutes(
                         index: Index,
                         answers: UserAnswers,
                         mode: Mode
                       ): Call =
    answers.get(HaveVATId(index)) match {
      case Some(true) => detailsRoutes.VATController.onPageLoad(index, mode)
      case Some(false) if mode == NormalMode => detailsRoutes.HavePAYEController.onPageLoad(index, mode)
      case Some(false) => detailsRoutes.CheckYourAnswersController.onPageLoad(index)
      case None => controllers.routes.TaskListController.onPageLoad()
    }

  private def payeRoutes(
                          index: Index,
                          answers: UserAnswers,
                          mode: Mode
                        ): Call =
    answers.get(HavePAYEId(index)) match {
      case Some(true) => detailsRoutes.PAYEController.onPageLoad(index, mode)
      case Some(false) => detailsRoutes.CheckYourAnswersController.onPageLoad(index)
      case None => controllers.routes.TaskListController.onPageLoad()
    }

}
