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

import controllers.establishers.company.address.routes._
import controllers.establishers.company.contact.routes._
import config.AppConfig
import controllers.establishers.company.address.routes._
import controllers.establishers.company.details.{routes => detailsRoutes}
import controllers.establishers.routes._
import controllers.routes._
import identifiers._
import identifiers.establishers.company.address._
import identifiers.establishers.company.contact.{EnterEmailId, EnterPhoneId}
import identifiers.establishers.company.details._
import identifiers.establishers.individual.EstablisherNameId
import identifiers.establishers.company.{AddCompanyDirectorsId, CompanyDetailsId}
import models.requests.DataRequest
import models.{CheckMode, Index, Mode, NormalMode}
import play.api.mvc.{AnyContent, Call}
import utils.{Enumerable, UserAnswers}
import models.{Mode, NormalMode}
import play.api.mvc.{AnyContent, Call}
import utils.{Enumerable, UserAnswers}
import models.{Mode, NormalMode}
import play.api.mvc.{AnyContent, Call}
import utils.{Enumerable, UserAnswers}

import javax.inject.Inject

class EstablishersCompanyNavigator@Inject()(config: AppConfig)
  extends Navigator
    with Enumerable.Implicits {

  //scalastyle:off cyclomatic.complexity
  override protected def routeMap(ua: UserAnswers)
                                 (implicit request: DataRequest[AnyContent]): PartialFunction[Identifier, Call] = {
    case EstablisherNameId(_) => AddEstablisherController.onPageLoad()
    case EnterEmailId(index) => EnterPhoneController.onPageLoad(index, NormalMode)
    case EnterPhoneId(index) => cyaContactDetails(index)
    case HaveCompanyNumberId(index) => companyNumberRoutes(index, ua, NormalMode)
    case CompanyNumberId(index) => detailsRoutes.HaveUTRController.onPageLoad(index, NormalMode)
    case NoCompanyNumberReasonId(index) => detailsRoutes.HaveUTRController.onPageLoad(index, NormalMode)
    case HaveUTRId(index) => utrRoutes(index, ua, NormalMode)
    case CompanyUTRId(index) => detailsRoutes.HaveVATController.onPageLoad(index, NormalMode)
    case NoUTRReasonId(index) => detailsRoutes.HaveVATController.onPageLoad(index, NormalMode)
    case HaveVATId(index) => vatRoutes(index, ua, NormalMode)
    case VATId(index) => detailsRoutes.HavePAYEController.onPageLoad(index, NormalMode)
    case HavePAYEId(index) => payeRoutes(index, ua, NormalMode)
    case PAYEId(index) => detailsRoutes.CheckYourAnswersController.onPageLoad(index)
    case CompanyDetailsId(_) => AddEstablisherController.onPageLoad()
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
    case AddCompanyDirectorsId(index) =>
      addDirectors(index, ua)

  }

  override protected def editRouteMap(ua: UserAnswers)
                                     (implicit request: DataRequest[AnyContent]): PartialFunction[Identifier, Call] = {
    case CompanyDetailsId(_) => IndexController.onPageLoad()
    case HaveCompanyNumberId(index) => companyNumberRoutes(index, ua, CheckMode)
    case CompanyNumberId(index) => detailsRoutes.CheckYourAnswersController.onPageLoad(index)
    case NoCompanyNumberReasonId(index) => detailsRoutes.CheckYourAnswersController.onPageLoad(index)
    case HaveUTRId(index) => utrRoutes(index, ua, CheckMode)
    case CompanyUTRId(index) => detailsRoutes.CheckYourAnswersController.onPageLoad(index)
    case NoUTRReasonId(index) => detailsRoutes.CheckYourAnswersController.onPageLoad(index)
    case HaveVATId(index) => vatRoutes(index, ua, CheckMode)
    case VATId(index) => detailsRoutes.CheckYourAnswersController.onPageLoad(index)
    case HavePAYEId(index) => payeRoutes(index, ua, CheckMode)
    case PAYEId(index) => detailsRoutes.CheckYourAnswersController.onPageLoad(index)
    case EnterEmailId(index) => cyaContactDetails(index)
    case EnterPhoneId(index) => cyaContactDetails(index)
  }

  private def cyaAddress(index:Int): Call = controllers.establishers.company.address.routes.CheckYourAnswersController.onPageLoad(index)
  private def addressYears(index:Int, mode:Mode): Call = controllers.establishers.company.address.routes.AddressYearsController.onPageLoad(index)
  private def addDirectors(index: Int, answers: UserAnswers): Call = {
    if (answers.allDirectorsAfterDelete(index).isEmpty) {
      controllers.establishers.company.director.routes.DirectorNameController
        .onPageLoad(index, answers.allDirectors(index).size, NormalMode)
    } else if (answers.allDirectorsAfterDelete(index).length < config.maxDirectors) {
      answers.get(AddCompanyDirectorsId(index)).map { addCompanyDirectors =>
        if (addCompanyDirectors) {
          controllers.establishers.company.director.routes.DirectorNameController
            .onPageLoad(index, answers.allDirectors(index).size, NormalMode)
        } else {
          controllers.routes.TaskListController.onPageLoad()
        }
      }.getOrElse(controllers.routes.TaskListController.onPageLoad())

    }else {
      controllers.establishers.company.routes.OtherDirectorsController.onPageLoad(index,NormalMode)
    }
  }
  private def companyNumberRoutes(
                                  index: Index,
                                  answers: UserAnswers,
                                  mode: Mode
                                ): Call =
    answers.get(HaveCompanyNumberId(index)) match {
      case Some(true) => detailsRoutes.CompanyNumberController.onPageLoad(index, mode)
      case Some(false) => detailsRoutes.NoCompanyNumberReasonController.onPageLoad(index, mode)
      case None => controllers.routes.TaskListController.onPageLoad()
    }

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
  private def cyaContactDetails(index:Int): Call = controllers.establishers.company.contact.routes.CheckYourAnswersController.onPageLoad(index)

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
