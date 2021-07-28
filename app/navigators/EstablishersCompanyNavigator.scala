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
import controllers.establishers.company.address.routes._
import controllers.establishers.routes._
import controllers.routes._
import identifiers._
import identifiers.establishers.company.address._
import identifiers.establishers.company.{AddCompanyDirectorsId, CompanyDetailsId}
import models.requests.DataRequest
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
  }

  private def cyaAddress(index:Int): Call = controllers.establishers.company.address.routes.CheckYourAnswersController.onPageLoad(index)
  private def addressYears(index:Int, mode:Mode): Call = controllers.establishers.company.address.routes.AddressYearsController.onPageLoad(index)
  private def addDirectors(index: Int, answers: UserAnswers): Call = {
    if (answers.allDirectorsAfterDelete(index).isEmpty) {
      controllers.establishers.company.director.routes.DirectorNameController
        .onPageLoad(index, answers.allDirectors(index).size)
    } else if (answers.allDirectorsAfterDelete(index).length < config.maxDirectors) {
      answers.get(AddCompanyDirectorsId(index)).map { addCompanyDirectors =>
        if (addCompanyDirectors) {
          controllers.establishers.company.director.routes.DirectorNameController
            .onPageLoad(index, answers.allDirectors(index).size)
        } else {
          controllers.routes.TaskListController.onPageLoad()
        }
      }.getOrElse(controllers.routes.TaskListController.onPageLoad())

    }else {
      controllers.establishers.company.routes.OtherDirectorsController.onPageLoad(index,NormalMode)
    }
  }

}
