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
import controllers.establishers.partnership.routes._
import controllers.routes.IndexController
import identifiers._
import identifiers.establishers.partnership.PartnershipDetailsId
import identifiers.establishers.partnership.address._
import models.requests.DataRequest
import models.{Mode, NormalMode}
import play.api.mvc.{AnyContent, Call}
import utils.{Enumerable, UserAnswers}

import javax.inject.Inject

class EstablishersPartnershipNavigator@Inject()(config: AppConfig)
  extends Navigator
    with Enumerable.Implicits {

  //scalastyle:off cyclomatic.complexity
  override protected def routeMap(ua: UserAnswers)
                                 (implicit request: DataRequest[AnyContent]): PartialFunction[Identifier, Call] = {
    case PartnershipDetailsId(index) => PartnershipDetailsController.onPageLoad(index)
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

 override protected def editRouteMap(ua: UserAnswers)                                     (implicit request: DataRequest[AnyContent]): PartialFunction[Identifier, Call] = {
   case PartnershipDetailsId(_) => IndexController.onPageLoad()
  }

  private def cyaAddress(index:Int): Call = controllers.establishers.company.address.routes.CheckYourAnswersController.onPageLoad(index)
  private def addressYears(index:Int, mode:Mode): Call = controllers.establishers.company.address.routes.AddressYearsController.onPageLoad(index)
}
