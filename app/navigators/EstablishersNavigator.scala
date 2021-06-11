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

import controllers.establishers.individual.routes._
import controllers.establishers.routes._
import controllers.routes._
import identifiers._
import identifiers.establishers.individual.EstablisherNameId
import identifiers.establishers.{AddEstablisherId, ConfirmDeleteEstablisherId, EstablisherKindId}
import models.Index
import models.establishers.EstablisherKind
import models.requests.DataRequest
import play.api.mvc.{AnyContent, Call}
import utils.{Enumerable, UserAnswers}

class EstablishersNavigator extends Navigator with Enumerable.Implicits {

  override protected def routeMap(ua: UserAnswers)
                                 (implicit request: DataRequest[AnyContent]): PartialFunction[Identifier, Call] = {
    case EstablisherKindId(index) => establisherKindRoutes(index, ua)
    case EstablisherNameId(_) => AddEstablisherController.onPageLoad()
    case AddEstablisherId(value) => addEstablisherRoutes(value, ua)
    case ConfirmDeleteEstablisherId => AddEstablisherController.onPageLoad()
  }

  private def establisherKindRoutes(index: Index, ua: UserAnswers): Call =
    ua.get(EstablisherKindId(index)) match {
      case Some(EstablisherKind.Individual) => EstablisherNameController.onPageLoad(index)
      case _ => IndexController.onPageLoad()
    }

  private def addEstablisherRoutes(value: Option[Boolean],
                                   answers: UserAnswers): Call = {
    value match {
      case Some(false) => TaskListController.onPageLoad()
      case Some(true) => EstablisherKindController.onPageLoad(answers.establishersCount)
      case None => IndexController.onPageLoad()
    }
  }
}
