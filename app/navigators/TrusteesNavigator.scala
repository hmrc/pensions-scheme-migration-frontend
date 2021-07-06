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
import controllers.trustees.individual.contact.routes._
import controllers.trustees.individual.routes._
import controllers.trustees.routes._
import identifiers._
import identifiers.trustees._
import identifiers.trustees.individual.TrusteeNameId
import identifiers.trustees.individual.contact.{EnterEmailId, EnterPhoneId}
import models.requests.DataRequest
import models.trustees.TrusteeKind
import models.{Index, NormalMode}
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
    case EnterEmailId(index) => EnterPhoneController.onPageLoad(index, NormalMode)
    case EnterPhoneId(index) => cyaContactDetails(index)
  }

  override protected def editRouteMap(ua: UserAnswers)
                                     (implicit request: DataRequest[AnyContent]): PartialFunction[Identifier, Call] = {
    case EnterEmailId(index) => cyaContactDetails(index)
    case EnterPhoneId(index) => cyaContactDetails(index)
    case _ => IndexController.onPageLoad()
  }

  private def cyaContactDetails(index:Int): Call = controllers.trustees.individual.contact.routes.CheckYourAnswersController.onPageLoad(index)

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
}
