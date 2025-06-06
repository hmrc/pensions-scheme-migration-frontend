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
import controllers.trustees.individual.details.{routes => detailsRoutes}
import controllers.trustees.routes
import identifiers.trustees._
import identifiers.trustees.individual.TrusteeNameId
import identifiers.trustees.individual.address._
import identifiers.trustees.individual.contact.{EnterEmailId, EnterPhoneId}
import identifiers.trustees.individual.details._
import identifiers.{Identifier, TypedIdentifier}
import models._
import models.trustees.TrusteeKind
import org.scalatest.TryValues
import org.scalatest.prop.TableFor3
import play.api.libs.json.Writes
import play.api.mvc.Call
import utils.Data.ua
import utils.{Data, Enumerable, UserAnswers}

import java.time.LocalDate

class TrusteesNavigatorSpec
  extends SpecBase
    with NavigatorBehaviour
    with Enumerable.Implicits
    with TryValues {

  private val navigator: CompoundNavigator = injector.instanceOf[CompoundNavigator]
  private val index: Index = Index(0)
  private val detailsUa: UserAnswers =
    ua.set(TrusteeNameId(0), PersonName("Jane", "Doe")).success.value
  private val uaWithTrusteeKind: TrusteeKind => UserAnswers = kind => UserAnswers().set(TrusteeKindId(index, TrusteeKind.Individual), kind).get
  private val indvDetailsUa: UserAnswers = uaWithTrusteeKind(TrusteeKind.Individual).set(TrusteeNameId(0), PersonName("Jane", "Doe")).success.value
  private val trusteeNamePage: Call = controllers.trustees.individual.routes.TrusteeNameController.onPageLoad(index)
  private val companyPage: Call = controllers.trustees.company.routes.CompanyDetailsController.onPageLoad(index)
  private val partnershipPage: Call = controllers.trustees.partnership.routes.PartnershipDetailsController.onPageLoad(index)
  private def trusteePhonePage(mode: Mode): Call = controllers.trustees.individual.contact.routes.EnterPhoneController.onPageLoad(index, mode)
  private val addTrusteePage: Call = controllers.trustees.routes.AddTrusteeController.onPageLoad
  private val addTrusteeDetailsPage: Call = controllers.common.routes.SpokeTaskListController.onPageLoad(index, entities.Trustee, entities.Individual)
  private val taskListPage: Call = controllers.routes.TaskListController.onPageLoad
  private val otherTrusteesPage: Call = controllers.trustees.routes.OtherTrusteesController.onPageLoad
  private val trusteeKindPage: Call = routes.TrusteeKindController.onPageLoad(index)
  private def hasNinoPage(mode: Mode): Call =
    detailsRoutes.TrusteeHasNINOController.onPageLoad(index, mode)
  private def enterNinoPage(mode: Mode): Call =
    detailsRoutes.TrusteeEnterNINOController.onPageLoad(index, mode)
  private def noNinoPage(mode: Mode): Call =
    detailsRoutes.TrusteeNoNINOReasonController.onPageLoad(index, mode)
  private def hasUtrPage(mode: Mode): Call =
    detailsRoutes.TrusteeHasUTRController.onPageLoad(index, mode)
  private def enterUtrPage(mode: Mode): Call =
    detailsRoutes.TrusteeEnterUTRController.onPageLoad(index, mode)
  private def noUtrPage(mode: Mode): Call =
    detailsRoutes.TrusteeNoUTRReasonController.onPageLoad(index, mode)
  private val cya: Call =
    controllers.common.routes.CheckYourAnswersController.onPageLoad(index,
      entities.Trustee,
      entities.Individual,
      entities.Details)
  private val cyaContact: Call =
    controllers.common.routes.CheckYourAnswersController.onPageLoad(index,
      entities.Trustee,
      entities.Individual,
      entities.Contacts)


  private def addressUAWithValue[A](idType:TypedIdentifier[A], idValue:A)(implicit writes: Writes[A]) =
    detailsUa.set(idType, idValue).toOption

  private val seqAddresses = Seq(
    TolerantAddress(Some("1"),Some("1"),Some("c"),Some("d"), Some("zz11zz"), Some("GB")),
    TolerantAddress(Some("2"),Some("2"),Some("c"),Some("d"), Some("zz11zz"), Some("GB"))
  )

  val address = Address("addr1", "addr2", None, None, Some("ZZ11ZZ"), "GB")

  private val cyaAddress: Call =
    controllers.common.routes.CheckYourAnswersController.onPageLoad(index,
      entities.Trustee,
      entities.Individual,
      entities.Address)

  private def enterPreviousPostcode(mode:Mode): Call =
    controllers.trustees.individual.address.routes.EnterPreviousPostcodeController.onPageLoad(index, mode)

  private def selectAddress(mode:Mode): Call =
    controllers.trustees.individual.address.routes.SelectAddressController.onPageLoad(index, mode)

  private def selectPreviousAddress(mode:Mode): Call =
    controllers.trustees.individual.address.routes.SelectPreviousAddressController.onPageLoad(index, mode)

  private def addressYears(mode:Mode): Call =
    controllers.trustees.individual.address.routes.AddressYearsController.onPageLoad(index, mode)

  @annotation.nowarn
  private def namePage(mode:Mode): Call =
    controllers.trustees.individual.routes.TrusteeNameController.onPageLoad(index)

  "TrusteesNavigator" when {
    def navigation: TableFor3[Identifier, UserAnswers, Call] =
      Table(
        ("Id", "Next Page", "UserAnswers (Optional)"),
        row(TrusteeKindId(index, TrusteeKind.Individual))(trusteeNamePage, Some(uaWithTrusteeKind(TrusteeKind.Individual))),
        row(TrusteeKindId(index, TrusteeKind.Company))(companyPage, Some(uaWithTrusteeKind(TrusteeKind.Company))),
        row(TrusteeKindId(index, TrusteeKind.Partnership))(partnershipPage, Some(uaWithTrusteeKind(TrusteeKind.Partnership))),
        row(TrusteeNameId(index))(addTrusteeDetailsPage),
        row(AddTrusteeId(Some(true)))(trusteeKindPage),
        row(AddTrusteeId(Some(false)))(taskListPage),
        row(AddTrusteeId(None))(otherTrusteesPage),
        row(TrusteeDOBId(index))(hasNinoPage(NormalMode), Some(detailsUa.set(TrusteeDOBId(index), LocalDate.parse("2000-01-01")).success.value)),
        row(TrusteeHasNINOId(index))(enterNinoPage(NormalMode), Some(detailsUa.set(TrusteeHasNINOId(index), true).success.value)),
        row(TrusteeHasNINOId(index))(noNinoPage(NormalMode), Some(detailsUa.set(TrusteeHasNINOId(index), false).success.value)),
        row(TrusteeNINOId(index))(hasUtrPage(NormalMode), Some(detailsUa.set(TrusteeNINOId(index), ReferenceValue("AB123456C")).success.value)),
        row(TrusteeNoNINOReasonId(index))(hasUtrPage(NormalMode), Some(detailsUa.set(TrusteeNoNINOReasonId(index), "Reason").success.value)),
        row(TrusteeHasUTRId(index))(enterUtrPage(NormalMode), Some(detailsUa.set(TrusteeHasUTRId(index), true).success.value)),
        row(TrusteeHasUTRId(index))(noUtrPage(NormalMode), Some(detailsUa.set(TrusteeHasUTRId(index), false).success.value)),
        row(TrusteeUTRId(index))(cya, Some(detailsUa.set(TrusteeUTRId(index), ReferenceValue("1234567890")).success.value)),
        row(TrusteeNoUTRReasonId(index))(cya, Some(detailsUa.set(TrusteeNoUTRReasonId(index), "Reason").success.value)),

        row(EnterPostCodeId(index))(selectAddress(NormalMode), addressUAWithValue(EnterPostCodeId(index), seqAddresses)),
        row(AddressListId(index))(addressYears(NormalMode), addressUAWithValue(AddressListId(index), Data.tolerantAddress)),
        row(AddressId(index))(addressYears(NormalMode), addressUAWithValue(AddressId(index), address)),

        row(AddressYearsId(index))(cyaAddress, addressUAWithValue(AddressYearsId(index), true)),
        row(AddressYearsId(index))(enterPreviousPostcode(NormalMode), addressUAWithValue(AddressYearsId(index), false)),

        row(EnterPreviousPostCodeId(index))(selectPreviousAddress(NormalMode), addressUAWithValue(EnterPreviousPostCodeId(index), seqAddresses)),
        row(PreviousAddressListId(index))(cyaAddress, addressUAWithValue(PreviousAddressListId(index), Data.tolerantAddress)),
        row(PreviousAddressId(index))(cyaAddress, addressUAWithValue(PreviousAddressId(index), address)),
          row(EnterEmailId(index))(trusteePhonePage(NormalMode), Some(indvDetailsUa.set(EnterEmailId(index), "test@test.com").success.value)),
        row(EnterPhoneId(index))(cyaContact, Some(indvDetailsUa.set(EnterPhoneId(index), "123").success.value)),
        row(OtherTrusteesId)(taskListPage),
        row(DirectorAlsoTrusteeId(index))(namePage(NormalMode), Some(indvDetailsUa.set(DirectorAlsoTrusteeId(index), -1).success.value)),
        row(DirectorAlsoTrusteeId(index))(addTrusteePage, Some(ua.set(DirectorAlsoTrusteeId(index), value = 2).success.value)),
        row(DirectorsAlsoTrusteesId(index))(namePage(NormalMode), Some(detailsUa.set(DirectorsAlsoTrusteesId(index), Seq(-1)).success.value)),
        row(DirectorsAlsoTrusteesId(index))(addTrusteePage, Some(ua.set(DirectorsAlsoTrusteesId(index), value = Seq(2)).success.value))
      )

    def editNavigation: TableFor3[Identifier, UserAnswers, Call] =
      Table(
        ("Id", "Next Page", "UserAnswers (Optional)"),
        row(TrusteeDOBId(index))(cya, Some(detailsUa.set(TrusteeDOBId(index), LocalDate.parse("2000-01-01")).success.value)),
        row(TrusteeHasNINOId(index))(enterNinoPage(CheckMode), Some(detailsUa.set(TrusteeHasNINOId(index), true).success.value)),
        row(TrusteeHasNINOId(index))(noNinoPage(CheckMode), Some(detailsUa.set(TrusteeHasNINOId(index), false).success.value)),
        row(TrusteeNINOId(index))(cya, Some(detailsUa.set(TrusteeNINOId(index), ReferenceValue("AB123456C")).success.value)),
        row(TrusteeNoNINOReasonId(index))(cya, Some(detailsUa.set(TrusteeNoNINOReasonId(index), "Reason").success.value)),
        row(TrusteeHasUTRId(index))(enterUtrPage(CheckMode), Some(detailsUa.set(TrusteeHasUTRId(index), true).success.value)),
        row(TrusteeHasUTRId(index))(noUtrPage(CheckMode), Some(detailsUa.set(TrusteeHasUTRId(index), false).success.value)),
        row(TrusteeUTRId(index))(cya, Some(detailsUa.set(TrusteeUTRId(index), ReferenceValue("1234567890")).success.value)),
        row(TrusteeNoUTRReasonId(index))(cya, Some(detailsUa.set(TrusteeNoUTRReasonId(index), "Reason").success.value)),
        row(EnterEmailId(index))(cyaContact, Some(indvDetailsUa.set(EnterEmailId(index), "test@test.com").success.value)),
        row(EnterPhoneId(index))(cyaContact, Some(indvDetailsUa.set(EnterPhoneId(index), "123").success.value)),
        row(EnterPostCodeId(index))(selectAddress(CheckMode), addressUAWithValue(EnterPostCodeId(index), seqAddresses)),
        row(AddressListId(index))(cyaAddress, addressUAWithValue(AddressListId(index), Data.tolerantAddress)),
        row(AddressId(index))(cyaAddress, Some(detailsUa.set(AddressYearsId(index), true).success.value)),
        row(AddressYearsId(index))(cyaAddress, Some(detailsUa.set(AddressYearsId(index), true).success.value)),
        row(AddressYearsId(index))(enterPreviousPostcode(CheckMode), Some(detailsUa.set(AddressYearsId(index), false).success.value)),
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
