/*
 * Copyright 2024 HM Revenue & Customs
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
import controllers.establishers.partnership.partner.details
import identifiers.establishers.partnership.partner.address._
import identifiers.establishers.partnership.partner.contact.{EnterEmailId, EnterPhoneId}
import identifiers.establishers.partnership.partner.details._
import identifiers.establishers.partnership.partner.{ConfirmDeletePartnerId, PartnerNameId}
import identifiers.{Identifier, TypedIdentifier}
import models._
import org.scalatest.TryValues
import org.scalatest.prop.TableFor3
import play.api.libs.json.Writes
import play.api.mvc.Call
import utils.Data.ua
import utils.{Data, Enumerable, UserAnswers}

import java.time.LocalDate

class EstablishersPartnerNavigatorSpec
  extends SpecBase
    with NavigatorBehaviour
    with Enumerable.Implicits
    with TryValues {

  private val navigator: CompoundNavigator = injector.instanceOf[CompoundNavigator]
  private val establisherIndex: Index = Index(0)
  private val partnerIndex: Index = Index(0)
  private val partnerDOBPage: Call = controllers.establishers.partnership.partner.details.routes.PartnerDOBController.onPageLoad(establisherIndex,partnerIndex,NormalMode)

  private val detailsUa: UserAnswers =
    ua.set(PartnerNameId(establisherIndex ,partnerIndex), PersonName("Jane", "Doe")).success.value
  private def hasNinoPage(mode: Mode): Call =
    details.routes.PartnerHasNINOController.onPageLoad(establisherIndex,partnerIndex, mode)
  private def enterNinoPage(mode: Mode): Call =
    details.routes.PartnerEnterNINOController.onPageLoad(establisherIndex,partnerIndex, mode)
  private def noNinoPage(mode: Mode): Call =
    details.routes.PartnerNoNINOReasonController.onPageLoad(establisherIndex,partnerIndex, mode)
  private def hasUtrPage(mode: Mode): Call =
    details.routes.PartnerHasUTRController.onPageLoad(establisherIndex,partnerIndex, mode)
  private def enterUtrPage(mode: Mode): Call =
    details.routes.PartnerEnterUTRController.onPageLoad(establisherIndex,partnerIndex, mode)
  private def noUtrPage(mode: Mode): Call =
    details.routes.PartnerNoUTRReasonController.onPageLoad(establisherIndex,partnerIndex, mode)
  private val cya: Call = controllers.common.routes.CheckYourAnswersController.onPageLoadWithRepresentative(establisherIndex,
    entities.Establisher,
    entities.Partnership,
    partnerIndex)
  private def addressUAWithValue[A](idType:TypedIdentifier[A], idValue:A)(implicit writes: Writes[A]) =
    detailsUa.set(idType, idValue).toOption

  private val seqAddresses = Seq(
    TolerantAddress(Some("1"),Some("1"),Some("c"),Some("d"), Some("zz11zz"), Some("GB")),
    TolerantAddress(Some("2"),Some("2"),Some("c"),Some("d"), Some("zz11zz"), Some("GB"))
  )

  val address = Address("addr1", "addr2", None, None, Some("ZZ11ZZ"), "GB")


  private def addAddPartnersPage(establisherIndex:Int,mode:Mode): Call = controllers.establishers.partnership.routes.AddPartnersController.onPageLoad(establisherIndex,mode)
  private def postcode(establisherIndex:Int,partnerIndex:Int,mode: Mode): Call = controllers.establishers.partnership.partner.address.routes.EnterPostcodeController.onPageLoad(establisherIndex, partnerIndex, mode)
  private def enterPreviousPostcode(establisherIndex:Int,partnerIndex:Int,mode:Mode): Call =
    controllers.establishers.partnership.partner.address.routes.EnterPreviousPostcodeController.onPageLoad(establisherIndex,partnerIndex,mode)

  private def selectAddress(establisherIndex:Int,partnerIndex:Int,mode:Mode): Call =
    controllers.establishers.partnership.partner.address.routes.SelectAddressController.onPageLoad(establisherIndex,partnerIndex,mode)

  private def selectPreviousAddress(establisherIndex:Int,partnerIndex:Int,mode:Mode): Call =
    controllers.establishers.partnership.partner.address.routes.SelectPreviousAddressController.onPageLoad(establisherIndex,partnerIndex,mode)

  private def addressYears(establisherIndex:Int,partnerIndex:Int,mode:Mode): Call =
    controllers.establishers.partnership.partner.address.routes.AddressYearsController.onPageLoad(establisherIndex,partnerIndex,mode)

  private def enterPhonePage(establisherIndex:Int,partnerIndex:Int,mode:Mode): Call =
    controllers.establishers.partnership.partner.contact.routes.EnterPhoneNumberController.onPageLoad(establisherIndex,partnerIndex,mode)

  private def enterEmailPage(establisherIndex:Int,partnerIndex:Int,mode:Mode): Call =
    controllers.establishers.partnership.partner.contact.routes.EnterEmailController.onPageLoad(establisherIndex,partnerIndex,mode)


  "EstablishersPartnerNavigator" when {
    def navigation: TableFor3[Identifier, UserAnswers, Call] =
      Table(
        ("Id", "Next Page", "UserAnswers (Optional)"),
        row(PartnerNameId(establisherIndex,partnerIndex))(partnerDOBPage),
        row(PartnerDOBId(establisherIndex,partnerIndex))(hasNinoPage(NormalMode),Some(detailsUa.set(PartnerDOBId(establisherIndex,partnerIndex), LocalDate.parse("2000-01-01")).success.value)),
        row(PartnerHasNINOId(establisherIndex,partnerIndex))(enterNinoPage(NormalMode), Some(detailsUa.set(PartnerHasNINOId(establisherIndex,partnerIndex), true).success.value)),
        row(PartnerHasNINOId(establisherIndex,partnerIndex))(noNinoPage(NormalMode), Some(detailsUa.set(PartnerHasNINOId(establisherIndex,partnerIndex), false).success.value)),
        row(PartnerNINOId(establisherIndex,partnerIndex))(hasUtrPage(NormalMode), Some(detailsUa.set(PartnerNINOId(establisherIndex,partnerIndex), ReferenceValue("AB123456C")).success.value)),
        row(PartnerNoNINOReasonId(establisherIndex,partnerIndex))(hasUtrPage(NormalMode), Some(detailsUa.set(PartnerNoNINOReasonId(establisherIndex,partnerIndex), "Reason").success.value)),
        row(PartnerHasUTRId(establisherIndex,partnerIndex))(enterUtrPage(NormalMode), Some(detailsUa.set(PartnerHasUTRId(establisherIndex,partnerIndex), true).success.value)),
        row(PartnerHasUTRId(establisherIndex,partnerIndex))(noUtrPage(NormalMode), Some(detailsUa.set(PartnerHasUTRId(establisherIndex,partnerIndex), false).success.value)),
        row(PartnerEnterUTRId(establisherIndex,partnerIndex))(postcode(establisherIndex,partnerIndex,NormalMode), Some(detailsUa.set(PartnerEnterUTRId(establisherIndex,partnerIndex), ReferenceValue("1234567890")).success.value)),
        row(PartnerNoUTRReasonId(establisherIndex,partnerIndex))(postcode(establisherIndex,partnerIndex,NormalMode), Some(detailsUa.set(PartnerNoUTRReasonId(establisherIndex,partnerIndex), "Reason").success.value)),
        row(AddressId(establisherIndex,partnerIndex))(addressYears(establisherIndex,partnerIndex,NormalMode), addressUAWithValue(AddressId(establisherIndex,partnerIndex), address)),
        row(AddressListId(establisherIndex,partnerIndex))(addressYears(establisherIndex,partnerIndex,NormalMode), addressUAWithValue(AddressListId(establisherIndex,partnerIndex), Data.tolerantAddress)),
        row(AddressYearsId(establisherIndex,partnerIndex))(enterEmailPage(establisherIndex,partnerIndex,NormalMode), addressUAWithValue(AddressYearsId(establisherIndex,partnerIndex), true)),
        row(AddressYearsId(establisherIndex,partnerIndex))(enterPreviousPostcode(establisherIndex,partnerIndex,NormalMode), addressUAWithValue(AddressYearsId(establisherIndex,partnerIndex), false)),
        row(EnterPostCodeId(establisherIndex,partnerIndex))(selectAddress(establisherIndex,partnerIndex,NormalMode), addressUAWithValue(EnterPostCodeId(establisherIndex,partnerIndex), seqAddresses)),
        row(EnterPreviousPostCodeId(establisherIndex,partnerIndex))(selectPreviousAddress(establisherIndex,partnerIndex,NormalMode), addressUAWithValue(EnterPreviousPostCodeId(establisherIndex,partnerIndex), seqAddresses)),
        row(PreviousAddressListId(establisherIndex,partnerIndex))(enterEmailPage(establisherIndex,partnerIndex,NormalMode), addressUAWithValue(PreviousAddressListId(establisherIndex,partnerIndex), Data.tolerantAddress)),
        row(PreviousAddressId(establisherIndex,partnerIndex))(enterEmailPage(establisherIndex,partnerIndex,NormalMode), addressUAWithValue(PreviousAddressId(establisherIndex,partnerIndex), address)),
        row(EnterEmailId(establisherIndex,partnerIndex))(enterPhonePage(establisherIndex,partnerIndex,NormalMode), Some(detailsUa.set(EnterEmailId(establisherIndex,partnerIndex), "test@test.com").success.value)),
        row(EnterPhoneId(establisherIndex,partnerIndex))(cya, Some(detailsUa.set(EnterPhoneId(establisherIndex,partnerIndex), "1234").success.value)),
        row(ConfirmDeletePartnerId(partnerIndex))(addAddPartnersPage(partnerIndex,NormalMode), Some(detailsUa.set(ConfirmDeletePartnerId(partnerIndex),true).success.value))
      )

    def editNavigation: TableFor3[Identifier, UserAnswers, Call] =
      Table(
        ("Id", "Next Page", "UserAnswers (Optional)"),
        row(PartnerNameId(establisherIndex,partnerIndex))(cya, Some(detailsUa.set(PartnerNameId(establisherIndex,partnerIndex),PersonName("Jane", "Doe")).success.value)),
        row(PartnerDOBId(establisherIndex,partnerIndex))(cya, Some(detailsUa.set(PartnerDOBId(establisherIndex,partnerIndex), LocalDate.parse("2000-01-01")).success.value)),
        row(PartnerHasNINOId(establisherIndex,partnerIndex))(enterNinoPage(CheckMode), Some(detailsUa.set(PartnerHasNINOId(establisherIndex,partnerIndex), true).success.value)),
        row(PartnerHasNINOId(establisherIndex,partnerIndex))(noNinoPage(CheckMode), Some(detailsUa.set(PartnerHasNINOId(establisherIndex,partnerIndex), false).success.value)),
        row(PartnerNINOId(establisherIndex,partnerIndex))(cya, Some(detailsUa.set(PartnerNINOId(establisherIndex,partnerIndex), ReferenceValue("AB123456C")).success.value)),
        row(PartnerNoNINOReasonId(establisherIndex,partnerIndex))(cya, Some(detailsUa.set(PartnerNoNINOReasonId(establisherIndex,partnerIndex), "Reason").success.value)),
        row(PartnerHasUTRId(establisherIndex,partnerIndex))(enterUtrPage(CheckMode), Some(detailsUa.set(PartnerHasUTRId(establisherIndex,partnerIndex), true).success.value)),
        row(PartnerHasUTRId(establisherIndex,partnerIndex))(noUtrPage(CheckMode), Some(detailsUa.set(PartnerHasUTRId(establisherIndex,partnerIndex), false).success.value)),
        row(PartnerEnterUTRId(establisherIndex,partnerIndex))(cya, Some(detailsUa.set(PartnerEnterUTRId(establisherIndex,partnerIndex), ReferenceValue("1234567890")).success.value)),
        row(PartnerNoUTRReasonId(establisherIndex,partnerIndex))(cya, Some(detailsUa.set(PartnerNoUTRReasonId(establisherIndex,partnerIndex), "Reason").success.value)),
        row(AddressId(establisherIndex,partnerIndex))(cya, addressUAWithValue(AddressId(establisherIndex,partnerIndex), address)),
        row(AddressListId(establisherIndex,partnerIndex))(cya, addressUAWithValue(AddressListId(establisherIndex,partnerIndex), Data.tolerantAddress)),
        row(AddressYearsId(establisherIndex,partnerIndex))(cya, addressUAWithValue(AddressYearsId(establisherIndex,partnerIndex), true)),
        row(AddressYearsId(establisherIndex,partnerIndex))(enterPreviousPostcode(establisherIndex,partnerIndex,CheckMode), addressUAWithValue(AddressYearsId(establisherIndex,partnerIndex), false)),
        row(EnterPostCodeId(establisherIndex,partnerIndex))(selectAddress(establisherIndex,partnerIndex,CheckMode), addressUAWithValue(EnterPostCodeId(establisherIndex,partnerIndex), seqAddresses)),
        row(EnterPreviousPostCodeId(establisherIndex,partnerIndex))(selectPreviousAddress(establisherIndex,partnerIndex,CheckMode), addressUAWithValue(EnterPreviousPostCodeId(establisherIndex,partnerIndex), seqAddresses)),
        row(PreviousAddressListId(establisherIndex,partnerIndex))(cya, addressUAWithValue(PreviousAddressListId(establisherIndex,partnerIndex), Data.tolerantAddress)),
        row(PreviousAddressId(establisherIndex,partnerIndex))(cya, addressUAWithValue(PreviousAddressId(establisherIndex,partnerIndex), address)),
        row(EnterEmailId(establisherIndex,partnerIndex))(cya, Some(detailsUa.set(EnterEmailId(establisherIndex,partnerIndex), "test@test.com").success.value)),
        row(EnterPhoneId(establisherIndex,partnerIndex))(cya, Some(detailsUa.set(EnterPhoneId(establisherIndex,partnerIndex), "1234").success.value))
      )

    "in NormalMode" must {
      behave like navigatorWithRoutesForMode(NormalMode)(navigator, navigation)
    }

    "CheckMode" must {
      behave like navigatorWithRoutesForMode(CheckMode)(navigator, editNavigation)
    }
  }
}
