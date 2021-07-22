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
import identifiers.{Identifier, TypedIdentifier}
import identifiers.establishers.company.CompanyDetailsId
import identifiers.establishers.company.address._
import identifiers.establishers.company.contact.{EnterEmailId, EnterPhoneId}
import models._
import org.scalatest.TryValues
import org.scalatest.prop.TableFor3
import play.api.libs.json.Writes
import play.api.mvc.Call
import utils.Data.{establisherCompanyDetails, ua}
import utils.{Enumerable, UserAnswers}

class EstablishersCompanyNavigatorSpec
  extends SpecBase
    with NavigatorBehaviour
    with Enumerable.Implicits
    with TryValues {

  private val navigator: CompoundNavigator = injector.instanceOf[CompoundNavigator]
  private val index: Index = Index(0)

  private val addEstablisherPage: Call = controllers.establishers.routes.AddEstablisherController.onPageLoad()
  private val detailsUa: UserAnswers =
    ua.set(CompanyDetailsId(0), establisherCompanyDetails).success.value
  private def addressUAWithValue[A](idType:TypedIdentifier[A], idValue:A)(implicit writes: Writes[A]) =
    detailsUa.set(idType, idValue).toOption

  private val seqAddresses = Seq(
    TolerantAddress(Some("1"),Some("1"),Some("c"),Some("d"), Some("zz11zz"), Some("GB")),
    TolerantAddress(Some("2"),Some("2"),Some("c"),Some("d"), Some("zz11zz"), Some("GB")),
  )

  val address = Address("addr1", "addr2", None, None, Some("ZZ11ZZ"), "GB")

  private val cyaAddress: Call =
    controllers.establishers.company.address.routes.CheckYourAnswersController.onPageLoad(index)

  private def enterPreviousPostcode: Call =
    controllers.establishers.company.address.routes.EnterPreviousPostcodeController.onPageLoad(index)

  private def tradingTime: Call =
    controllers.establishers.company.address.routes.TradingTimeController.onPageLoad(index)

  private def selectAddress: Call =
    controllers.establishers.company.address.routes.SelectAddressController.onPageLoad(index)

  private def selectPreviousAddress: Call =
    controllers.establishers.company.address.routes.SelectPreviousAddressController.onPageLoad(index)

  private def addressYears: Call =
    controllers.establishers.company.address.routes.AddressYearsController.onPageLoad(index)

  private def enterPhonePage(mode:Mode): Call =
    controllers.establishers.company.contact.routes.EnterPhoneController.onPageLoad(index, mode)

  private val cyaContact: Call =
    controllers.establishers.company.contact.routes.CheckYourAnswersController.onPageLoad(index)

  "EstablishersCompanyNavigator" when {
    def navigation: TableFor3[Identifier, UserAnswers, Call] =
      Table(
        ("Id", "Next Page", "UserAnswers (Optional)"),
        row(CompanyDetailsId(index))(addEstablisherPage),
        row(EnterPostCodeId(index))(selectAddress, addressUAWithValue(EnterPostCodeId(index), seqAddresses)),
        row(AddressListId(index))(addressYears, addressUAWithValue(AddressListId(index), 0)),
        row(AddressId(index))(addressYears, addressUAWithValue(AddressId(index), address)),

        row(AddressYearsId(index))(cyaAddress, addressUAWithValue(AddressYearsId(index), true)),
        row(AddressYearsId(index))(tradingTime, addressUAWithValue(AddressYearsId(index), false)),

        row(TradingTimeId(index))(cyaAddress, addressUAWithValue(TradingTimeId(index), false)),
        row(TradingTimeId(index))(enterPreviousPostcode, addressUAWithValue(TradingTimeId(index), true)),

        row(EnterPreviousPostCodeId(index))(selectPreviousAddress, addressUAWithValue(EnterPreviousPostCodeId(index), seqAddresses)),
        row(PreviousAddressListId(index))(cyaAddress, addressUAWithValue(PreviousAddressListId(index), 0)),
        row(PreviousAddressId(index))(cyaAddress, addressUAWithValue(PreviousAddressId(index), address)),
        row(EnterEmailId(index))(enterPhonePage(NormalMode), Some(detailsUa.set(EnterEmailId(index), "test@test.com").success.value)),
        row(EnterPhoneId(index))(cyaContact, Some(detailsUa.set(EnterPhoneId(index), "1234").success.value))
      )

    def editNavigation: TableFor3[Identifier, UserAnswers, Call] =
      Table(
        ("Id", "Next Page", "UserAnswers (Optional)"),
        row(CompanyDetailsId(index))(controllers.routes.IndexController.onPageLoad()) ,
        row(EnterEmailId(index))(cyaContact, Some(detailsUa.set(EnterEmailId(index), "test@test.com").success.value)),
        row(EnterPhoneId(index))(cyaContact, Some(detailsUa.set(EnterPhoneId(index), "1234").success.value))
      )

    "in NormalMode" must {
      behave like navigatorWithRoutesForMode(NormalMode)(navigator, navigation)
    }

    "CheckMode" must {
      behave like navigatorWithRoutesForMode(CheckMode)(navigator, editNavigation)
    }
  }
}
