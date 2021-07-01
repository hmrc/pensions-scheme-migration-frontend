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
import controllers.trustees.individual.details.routes._
import controllers.trustees.individual.routes._
import controllers.trustees.routes._
import identifiers._
import identifiers.establishers.individual.details._
import identifiers.trustees._
import identifiers.trustees.individual.TrusteeNameId
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
    case TrusteeNameId(_) => AddTrusteeController.onPageLoad()
    case AddTrusteeId(value) => addTrusteeRoutes(value, ua)
    case ConfirmDeleteTrusteeId => AddTrusteeController.onPageLoad()
    case TrusteeDOBId(index) => TrusteeHasNINOController.onPageLoad(index, NormalMode)
    case TrusteeHasNINOId(index) => trusteeHasNino(index, ua, NormalMode)
    case TrusteeNINOId(index) => TrusteeHasUTRController.onPageLoad(index, NormalMode)
    case TrusteeNoNINOReasonId(index) => TrusteeHasUTRController.onPageLoad(index, NormalMode)
    case TrusteeHasUTRId(index) => trusteeHasUtr(index, ua, NormalMode)
    case TrusteeUTRId(index) => cyaDetails(index)
    case TrusteeNoUTRReasonId(index) => cyaDetails(index)
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
  }

  private def cyaDetails(index:Int): Call = controllers.trustees.individual.details.routes.CheckYourAnswersController.onPageLoad(index)

  private def trusteeKindRoutes(
                                     index: Index,
                                     ua: UserAnswers
                                   ): Call =
    ua.get(TrusteeKindId(index)) match {
      case Some(TrusteeKind.Individual) => TrusteeNameController.onPageLoad(index)
      case _ => IndexController.onPageLoad()
    }

  private def addTrusteeRoutes(
                                    value: Option[Boolean],
                                    answers: UserAnswers
                                  ): Call =
    value match {
      case Some(false) => TaskListController.onPageLoad()
      case Some(true) => TrusteeKindController.onPageLoad(answers.trusteesCount)
      case None => IndexController.onPageLoad()
    }

  private def trusteeHasNino(
                                  index: Index,
                                  answers: UserAnswers,
                                  mode: Mode
                                ): Call =
    answers.get(TrusteeHasNINOId(index)) match {
      case Some(true) => TrusteeEnterNINOController.onPageLoad(index, mode)
      case Some(false) => TrusteeNoNINOReasonController.onPageLoad(index, mode)
      case None => controllers.routes.TaskListController.onPageLoad()
    }

  private def trusteeHasUtr(
                                 index: Index,
                                 answers: UserAnswers,
                                 mode: Mode
                               ): Call =
    answers.get(TrusteeHasUTRId(index)) match {
      case Some(true) => TrusteeEnterUTRController.onPageLoad(index, mode)
      case Some(false) => TrusteeNoUTRReasonController.onPageLoad(index, mode)
      case None => controllers.routes.TaskListController.onPageLoad()
    }
}
