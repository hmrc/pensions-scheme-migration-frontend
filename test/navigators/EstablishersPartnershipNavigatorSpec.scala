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
import controllers.establishers.partnership.address.{routes => addressRoutes}
import controllers.establishers.partnership.details.{routes => detailsRoutes}
import identifiers.establishers.partnership.PartnershipDetailsId
import identifiers.establishers.partnership.address._
import identifiers.establishers.partnership.contact.{EnterEmailId, EnterPhoneId}
import identifiers.establishers.partnership.details._
import identifiers.{Identifier, TypedIdentifier}
import models._
import org.scalatest.TryValues
import org.scalatest.prop.TableFor3
import play.api.libs.json.Writes
import play.api.mvc.Call
import utils.Data.{partnershipDetails, ua}
import utils.{Data, Enumerable, UserAnswers}

class EstablishersPartnershipNavigatorSpec
  extends SpecBase
    with NavigatorBehaviour
    with Enumerable.Implicits
    with TryValues {

  private val navigator: CompoundNavigator = injector.instanceOf[CompoundNavigator]
  private val index: Index = Index(0)
  private val addEstablisherDetailsPage: Call = controllers.establishers.partnership.routes.SpokeTaskListController.onPageLoad(index)
  private val detailsUa: UserAnswers =
    ua.set(PartnershipDetailsId(0), partnershipDetails).success.value
  private def uaWithValue[A](idType:TypedIdentifier[A], idValue:A)(implicit writes: Writes[A]) =
    detailsUa.set(idType, idValue).toOption

  private val seqAddresses = Seq(
    TolerantAddress(Some("1"),Some("1"),Some("c"),Some("d"), Some("zz11zz"), Some("GB")),
    TolerantAddress(Some("2"),Some("2"),Some("c"),Some("d"), Some("zz11zz"), Some("GB")),
  )

  val address: Address = Address("addr1", "addr2", None, None, Some("ZZ11ZZ"), "GB")

  private def utr(mode: Mode = NormalMode): Call = detailsRoutes.UTRController.onPageLoad(index, mode)
  private def noUtr(mode: Mode = NormalMode): Call = detailsRoutes.NoUTRReasonController.onPageLoad(index, mode)
  private def haveVat(mode: Mode = NormalMode): Call = detailsRoutes.HaveVATController.onPageLoad(index, mode)
  private def vat(mode: Mode = NormalMode): Call = detailsRoutes.VATController.onPageLoad(index, mode)
  private def havePaye(mode: Mode = NormalMode): Call = detailsRoutes.HavePAYEController.onPageLoad(index, mode)
  private def paye(mode: Mode = NormalMode): Call = detailsRoutes.PAYEController.onPageLoad(index, mode)
  private val cyaDetails: Call = detailsRoutes.CheckYourAnswersController.onPageLoad(index)

  private val cyaAddress: Call = addressRoutes.CheckYourAnswersController.onPageLoad(index)

  private def enterPreviousPostcode(mode:Mode): Call = addressRoutes.EnterPreviousPostcodeController.onPageLoad(index,mode)

  private def tradingTime(mode:Mode): Call = addressRoutes.TradingTimeController.onPageLoad(index,mode)

  private def selectAddress(mode:Mode): Call = addressRoutes.SelectAddressController.onPageLoad(index,mode)

  private def selectPreviousAddress(mode:Mode): Call = addressRoutes.SelectPreviousAddressController.onPageLoad(index,mode)

  private def addressYears(mode:Mode): Call = addressRoutes.AddressYearsController.onPageLoad(index,mode)


  "EstablishersPartnershipNavigator" when {
    def navigation: TableFor3[Identifier, UserAnswers, Call] =
      Table(
        ("Id", "Next Page", "UserAnswers (Optional)"),
        row(PartnershipDetailsId(index))(addEstablisherDetailsPage),

        row(EnterEmailId(index))(controllers.establishers.partnership.contact.routes.EnterPhoneController.onPageLoad(index, NormalMode)),
        row(EnterPhoneId(index))(controllers.establishers.partnership.contact.routes.CheckYourAnswersController.onPageLoad(index)),

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
        row(EnterPostCodeId(index))(selectAddress(NormalMode), uaWithValue(EnterPostCodeId(index), seqAddresses)),
        row(AddressListId(index))(addressYears(NormalMode), uaWithValue(AddressListId(index), Data.tolerantAddress)),
        row(AddressId(index))(addressYears(NormalMode), uaWithValue(AddressId(index), address)),

        row(AddressYearsId(index))(cyaAddress, uaWithValue(AddressYearsId(index), true)),
        row(AddressYearsId(index))(tradingTime(NormalMode), uaWithValue(AddressYearsId(index), false)),

        row(TradingTimeId(index))(cyaAddress, uaWithValue(TradingTimeId(index), false)),
        row(TradingTimeId(index))(enterPreviousPostcode(NormalMode), uaWithValue(TradingTimeId(index), true)),

        row(EnterPreviousPostCodeId(index))(selectPreviousAddress(NormalMode), uaWithValue(EnterPreviousPostCodeId(index), seqAddresses)),
        row(PreviousAddressListId(index))(cyaAddress, uaWithValue(PreviousAddressListId(index), Data.tolerantAddress)),
        row(PreviousAddressId(index))(cyaAddress, uaWithValue(PreviousAddressId(index), address)),

      )

    def editNavigation: TableFor3[Identifier, UserAnswers, Call] =
      Table(
        ("Id", "Next Page", "UserAnswers (Optional)"),
        row(PartnershipDetailsId(index))(controllers.routes.IndexController.onPageLoad()),

        row(EnterEmailId(index))(controllers.establishers.partnership.contact.routes.CheckYourAnswersController.onPageLoad(index)),
        row(EnterPhoneId(index))(controllers.establishers.partnership.contact.routes.CheckYourAnswersController.onPageLoad(index)),

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
        row(EnterPostCodeId(index))(selectAddress(CheckMode), uaWithValue(EnterPostCodeId(index), seqAddresses)),
        row(AddressListId(index))(cyaAddress, uaWithValue(AddressListId(index), Data.tolerantAddress)),
        row(AddressId(index))(cyaAddress, uaWithValue(AddressYearsId(index), true)),
        row(AddressYearsId(index))(cyaAddress, uaWithValue(AddressYearsId(index), true)),
        row(AddressYearsId(index))(enterPreviousPostcode(CheckMode),uaWithValue(AddressYearsId(index), false)),
        row(EnterPreviousPostCodeId(index))(selectPreviousAddress(CheckMode),uaWithValue(EnterPreviousPostCodeId(index), seqAddresses)),
        row(PreviousAddressListId(index))(cyaAddress, uaWithValue(PreviousAddressListId(index), Data.tolerantAddress)),
        row(PreviousAddressId(index))(cyaAddress, uaWithValue(PreviousAddressId(index), address))
      )

    "in NormalMode" must {
      behave like navigatorWithRoutesForMode(NormalMode)(navigator, navigation)
    }

    "CheckMode" must {
      behave like navigatorWithRoutesForMode(CheckMode)(navigator, editNavigation)
    }
  }
}
