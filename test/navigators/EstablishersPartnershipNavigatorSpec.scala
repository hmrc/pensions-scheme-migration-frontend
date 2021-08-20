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

import base.SpecBase
import controllers.establishers.partnership.address.{routes => addressRoutes}
import identifiers.establishers.partnership.PartnershipDetailsId
import identifiers.establishers.partnership.address._
import identifiers.{Identifier, TypedIdentifier}
import models._
import org.scalatest.TryValues
import org.scalatest.prop.TableFor3
import play.api.libs.json.Writes
import play.api.mvc.Call
import utils.Data.{establisherPartnershipDetails, ua}
import utils.{Enumerable, UserAnswers}

class EstablishersPartnershipNavigatorSpec
  extends SpecBase
    with NavigatorBehaviour
    with Enumerable.Implicits
    with TryValues {

  private val navigator: CompoundNavigator = injector.instanceOf[CompoundNavigator]
  private val index: Index = Index(0)
  private val addEstablisherPage: Call = controllers.establishers.routes.AddEstablisherController.onPageLoad()
  private val detailsUa: UserAnswers =
    ua.set(PartnershipDetailsId(0), establisherPartnershipDetails).success.value
  private def uaWithValue[A](idType:TypedIdentifier[A], idValue:A)(implicit writes: Writes[A]) =
    detailsUa.set(idType, idValue).toOption


  private val seqAddresses = Seq(
    TolerantAddress(Some("1"),Some("1"),Some("c"),Some("d"), Some("zz11zz"), Some("GB")),
    TolerantAddress(Some("2"),Some("2"),Some("c"),Some("d"), Some("zz11zz"), Some("GB")),
  )

  val address: Address = Address("addr1", "addr2", None, None, Some("ZZ11ZZ"), "GB")

  private val cyaAddress: Call = addressRoutes.CheckYourAnswersController.onPageLoad(index)

  private def enterPreviousPostcode: Call = addressRoutes.EnterPreviousPostcodeController.onPageLoad(index)

  private def tradingTime: Call = addressRoutes.TradingTimeController.onPageLoad(index)

  private def selectAddress: Call = addressRoutes.SelectAddressController.onPageLoad(index)

  private def selectPreviousAddress: Call = addressRoutes.SelectPreviousAddressController.onPageLoad(index)

  private def addressYears: Call = addressRoutes.AddressYearsController.onPageLoad(index)


  "EstablishersPartnershipNavigator" when {
    def navigation: TableFor3[Identifier, UserAnswers, Call] =
      Table(
        ("Id", "Next Page", "UserAnswers (Optional)"),
         row(PartnershipDetailsId(index))(addEstablisherPage),
        row(EnterPostCodeId(index))(selectAddress, uaWithValue(EnterPostCodeId(index), seqAddresses)),
        row(AddressListId(index))(addressYears, uaWithValue(AddressListId(index), 0)),
        row(AddressId(index))(addressYears, uaWithValue(AddressId(index), address)),

        row(AddressYearsId(index))(cyaAddress, uaWithValue(AddressYearsId(index), true)),
        row(AddressYearsId(index))(tradingTime, uaWithValue(AddressYearsId(index), false)),

        row(TradingTimeId(index))(cyaAddress, uaWithValue(TradingTimeId(index), false)),
        row(TradingTimeId(index))(enterPreviousPostcode, uaWithValue(TradingTimeId(index), true)),

        row(EnterPreviousPostCodeId(index))(selectPreviousAddress, uaWithValue(EnterPreviousPostCodeId(index), seqAddresses)),
        row(PreviousAddressListId(index))(cyaAddress, uaWithValue(PreviousAddressListId(index), 0)),
        row(PreviousAddressId(index))(cyaAddress, uaWithValue(PreviousAddressId(index), address)),

      )

    def editNavigation: TableFor3[Identifier, UserAnswers, Call] =
      Table(
        ("Id", "Next Page", "UserAnswers (Optional)"),
        row(PartnershipDetailsId(index))(controllers.routes.IndexController.onPageLoad()),
      )

    "in NormalMode" must {
      behave like navigatorWithRoutesForMode(NormalMode)(navigator, navigation)
    }

    "CheckMode" must {
      behave like navigatorWithRoutesForMode(CheckMode)(navigator, editNavigation)
    }
  }
}
