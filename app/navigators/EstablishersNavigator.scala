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

import controllers.establishers.company.routes.CompanyDetailsController
import controllers.establishers.individual.address.routes._
import controllers.establishers.individual.contact.routes._
import controllers.establishers.individual.details.routes._
import controllers.establishers.individual.routes._
import controllers.establishers.routes._
import controllers.routes._
import identifiers._
import identifiers.establishers._
import identifiers.establishers.individual.EstablisherNameId
import identifiers.establishers.individual.address._
import identifiers.establishers.individual.contact.{EnterEmailId, EnterPhoneId}
import identifiers.establishers.individual.details._
import models.establishers.EstablisherKind
import models.requests.DataRequest
import models.{CheckMode, Index, Mode, NormalMode}
import play.api.mvc.{AnyContent, Call}
import utils.{Enumerable, UserAnswers}

class EstablishersNavigator
  extends Navigator
    with Enumerable.Implicits {

  //scalastyle:off cyclomatic.complexity
  override protected def routeMap(ua: UserAnswers)
                                 (implicit request: DataRequest[AnyContent]): PartialFunction[Identifier, Call] = {
    case EstablisherKindId(index) => establisherKindRoutes(index, ua)
    case EstablisherNameId(_) => AddEstablisherController.onPageLoad()
    case AddEstablisherId(value) => addEstablisherRoutes(value, ua)
    case ConfirmDeleteEstablisherId => AddEstablisherController.onPageLoad()
    case EstablisherDOBId(index) => EstablisherHasNINOController.onPageLoad(index, NormalMode)
    case EstablisherHasNINOId(index) => establisherHasNino(index, ua, NormalMode)
    case EstablisherNINOId(index) => EstablisherHasUTRController.onPageLoad(index, NormalMode)
    case EstablisherNoNINOReasonId(index) => EstablisherHasUTRController.onPageLoad(index, NormalMode)
    case EstablisherHasUTRId(index) => establisherHasUtr(index, ua, NormalMode)
    case EstablisherUTRId(index) => cyaDetails(index)
    case EstablisherNoUTRReasonId(index) => cyaDetails(index)
    case EnterPostCodeId(index) => SelectAddressController.onPageLoad(index)
    case AddressListId(index) => addressYears(index, NormalMode)
    case AddressId(index) => addressYears(index, NormalMode)
    case AddressYearsId(index) =>
      if (ua.get(AddressYearsId(index)).contains(true)) cyaAddress(index) else EnterPreviousPostcodeController.onPageLoad(index)
    case EnterPreviousPostCodeId(index) => SelectPreviousAddressController.onPageLoad(index)
    case PreviousAddressListId(index) => cyaAddress(index)
    case PreviousAddressId(index) => cyaAddress(index)
    case EnterEmailId(index) => EnterPhoneController.onPageLoad(index, NormalMode)
    case EnterPhoneId(index) => cyaContactDetails(index)
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

  private def cyaAddress(index:Int): Call = controllers.establishers.individual.address.routes.CheckYourAnswersController.onPageLoad(index)
  private def cyaDetails(index:Int): Call = controllers.establishers.individual.details.routes.CheckYourAnswersController.onPageLoad(index)
  private def cyaContactDetails(index:Int): Call = controllers.establishers.individual.contact.routes.CheckYourAnswersController.onPageLoad(index)
  private def addressYears(index:Int, mode:Mode): Call = controllers.establishers.individual.address.routes.AddressYearsController.onPageLoad(index)

  private def establisherKindRoutes(
                                     index: Index,
                                     ua: UserAnswers
                                   ): Call =
    ua.get(EstablisherKindId(index)) match {
      case Some(EstablisherKind.Individual) => EstablisherNameController.onPageLoad(index)
      case Some(EstablisherKind.Company) => CompanyDetailsController.onPageLoad(index)
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
      case Some(true) => EstablisherEnterNINOController.onPageLoad(index, mode)
      case Some(false) => EstablisherNoNINOReasonController.onPageLoad(index, mode)
      case None => controllers.routes.TaskListController.onPageLoad()
    }

  private def establisherHasUtr(
                                 index: Index,
                                 answers: UserAnswers,
                                 mode: Mode
                               ): Call =
    answers.get(EstablisherHasUTRId(index)) match {
      case Some(true) => EstablisherEnterUTRController.onPageLoad(index, mode)
      case Some(false) => EstablisherNoUTRReasonController.onPageLoad(index, mode)
      case None => controllers.routes.TaskListController.onPageLoad()
    }
}
