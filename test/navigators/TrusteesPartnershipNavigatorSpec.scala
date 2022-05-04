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
import controllers.trustees.partnership.details.{routes => detailsRoutes}
import identifiers.trustees.partnership.PartnershipDetailsId
import identifiers.trustees.partnership.address._
import identifiers.trustees.partnership.details._
import identifiers.{Identifier, TypedIdentifier}
import models._
import org.scalatest.TryValues
import org.scalatest.prop.TableFor3
import play.api.libs.json.Writes
import play.api.mvc.Call
import utils.Data.{partnershipDetails, ua}
import utils.{Data, Enumerable, UserAnswers}


class TrusteesPartnershipNavigatorSpec extends SpecBase with NavigatorBehaviour with Enumerable.Implicits with TryValues {

  private val navigator: CompoundNavigator = injector.instanceOf[CompoundNavigator]
  private val index: Index = Index(0)
  private val detailsUa: UserAnswers =
    ua.set(PartnershipDetailsId(0), partnershipDetails).success.value
  private def uaWithValue[A](idType:TypedIdentifier[A], idValue:A)(implicit writes: Writes[A]) =
    detailsUa.set(idType, idValue).toOption

  private def utr(mode: Mode = NormalMode): Call = detailsRoutes.UTRController.onPageLoad(index, mode)
  private def noUtr(mode: Mode = NormalMode): Call = detailsRoutes.NoUTRReasonController.onPageLoad(index, mode)
  private def haveVat(mode: Mode = NormalMode): Call = detailsRoutes.HaveVATController.onPageLoad(index, mode)
  private def vat(mode: Mode = NormalMode): Call = detailsRoutes.VATController.onPageLoad(index, mode)
  private def havePaye(mode: Mode = NormalMode): Call = detailsRoutes.HavePAYEController.onPageLoad(index, mode)
  private def paye(mode: Mode = NormalMode): Call = detailsRoutes.PAYEController.onPageLoad(index, mode)
  private val cyaDetails: Call = detailsRoutes.CheckYourAnswersController.onPageLoad(index)


  private val addTrusteeDetailsPage: Call = controllers.trustees.partnership.routes.SpokeTaskListController.onPageLoad(index)

  private def addressUAWithValue[A](idType:TypedIdentifier[A], idValue:A)(implicit writes: Writes[A]) =
    detailsUa.set(idType, idValue).toOption
  val address = Address("addr1", "addr2", None, None, Some("ZZ11ZZ"), "GB")

  private val seqAddresses = Seq(
    TolerantAddress(Some("1"),Some("1"),Some("c"),Some("d"), Some("zz11zz"), Some("GB")),
    TolerantAddress(Some("2"),Some("2"),Some("c"),Some("d"), Some("zz11zz"), Some("GB")),
  )

  private def enterPreviousPostcode(mode:Mode): Call =
    controllers.trustees.partnership.address.routes.EnterPreviousPostcodeController.onPageLoad(index,mode)

  private def selectAddress(mode:Mode): Call =
    controllers.trustees.partnership.address.routes.SelectAddressController.onPageLoad(index,mode)

  private def selectPreviousAddress(mode:Mode): Call =
    controllers.trustees.partnership.address.routes.SelectPreviousAddressController.onPageLoad(index,mode)

  private def addressYears(mode:Mode): Call =
    controllers.trustees.partnership.address.routes.AddressYearsController.onPageLoad(index,mode)

  private def tradingTime(mode:Mode): Call = controllers.trustees.partnership.address.routes.TradingTimeController.onPageLoad(index,mode)

  private val cyaAddress: Call =
    controllers.trustees.partnership.address.routes.CheckYourAnswersController.onPageLoad(index)

  "TrusteesPartnershipNavigator" when {
    def navigation: TableFor3[Identifier, UserAnswers, Call] =
      Table(
        ("Id", "Next Page", "UserAnswers (Optional)"),
        row(PartnershipDetailsId(index))(addTrusteeDetailsPage),
        row(HaveUTRId(index))(utr(), uaWithValue(HaveUTRId(index), true)),
        row(HaveUTRId(index))(noUtr(), uaWithValue(HaveUTRId(index), false)),
        row(PartnershipUTRId(index))(haveVat()),
        row(NoUTRReasonId(index))(haveVat()),
        row(HaveVATId(index))(vat(), uaWithValue(HaveVATId(index), true)),
        row(HaveVATId(index))(havePaye(), uaWithValue(HaveVATId(index), false)),
        row(VATId(index))(havePaye()),
        row(HavePAYEId(index))(paye(), uaWithValue(HavePAYEId(index), true)),
        row(HavePAYEId(index))(cyaDetails, uaWithValue(HavePAYEId(index), false)),
        row(PAYEId(index))(cyaDetails),
        row(EnterPostCodeId(index))(selectAddress(NormalMode), addressUAWithValue(EnterPostCodeId(index), seqAddresses)),
        row(AddressListId(index))(addressYears(NormalMode), addressUAWithValue(AddressListId(index), Data.tolerantAddress)),
        row(AddressId(index))(addressYears(NormalMode), addressUAWithValue(AddressId(index), address)),

        row(AddressYearsId(index))(cyaAddress, uaWithValue(AddressYearsId(index), true)),
        row(AddressYearsId(index))(tradingTime(NormalMode), uaWithValue(AddressYearsId(index), false)),

        row(TradingTimeId(index))(cyaAddress, uaWithValue(TradingTimeId(index), false)),
        row(TradingTimeId(index))(enterPreviousPostcode(NormalMode), uaWithValue(TradingTimeId(index), true)),

        row(EnterPreviousPostCodeId(index))(selectPreviousAddress(NormalMode), addressUAWithValue(EnterPreviousPostCodeId(index), seqAddresses)),
        row(PreviousAddressListId(index))(cyaAddress, addressUAWithValue(PreviousAddressListId(index), Data.tolerantAddress)),
        row(PreviousAddressId(index))(cyaAddress, addressUAWithValue(PreviousAddressId(index), address)),
      )

    def editNavigation: TableFor3[Identifier, UserAnswers, Call] =
      Table(
        ("Id", "Next Page", "UserAnswers (Optional)"),
        row(HaveUTRId(index))(utr(CheckMode), uaWithValue(HaveUTRId(index), true)),
        row(HaveUTRId(index))(noUtr(CheckMode), uaWithValue(HaveUTRId(index), false)),
        row(PartnershipUTRId(index))(cyaDetails),
        row(NoUTRReasonId(index))(cyaDetails),
        row(HaveVATId(index))(vat(CheckMode), uaWithValue(HaveVATId(index), true)),
        row(HaveVATId(index))(cyaDetails, uaWithValue(HaveVATId(index), false)),
        row(VATId(index))(cyaDetails),
        row(HavePAYEId(index))(paye(CheckMode), uaWithValue(HavePAYEId(index), true)),
        row(HavePAYEId(index))(cyaDetails, uaWithValue(HavePAYEId(index), false)),
        row(PAYEId(index))(cyaDetails),
        row(EnterPostCodeId(index))(selectAddress(CheckMode), addressUAWithValue(EnterPostCodeId(index), seqAddresses)),
        row(AddressListId(index))(cyaAddress, addressUAWithValue(AddressListId(index), Data.tolerantAddress)),
        row(AddressId(index))(cyaAddress, uaWithValue(AddressYearsId(index), true)),
        row(AddressYearsId(index))(cyaAddress, uaWithValue(AddressYearsId(index), true)),
        row(AddressYearsId(index))(enterPreviousPostcode(CheckMode),uaWithValue(AddressYearsId(index), false)),
        row(EnterPreviousPostCodeId(index))(selectPreviousAddress(CheckMode),addressUAWithValue(EnterPreviousPostCodeId(index), seqAddresses)),
        row(PreviousAddressListId(index))(cyaAddress, addressUAWithValue(PreviousAddressListId(index), Data.tolerantAddress)),
        row(PreviousAddressId(index))(cyaAddress, addressUAWithValue(PreviousAddressId(index), address))
      )

    "in NormalMode" must {
      behave like navigatorWithRoutesForMode(NormalMode)(navigator, navigation)
    }

    "CheckMode" must {
      behave like navigatorWithRoutesForMode(CheckMode)(navigator, editNavigation)
    }
  }
}
