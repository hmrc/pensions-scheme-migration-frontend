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
import controllers.establishers.routes
import identifiers.establishers.individual.EstablisherNameId
import identifiers.establishers.individual.address._
import identifiers.establishers.individual.contact.{EnterEmailId, EnterPhoneId}
import identifiers.establishers.individual.details._
import identifiers.establishers.{AddEstablisherId, ConfirmDeleteEstablisherId, EstablisherKindId}
import identifiers.{Identifier, TypedIdentifier}
import models.establishers.EstablisherKind
import models.{CheckMode, Index, Mode, NormalMode, PersonName, ReferenceValue, _}
import org.scalatest.TryValues
import org.scalatest.prop.TableFor3
import play.api.libs.json.Writes
import play.api.mvc.Call
import utils.Data.ua
import utils.{Data, Enumerable, UserAnswers}

import java.time.LocalDate

class EstablishersNavigatorSpec
  extends SpecBase
    with NavigatorBehaviour
    with Enumerable.Implicits
    with TryValues {

  private val navigator: CompoundNavigator = injector.instanceOf[CompoundNavigator]
  private val index: Index = Index(0)
  private val uaWithEstablisherKind: EstablisherKind => UserAnswers = kind => UserAnswers().set(EstablisherKindId(index), kind).get
  private val establisherNamePage: Call = controllers.establishers.individual.routes.EstablisherNameController.onPageLoad(index)
  private val companyDetailsPage: Call = controllers.establishers.company.routes.CompanyDetailsController.onPageLoad(index)
  private val partnershipDetailsPage: Call = controllers.establishers.partnership.routes.PartnershipDetailsController.onPageLoad(index)
  private val addEstablisherPage: Call = controllers.establishers.routes.AddEstablisherController.onPageLoad()
  private val addEstablisherDetailsPage: Call = controllers.establishers.individual.routes.SpokeTaskListController.onPageLoad(index)
  private val taskListPage: Call = controllers.routes.TaskListController.onPageLoad()
  private val establisherKindPage: Call = routes.EstablisherKindController.onPageLoad(index)
  private val detailsUa: UserAnswers =
    ua.set(EstablisherNameId(0), PersonName("Jane", "Doe")).success.value

  private def addressUAWithValue[A](idType:TypedIdentifier[A], idValue:A)(implicit writes: Writes[A]) =
    detailsUa.set(idType, idValue).toOption

  private val seqAddresses = Seq(
    TolerantAddress(Some("1"),Some("1"),Some("c"),Some("d"), Some("zz11zz"), Some("GB")),
    TolerantAddress(Some("2"),Some("2"),Some("c"),Some("d"), Some("zz11zz"), Some("GB")),
  )

  val address = Address("addr1", "addr2", None, None, Some("ZZ11ZZ"), "GB")

  private val cyaAddress: Call = controllers.establishers.individual.address.routes.CheckYourAnswersController.onPageLoad(index)

  private def enterPreviousPostcode(mode:Mode): Call =
    controllers.establishers.individual.address.routes.EnterPreviousPostcodeController.onPageLoad( index,mode)

  private def selectAddress(mode:Mode): Call = {
    controllers.establishers.individual.address.routes.SelectAddressController.onPageLoad(index,mode)
  }

  private def selectPreviousAddress(mode:Mode): Call = controllers.establishers.individual.address.routes.SelectPreviousAddressController.onPageLoad(index,mode)

  private def addressYears(mode:Mode): Call = controllers.establishers.individual.address.routes.AddressYearsController.onPageLoad(index,mode)

  private def enterPhonePage(mode:Mode): Call =
    controllers.establishers.individual.contact.routes.EnterPhoneController.onPageLoad(index, mode)

  private val cyaContact: Call =
    controllers.establishers.individual.contact.routes.CheckYourAnswersController.onPageLoad(index)

  "EstablishersNavigator" when {
    def navigation: TableFor3[Identifier, UserAnswers, Call] =
      Table(
        ("Id", "Next Page", "UserAnswers (Optional)"),
        row(EstablisherKindId(index))(establisherNamePage, Some(uaWithEstablisherKind(EstablisherKind.Individual))),
        row(EstablisherKindId(index))(companyDetailsPage, Some(uaWithEstablisherKind(EstablisherKind.Company))),
        row(EstablisherKindId(index))(partnershipDetailsPage, Some(uaWithEstablisherKind(EstablisherKind.Partnership))),
        row(EstablisherNameId(index))(addEstablisherDetailsPage),
        row(AddEstablisherId(Some(true)))(establisherKindPage),
        row(AddEstablisherId(Some(false)))(taskListPage),
        row(ConfirmDeleteEstablisherId)(addEstablisherPage),

        row(EstablisherDOBId(index))(controllers.establishers.individual.details.routes.EstablisherHasNINOController.onPageLoad(index, NormalMode), Some(detailsUa.set(EstablisherDOBId(index), LocalDate.parse("2000-01-01")).success.value)),
        row(EstablisherHasNINOId(index))(controllers.establishers.individual.details.routes.EstablisherEnterNINOController.onPageLoad(index, NormalMode), Some(detailsUa.set(EstablisherHasNINOId(index), true).success.value)),
        row(EstablisherHasNINOId(index))(controllers.establishers.individual.details.routes.EstablisherNoNINOReasonController.onPageLoad(index, NormalMode), Some(detailsUa.set(EstablisherHasNINOId(index), false).success.value)),
        row(EstablisherNINOId(index))(controllers.establishers.individual.details.routes.EstablisherHasUTRController.onPageLoad(index, NormalMode), Some(detailsUa.set(EstablisherNINOId(index), ReferenceValue("AB123456C")).success.value)),
        row(EstablisherNoNINOReasonId(index))(controllers.establishers.individual.details.routes.EstablisherHasUTRController.onPageLoad(index, NormalMode), Some(detailsUa.set(EstablisherNoNINOReasonId(index), "Reason").success.value)),
        row(EstablisherHasUTRId(index))(controllers.establishers.individual.details.routes.EstablisherEnterUTRController.onPageLoad(index, NormalMode), Some(detailsUa.set(EstablisherHasUTRId(index), true).success.value)),
        row(EstablisherHasUTRId(index))(controllers.establishers.individual.details.routes.EstablisherNoUTRReasonController.onPageLoad(index, NormalMode), Some(detailsUa.set(EstablisherHasUTRId(index), false).success.value)),
        row(EstablisherUTRId(index))(controllers.establishers.individual.details.routes.CheckYourAnswersController.onPageLoad(index), Some(detailsUa.set(EstablisherUTRId(index), ReferenceValue("1234567890")).success.value)),
        row(EstablisherNoUTRReasonId(index))(controllers.establishers.individual.details.routes.CheckYourAnswersController.onPageLoad(index), Some(detailsUa.set(EstablisherNoUTRReasonId(index), "Reason").success.value)),

        row(EnterPostCodeId(index))(selectAddress(NormalMode), addressUAWithValue(EnterPostCodeId(index), seqAddresses)),
        row(AddressListId(index))(addressYears(NormalMode), addressUAWithValue(AddressListId(index), Data.tolerantAddress)),
        row(AddressId(index))(addressYears(NormalMode), addressUAWithValue(AddressId(index), address)),

        row(AddressYearsId(index))(cyaAddress, addressUAWithValue(AddressYearsId(index), true)),
        row(AddressYearsId(index))(enterPreviousPostcode(NormalMode), addressUAWithValue(AddressYearsId(index), false)),

        row(EnterPreviousPostCodeId(index))(selectPreviousAddress(NormalMode), addressUAWithValue(EnterPreviousPostCodeId(index), seqAddresses)),
        row(PreviousAddressListId(index))(cyaAddress, addressUAWithValue(PreviousAddressListId(index), Data.tolerantAddress)),
        row(PreviousAddressId(index))(cyaAddress, addressUAWithValue(PreviousAddressId(index), address)),
        row(EnterEmailId(index))(enterPhonePage(NormalMode), Some(detailsUa.set(EnterEmailId(index), "test@test.com").success.value)),
        row(EnterPhoneId(index))(cyaContact, Some(detailsUa.set(EnterPhoneId(index), "1234").success.value))
      )

    def editNavigation: TableFor3[Identifier, UserAnswers, Call] =
      Table(
        ("Id", "Next Page", "UserAnswers (Optional)"),
        row(EstablisherDOBId(index))(controllers.establishers.individual.details.routes.CheckYourAnswersController.onPageLoad(index), Some(detailsUa.set(EstablisherDOBId(index), LocalDate.parse("2000-01-01")).success.value)),
        row(EstablisherHasNINOId(index))(controllers.establishers.individual.details.routes.EstablisherEnterNINOController.onPageLoad(index, CheckMode), Some(detailsUa.set(EstablisherHasNINOId(index), true).success.value)),
        row(EstablisherHasNINOId(index))(controllers.establishers.individual.details.routes.EstablisherNoNINOReasonController.onPageLoad(index, CheckMode), Some(detailsUa.set(EstablisherHasNINOId(index), false).success.value)),
        row(EstablisherNINOId(index))(controllers.establishers.individual.details.routes.CheckYourAnswersController.onPageLoad(index), Some(detailsUa.set(EstablisherNINOId(index), ReferenceValue("AB123456C")).success.value)),
        row(EstablisherNoNINOReasonId(index))(controllers.establishers.individual.details.routes.CheckYourAnswersController.onPageLoad(index), Some(detailsUa.set(EstablisherNoNINOReasonId(index), "Reason").success.value)),
        row(EstablisherHasUTRId(index))(controllers.establishers.individual.details.routes.EstablisherEnterUTRController.onPageLoad(index, CheckMode), Some(detailsUa.set(EstablisherHasUTRId(index), true).success.value)),
        row(EstablisherHasUTRId(index))(controllers.establishers.individual.details.routes.EstablisherNoUTRReasonController.onPageLoad(index, CheckMode), Some(detailsUa.set(EstablisherHasUTRId(index), false).success.value)),
        row(EstablisherUTRId(index))(controllers.establishers.individual.details.routes.CheckYourAnswersController.onPageLoad(index), Some(detailsUa.set(EstablisherUTRId(index), ReferenceValue("1234567890")).success.value)),
        row(EstablisherNoUTRReasonId(index))(controllers.establishers.individual.details.routes.CheckYourAnswersController.onPageLoad(index), Some(detailsUa.set(EstablisherNoUTRReasonId(index), "Reason").success.value)),
        row(EnterEmailId(index))(cyaContact, Some(detailsUa.set(EnterEmailId(index), "test@test.com").success.value)),
        row(EnterPhoneId(index))(cyaContact, Some(detailsUa.set(EnterPhoneId(index), "1234").success.value)),
        row(EnterPostCodeId(index))(selectAddress(CheckMode), addressUAWithValue(EnterPostCodeId(index), seqAddresses)),
        row(AddressListId(index))(cyaAddress, addressUAWithValue(AddressListId(index), Data.tolerantAddress)),
        row(AddressId(index))(cyaAddress, addressUAWithValue(AddressYearsId(index), true)),
        row(AddressYearsId(index))(cyaAddress, addressUAWithValue(AddressYearsId(index), true)),
        row(AddressYearsId(index))(enterPreviousPostcode(CheckMode),addressUAWithValue(AddressYearsId(index), false)),
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
