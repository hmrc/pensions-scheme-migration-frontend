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

import config.AppConfig
import controllers.routes._
import controllers.trustees.individual.address.routes.{EnterPreviousPostcodeController, SelectAddressController, SelectPreviousAddressController}
import controllers.trustees.individual.contact.routes._
import controllers.trustees.individual.details.routes._
import controllers.trustees.individual.routes._
import controllers.trustees.routes._
import identifiers._
import identifiers.trustees.individual.TrusteeNameId
import identifiers.trustees.individual.address._
import identifiers.trustees.individual.contact.{EnterEmailId, EnterPhoneId}
import identifiers.trustees.individual.details._
import identifiers.trustees._
import models.requests.DataRequest
import models.trustees.TrusteeKind
import models.{CheckMode, Index, Mode, NormalMode, entities}
import play.api.mvc.{AnyContent, Call}
import services.DataPrefillService
import utils.{Enumerable, UserAnswers}

import javax.inject.Inject

class TrusteesNavigator @Inject()(config: AppConfig, dataPrefillService: DataPrefillService)
  extends Navigator
    with Enumerable.Implicits {

  //scalastyle:off cyclomatic.complexity
  override protected def routeMap(ua: UserAnswers)
                                 (implicit request: DataRequest[AnyContent]): PartialFunction[Identifier, Call] = {
    case TrusteeKindId(index, kind) => trusteeKindRoutes(index, kind, ua)
    case AnyTrusteesId => anyTrusteesRoutes(ua)
    case TrusteeNameId(index) => controllers.common.routes.SpokeTaskListController.onPageLoad(index, entities.Trustee, entities.Individual)
    case AddTrusteeId(value) => addTrusteeRoutes(value, ua)
    case ConfirmDeleteTrusteeId => AddTrusteeController.onPageLoad
    case TrusteeDOBId(index) => TrusteeHasNINOController.onPageLoad(index, NormalMode)
    case TrusteeHasNINOId(index) => trusteeHasNino(index, ua, NormalMode)
    case TrusteeNINOId(index) => TrusteeHasUTRController.onPageLoad(index, NormalMode)
    case TrusteeNoNINOReasonId(index) => TrusteeHasUTRController.onPageLoad(index, NormalMode)
    case TrusteeHasUTRId(index) => trusteeHasUtr(index, ua, NormalMode)
    case TrusteeUTRId(index) => cyaDetails(index)
    case TrusteeNoUTRReasonId(index) => cyaDetails(index)
    case EnterPostCodeId(index) => SelectAddressController.onPageLoad(index, NormalMode)
    case AddressListId(index) => addressYears(index, NormalMode)
    case AddressId(index) => addressYears(index, NormalMode)
    case AddressYearsId(index) =>
      if (ua.get(AddressYearsId(index)).contains(true)) cyaAddress(index) else EnterPreviousPostcodeController.onPageLoad(index, NormalMode)
    case EnterPreviousPostCodeId(index) => SelectPreviousAddressController.onPageLoad(index, NormalMode)
    case PreviousAddressListId(index) => cyaAddress(index)
    case PreviousAddressId(index) => cyaAddress(index)
    case EnterEmailId(index) => EnterPhoneController.onPageLoad(index, NormalMode)
    case EnterPhoneId(index) => cyaContactDetails(index)
    case OtherTrusteesId => TaskListController.onPageLoad
    case DirectorAlsoTrusteeId(index) => directorAlsoTrusteeRoutes(index, ua)
    case DirectorsAlsoTrusteesId(index) => directorsAlsoTrusteesRoutes(index, ua)
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
    case AddressId(index) => cyaAddress(index)
    case AddressListId(index) => cyaAddress(index)
    case AddressYearsId(index) =>
      if (ua.get(AddressYearsId(index)).contains(true)) cyaAddress(index)
      else EnterPreviousPostcodeController.onPageLoad(index, CheckMode)
    case EnterPostCodeId(index) => SelectAddressController.onPageLoad(index, CheckMode)
    case EnterPreviousPostCodeId(index) => SelectPreviousAddressController.onPageLoad(index, CheckMode)
    case PreviousAddressId(index) => cyaAddress(index)
    case PreviousAddressListId(index) => cyaAddress(index)
    case EnterEmailId(index) => cyaContactDetails(index)
    case EnterPhoneId(index) => cyaContactDetails(index)
  }

  private def cyaDetails(index:Int): Call = controllers.common.routes.CheckYourAnswersController.onPageLoad(index, entities.Trustee, entities.Individual, entities.Details)


  private def cyaAddress(index:Int): Call = controllers.common.routes.CheckYourAnswersController.onPageLoad(index, entities.Trustee, entities.Individual, entities.Address)
  private def addressYears(index:Int, mode:Mode): Call = controllers.trustees.individual.address.routes.AddressYearsController.onPageLoad(index, mode)

  private def cyaContactDetails(index:Int): Call = controllers.common.routes.CheckYourAnswersController.onPageLoad(index, entities.Trustee, entities.Individual, entities.Contacts)

  private def trusteeKindRoutes(
                                 index: Index,
                                 kind: TrusteeKind,
                                 ua: UserAnswers
                               ): Call = {
    val noOfDirectors = dataPrefillService.getListOfDirectorsToBeCopied(ua).size
    kind match {
      case TrusteeKind.Individual if noOfDirectors == 1 =>
        controllers.trustees.individual.routes.DirectorAlsoTrusteeController.onPageLoad(index)
      case TrusteeKind.Individual if noOfDirectors > 1 =>
        controllers.trustees.individual.routes.DirectorsAlsoTrusteesController.onPageLoad(index)
      case TrusteeKind.Individual =>
        TrusteeNameController.onPageLoad(index)
      case TrusteeKind.Company =>
        controllers.trustees.company.routes.CompanyDetailsController.onPageLoad(index)
      case TrusteeKind.Partnership =>
        controllers.trustees.partnership.routes.PartnershipDetailsController.onPageLoad(index)
      case _ => throw new RuntimeException("index page unavailable")
    }
  }

  private def addTrusteeRoutes(
                                value: Option[Boolean],
                                answers: UserAnswers
                              ): Call =
    value match {
      case Some(false) => TaskListController.onPageLoad
      case Some(true) => TrusteeKindController.onPageLoad(answers.trusteesCount)
      case None => controllers.trustees.routes.OtherTrusteesController.onPageLoad
    }

  private def anyTrusteesRoutes(
                                 answers: UserAnswers
                               ): Call = {

    answers.get(AnyTrusteesId) match {
    case Some(false) => TaskListController.onPageLoad
    case Some(true) => TrusteeKindController.onPageLoad(answers.trusteesCount)
    case None => throw new RuntimeException("index page unavailable")
  }
}

  private def trusteeHasNino(
                                  index: Index,
                                  answers: UserAnswers,
                                  mode: Mode
                                ): Call =
    answers.get(TrusteeHasNINOId(index)) match {
      case Some(true) => TrusteeEnterNINOController.onPageLoad(index, mode)
      case Some(false) => TrusteeNoNINOReasonController.onPageLoad(index, mode)
      case None => controllers.routes.TaskListController.onPageLoad
    }

  private def trusteeHasUtr(
                                 index: Index,
                                 answers: UserAnswers,
                                 mode: Mode
                               ): Call =
    answers.get(TrusteeHasUTRId(index)) match {
      case Some(true) => TrusteeEnterUTRController.onPageLoad(index, mode)
      case Some(false) => TrusteeNoUTRReasonController.onPageLoad(index, mode)
      case None => controllers.routes.TaskListController.onPageLoad
    }

  private def directorAlsoTrusteeRoutes(index: Index, answers: UserAnswers): Call = {
    val noneValue = -1
    answers.get(DirectorAlsoTrusteeId(index)) match {
      case Some(value) if value == noneValue =>
        controllers.trustees.individual.routes.TrusteeNameController.onPageLoad(index)
      case _ =>
        AddTrusteeController.onPageLoad
    }
  }

  private def directorsAlsoTrusteesRoutes(index: Index, answers: UserAnswers): Call = {
    val noneValue = -1
    answers.get(DirectorsAlsoTrusteesId(index)) match {
      case Some(value) if value.size == 1 && value.contains(noneValue) =>
        controllers.trustees.individual.routes.TrusteeNameController.onPageLoad(index)
      case _ =>
        AddTrusteeController.onPageLoad
    }
  }
}
