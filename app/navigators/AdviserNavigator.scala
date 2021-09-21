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

import controllers.adviser.routes._
import controllers.routes.IndexController
import identifiers._
import identifiers.adviser._
import models.NormalMode
import models.requests.DataRequest
import play.api.mvc.{AnyContent, Call}
import utils.{Enumerable, UserAnswers}

class AdviserNavigator
  extends Navigator
    with Enumerable.Implicits {

  //scalastyle:off cyclomatic.complexity
  override protected def routeMap(ua: UserAnswers)
                                 (implicit request: DataRequest[AnyContent]): PartialFunction[Identifier, Call] = {
    case AdviserNameId => EnterEmailController.onPageLoad(NormalMode)
    case EnterEmailId => EnterPhoneController.onPageLoad(NormalMode)
    case EnterPhoneId => EnterPostcodeController.onPageLoad(NormalMode)
    case EnterPostCodeId => selectAddress
    case AddressListId => cyaDetails
    case AddressId => cyaDetails
    case _ => IndexController.onPageLoad()
  }

  override protected def editRouteMap(ua: UserAnswers)
                                     (implicit request: DataRequest[AnyContent]): PartialFunction[Identifier, Call] = {
    case AdviserNameId => cyaDetails
    case EnterEmailId => cyaDetails
    case EnterPhoneId => cyaDetails
    case EnterPostCodeId => selectAddress
    case AddressListId => cyaDetails
    case AddressId => cyaDetails
    case _ => IndexController.onPageLoad()
  }

  private def cyaDetails: Call =CheckYourAnswersController.onPageLoad
  private def selectAddress: Call = SelectAddressController.onPageLoad
}

