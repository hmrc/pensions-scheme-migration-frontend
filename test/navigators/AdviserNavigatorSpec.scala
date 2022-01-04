/*
 * Copyright 2022 HM Revenue & Customs
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
import controllers.adviser.routes._
import identifiers.adviser.{AddressId, AddressListId, AdviserNameId, EnterEmailId, EnterPhoneId, EnterPostCodeId}
import identifiers.{Identifier, TypedIdentifier}
import models.{CheckMode, Mode, NormalMode, _}
import org.scalatest.TryValues
import org.scalatest.prop.TableFor3
import play.api.libs.json.Writes
import play.api.mvc.Call
import utils.Data.ua
import utils.{Data, Enumerable, UserAnswers}


class AdviserNavigatorSpec
  extends SpecBase
    with NavigatorBehaviour
    with Enumerable.Implicits
    with TryValues {

  private val navigator: CompoundNavigator = injector.instanceOf[CompoundNavigator]
  private def cya: Call =CheckYourAnswersController.onPageLoad
  private def selectAddress: Call = SelectAddressController.onPageLoad


  private val detailsUa: UserAnswers =
    ua.set(AdviserNameId, "test").success.value

  private def addressUAWithValue[A](idType:TypedIdentifier[A], idValue:A)(implicit writes: Writes[A]) =
    detailsUa.set(idType, idValue).toOption

  private val seqAddresses = Seq(
    TolerantAddress(Some("1"),Some("1"),Some("c"),Some("d"), Some("zz11zz"), Some("GB")),
    TolerantAddress(Some("2"),Some("2"),Some("c"),Some("d"), Some("zz11zz"), Some("GB")),
  )

  val address = Address("addr1", "addr2", None, None, Some("ZZ11ZZ"), "GB")

  private def postcode(mode: Mode): Call = EnterPostcodeController.onPageLoad(mode)

  private def enterPhonePage(mode:Mode): Call =
    EnterPhoneController.onPageLoad(mode)

  private def enterEmailPage(mode:Mode): Call =
    EnterEmailController.onPageLoad(mode)


  "AdviserNavigator" when {
    def navigation: TableFor3[Identifier, UserAnswers, Call] =
      Table(
        ("Id", "Next Page", "UserAnswers (Optional)"),
        row(AdviserNameId)(enterEmailPage(NormalMode)),
        row(EnterEmailId)(enterPhonePage(NormalMode), Some(detailsUa.set(EnterEmailId, "test@test.com").success.value)),
        row(EnterPhoneId)(postcode(NormalMode), Some(detailsUa.set(EnterPhoneId, "1234").success.value)),
        row(EnterPostCodeId)(selectAddress, addressUAWithValue(EnterPostCodeId, seqAddresses)),
        row(AddressListId)(cya, addressUAWithValue(AddressListId, Data.tolerantAddress)),
        row(AddressId)(cya, addressUAWithValue(AddressId, address))
      )

    def editNavigation: TableFor3[Identifier, UserAnswers, Call] =
      Table(
        ("Id", "Next Page", "UserAnswers (Optional)"),
        row(AdviserNameId)(cya, Some(detailsUa.set(AdviserNameId,"test").success.value)),
        row(EnterEmailId)(cya, Some(detailsUa.set(EnterEmailId, "test@test.com").success.value)),
        row(EnterPhoneId)(cya, Some(detailsUa.set(EnterPhoneId, "1234").success.value)),
        row(EnterPostCodeId)(selectAddress, addressUAWithValue(EnterPostCodeId, seqAddresses)),
        row(AddressId)(cya, addressUAWithValue(AddressId, address)),
        row(AddressListId)(cya, addressUAWithValue(AddressListId, Data.tolerantAddress))
      )

    "in NormalMode" must {
      behave like navigatorWithRoutesForMode(NormalMode)(navigator, navigation)
    }

    "CheckMode" must {
      behave like navigatorWithRoutesForMode(CheckMode)(navigator, editNavigation)
    }
  }
}
