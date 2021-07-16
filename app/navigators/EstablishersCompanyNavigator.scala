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

import controllers.establishers.company.contact.routes._
import controllers.establishers.routes._
import identifiers._
import identifiers.establishers.individual.EstablisherNameId
import identifiers.establishers.company.contact.{EnterEmailId, EnterPhoneId}
import models.NormalMode
import models.requests.DataRequest
import play.api.mvc.{AnyContent, Call}
import utils.{Enumerable, UserAnswers}

class EstablishersCompanyNavigator
  extends Navigator
    with Enumerable.Implicits {

  //scalastyle:off cyclomatic.complexity
  override protected def routeMap(ua: UserAnswers)
                                 (implicit request: DataRequest[AnyContent]): PartialFunction[Identifier, Call] = {
    case EstablisherNameId(_) => AddEstablisherController.onPageLoad()
    case EnterEmailId(index) => EnterPhoneController.onPageLoad(index, NormalMode)
    case EnterPhoneId(index) => cyaContactDetails(index)
  }

  override protected def editRouteMap(ua: UserAnswers)
                                     (implicit request: DataRequest[AnyContent]): PartialFunction[Identifier, Call] = {
    case EnterEmailId(index) => cyaContactDetails(index)
    case EnterPhoneId(index) => cyaContactDetails(index)
  }

  private def cyaContactDetails(index:Int): Call = controllers.establishers.company.contact.routes.CheckYourAnswersController.onPageLoad(index)

}
