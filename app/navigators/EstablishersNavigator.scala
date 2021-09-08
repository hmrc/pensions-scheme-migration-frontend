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
import controllers.establishers.company.routes.CompanyDetailsController
import controllers.establishers.partnership.routes._
import controllers.establishers.routes._
import controllers.routes._
import helpers.routes.EstablishersIndividualRoutes
import helpers.routes.EstablishersIndividualRoutes._
import identifiers._
import identifiers.establishers._
import identifiers.establishers.individual.EstablisherNameId
import identifiers.establishers.individual.address._
import identifiers.establishers.individual.contact.{EnterEmailId, EnterPhoneId}
import identifiers.establishers.individual.details._
import identifiers.establishers.partnership.AddPartnersId
import models.establishers.EstablisherKind
import models.requests.DataRequest
import models.{Mode, Index, CheckMode, NormalMode}
import play.api.mvc.{Call, AnyContent}
import utils.{UserAnswers, Enumerable}

import javax.inject.Inject

class EstablishersNavigator@Inject()(config: AppConfig)
  extends Navigator
    with Enumerable.Implicits {

  //scalastyle:off cyclomatic.complexity
  override protected def routeMap(ua: UserAnswers)
                                 (implicit request: DataRequest[AnyContent]): PartialFunction[Identifier, Call] = {
    case EstablisherKindId(index) => establisherKindRoutes(index, ua)
    case EstablisherNameId(_) => AddEstablisherController.onPageLoad()
    case AddEstablisherId(value) => addEstablisherRoutes(value, ua)
    case ConfirmDeleteEstablisherId => AddEstablisherController.onPageLoad()
    case EstablisherDOBId(index) => controllers.establishers.individual.details.routes.EstablisherHasNINOController.onPageLoad(index, NormalMode)
    case EstablisherHasNINOId(index) => establisherHasNino(index, ua, NormalMode)
    case EstablisherNINOId(index) => controllers.establishers.individual.details.routes.EstablisherHasNINOController.onPageLoad(index, NormalMode)
    case EstablisherNoNINOReasonId(index) => controllers.establishers.individual.details.routes.EstablisherHasUTRController.onPageLoad(index, NormalMode)
    case EstablisherHasUTRId(index) => establisherHasUtr(index, ua, NormalMode)
    case EstablisherUTRId(index) => cyaDetails(index)
    case EstablisherNoUTRReasonId(index) => cyaDetails(index)
    case EnterPostCodeId(index) => EstablishersIndividualRoutes.selectAddressRoute(index, NormalMode)
    case AddressListId(index) => addressYears(index, NormalMode)
    case AddressId(index) => addressYears(index, NormalMode)
    case AddressYearsId(index) =>
      if (ua.get(AddressYearsId(index)).contains(true)) cyaAddress(index) else EstablishersIndividualRoutes.enterPreviousPostcodeRoute(index, NormalMode)
    case EnterPreviousPostCodeId(index) => EstablishersIndividualRoutes.previousAddressResultsRoute(index, NormalMode)
    case PreviousAddressListId(index) => cyaAddress(index)
    case PreviousAddressId(index) => cyaAddress(index)
    case EnterEmailId(index) => phoneNumberRoute(index, NormalMode)
    case EnterPhoneId(index) => cyaContactDetails(index)
    case AddPartnersId(index) => addPartners(index, ua)
  }

  override protected def editRouteMap(ua: UserAnswers)
                                     (implicit request: DataRequest[AnyContent]): PartialFunction[Identifier, Call] = {
    case EstablisherDOBId(index) => cyaDetails(index)
    case EstablisherHasNINOId(index) => establisherHasNino(index, ua, CheckMode)
    case EstablisherNINOId(index) => cyaDetails(index)
    case EstablisherNoNINOReasonId(index) => cyaDetails(index)
    case EstablisherHasUTRId(index) => establisherHasUtr(index, ua, CheckMode)
    case EstablisherUTRId(index) => cyaDetails(index)
    case EstablisherNoUTRReasonId(index) => cyaDetails(index)
    case EnterEmailId(index) => cyaContactDetails(index)
    case EnterPhoneId(index) => cyaContactDetails(index)
  }

  private def cyaAddress(index:Int): Call = EstablishersIndividualRoutes.cyaAddressRoute(index, NormalMode)
  private def cyaDetails(index:Int): Call = cyaDetailsRoute(index, NormalMode)
  private def cyaContactDetails(index:Int): Call = cyaContactRoute(index, NormalMode)
  private def addressYears(index:Int, mode:Mode): Call =EstablishersIndividualRoutes.timeAtAddressRoute(index, NormalMode)

  private def addPartners(index: Int, answers: UserAnswers): Call = {
    if (answers.allPartnersAfterDelete(index).isEmpty) {
      controllers.establishers.partnership.partner.routes.PartnerNameController
        .onPageLoad(index, answers.allPartners(index).size, NormalMode)
    } else if (answers.allPartnersAfterDelete(index).length < config.maxPartners) {
      answers.get(AddPartnersId(index)).map { addPartners =>
        if (addPartners) {
          controllers.establishers.partnership.partner.routes.PartnerNameController
            .onPageLoad(index, answers.allPartners(index).size, NormalMode)
        } else {
          controllers.routes.TaskListController.onPageLoad()
        }
      }.getOrElse(controllers.routes.TaskListController.onPageLoad())

    }else {
      controllers.establishers.partnership.routes.OtherPartnersController.onPageLoad(index,NormalMode)
    }
  }
  private def establisherKindRoutes(
                                     index: Index,
                                     ua: UserAnswers
                                   ): Call =
    ua.get(EstablisherKindId(index)) match {
      case Some(EstablisherKind.Individual) => controllers.establishers.individual.routes.EstablisherNameController.onPageLoad(index)
      case Some(EstablisherKind.Company) => CompanyDetailsController.onPageLoad(index)
      case Some(EstablisherKind.Partnership) => PartnershipDetailsController.onPageLoad(index)
      case _ => IndexController.onPageLoad()
    }

  private def addEstablisherRoutes(
                                    value: Option[Boolean],
                                    answers: UserAnswers
                                  ): Call =
    value match {
      case Some(false) => TaskListController.onPageLoad()
      case Some(true) => EstablisherKindController.onPageLoad(answers.establishersCount)
      case None => controllers.routes.TaskListController.onPageLoad()
    }

  private def establisherHasNino(
                                  index: Index,
                                  answers: UserAnswers,
                                  mode: Mode
                                ): Call =
    answers.get(EstablisherHasNINOId(index)) match {
      case Some(true) => controllers.establishers.individual.details.routes.EstablisherEnterNINOController.onPageLoad(index, mode)
      case Some(false) => controllers.establishers.individual.details.routes.EstablisherNoNINOReasonController.onPageLoad(index, mode)
      case None => controllers.routes.TaskListController.onPageLoad()
    }

  private def establisherHasUtr(
                                 index: Index,
                                 answers: UserAnswers,
                                 mode: Mode
                               ): Call =
    answers.get(EstablisherHasUTRId(index)) match {
      case Some(true) => controllers.establishers.individual.details.routes.EstablisherHasUTRController.onPageLoad(index, mode)
      case Some(false) => controllers.establishers.individual.details.routes.EstablisherNoUTRReasonController.onPageLoad(index, mode)
      case None => controllers.routes.TaskListController.onPageLoad()
    }
}
