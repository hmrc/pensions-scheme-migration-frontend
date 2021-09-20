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
import controllers.establishers.routes
import helpers.routes.EstablishersIndividualRoutes
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
import utils.{Enumerable, UserAnswers}
import helpers.routes.EstablishersIndividualRoutes._

import java.time.LocalDate

class EstablishersNavigatorSpec
  extends SpecBase
    with NavigatorBehaviour
    with Enumerable.Implicits
    with TryValues {

  private val navigator: CompoundNavigator = injector.instanceOf[CompoundNavigator]
  private val index: Index = Index(0)
  private val uaWithEstablisherKind: EstablisherKind => UserAnswers = kind => UserAnswers().set(EstablisherKindId(index), kind).get
  private val establisherNamePage: Call = nameRoute(index, NormalMode)
  private val companyDetailsPage: Call = controllers.establishers.company.routes.CompanyDetailsController.onPageLoad(index)
  private val partnershipDetailsPage: Call = controllers.establishers.partnership.routes.PartnershipDetailsController.onPageLoad(index)
  private val addEstablisherPage: Call = controllers.establishers.routes.AddEstablisherController.onPageLoad()
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

  private val cyaAddress: Call = EstablishersIndividualRoutes.cyaAddressRoute(index, NormalMode)

  private def enterPreviousPostcode(mode:Mode): Call =
    EstablishersIndividualRoutes.enterPreviousPostcodeRoute( index, NormalMode)

  private def selectAddress(mode:Mode): Call = {
    EstablishersIndividualRoutes.selectAddressRoute(index, NormalMode)
  }

  private def selectPreviousAddress(mode:Mode): Call = EstablishersIndividualRoutes.previousAddressResultsRoute(index, NormalMode)

  private def addressYears(mode:Mode): Call = EstablishersIndividualRoutes.timeAtAddressRoute(index, NormalMode)

  private def enterPhonePage(mode:Mode): Call =
    phoneNumberRoute(index, mode)

  private val cyaContact: Call =
    cyaContactRoute(index, NormalMode)

  "EstablishersNavigator" when {
    def navigation: TableFor3[Identifier, UserAnswers, Call] =
      Table(
        ("Id", "Next Page", "UserAnswers (Optional)"),
        row(EstablisherKindId(index))(establisherNamePage, Some(uaWithEstablisherKind(EstablisherKind.Individual))),
        row(EstablisherKindId(index))(companyDetailsPage, Some(uaWithEstablisherKind(EstablisherKind.Company))),
        row(EstablisherKindId(index))(partnershipDetailsPage, Some(uaWithEstablisherKind(EstablisherKind.Partnership))),
        row(EstablisherNameId(index))(addEstablisherPage),
        row(AddEstablisherId(Some(true)))(establisherKindPage),
        row(AddEstablisherId(Some(false)))(taskListPage),
        row(ConfirmDeleteEstablisherId)(addEstablisherPage),
        row(EstablisherDOBId(index))(haveNationalInsuranceNumberRoute(index, NormalMode), Some(detailsUa.set(EstablisherDOBId(index), LocalDate.parse("2000-01-01")).success.value)),
        row(EstablisherHasNINOId(index))(enterNationaInsuranceNumberRoute(index, NormalMode), Some(detailsUa.set(EstablisherHasNINOId(index), true).success.value)),
        row(EstablisherHasNINOId(index))(reasonForNoNationalInsuranceNumberRoute(index, NormalMode), Some(detailsUa.set(EstablisherHasNINOId(index), false).success.value)),
        row(EstablisherNINOId(index))(haveUniqueTaxpayerReferenceRoute(index, NormalMode), Some(detailsUa.set(EstablisherNINOId(index), ReferenceValue("AB123456C")).success.value)),
        row(EstablisherNoNINOReasonId(index))(haveUniqueTaxpayerReferenceRoute(index, NormalMode), Some(detailsUa.set(EstablisherNoNINOReasonId(index), "Reason").success.value)),
        row(EstablisherHasUTRId(index))(enterUniqueTaxpayerReferenceRoute(index, NormalMode), Some(detailsUa.set(EstablisherHasUTRId(index), true).success.value)),
        row(EstablisherHasUTRId(index))(reasonForNoUniqueTaxpayerReferenceRoute(index, NormalMode), Some(detailsUa.set(EstablisherHasUTRId(index), false).success.value)),
        row(EstablisherUTRId(index))(cyaDetailsRoute(index, NormalMode), Some(detailsUa.set(EstablisherUTRId(index), ReferenceValue("1234567890")).success.value)),
        row(EstablisherNoUTRReasonId(index))(cyaDetailsRoute(index, NormalMode), Some(detailsUa.set(EstablisherNoUTRReasonId(index), "Reason").success.value)),
        row(EnterPostCodeId(index))(selectAddress(NormalMode), addressUAWithValue(EnterPostCodeId(index), seqAddresses)),
        row(AddressListId(index))(addressYears(NormalMode), addressUAWithValue(AddressListId(index), 0)),
        row(AddressId(index))(addressYears(NormalMode), addressUAWithValue(AddressId(index), address)),

        row(AddressYearsId(index))(cyaAddress, addressUAWithValue(AddressYearsId(index), true)),
        row(AddressYearsId(index))(enterPreviousPostcode(NormalMode), addressUAWithValue(AddressYearsId(index), false)),

        row(EnterPreviousPostCodeId(index))(selectPreviousAddress(NormalMode), addressUAWithValue(EnterPreviousPostCodeId(index), seqAddresses)),
        row(PreviousAddressListId(index))(cyaAddress, addressUAWithValue(PreviousAddressListId(index), 0)),
        row(PreviousAddressId(index))(cyaAddress, addressUAWithValue(PreviousAddressId(index), address)),
        row(EnterEmailId(index))(enterPhonePage(NormalMode), Some(detailsUa.set(EnterEmailId(index), "test@test.com").success.value)),
        row(EnterPhoneId(index))(cyaContact, Some(detailsUa.set(EnterPhoneId(index), "1234").success.value))
      )

    def editNavigation: TableFor3[Identifier, UserAnswers, Call] =
      Table(
        ("Id", "Next Page", "UserAnswers (Optional)"),
        row(EstablisherDOBId(index))(cyaDetailsRoute(index, NormalMode), Some(detailsUa.set(EstablisherDOBId(index), LocalDate.parse("2000-01-01")).success.value)),
        row(EstablisherHasNINOId(index))(enterNationaInsuranceNumberRoute(index, CheckMode), Some(detailsUa.set(EstablisherHasNINOId(index), true).success.value)),
        row(EstablisherHasNINOId(index))(reasonForNoNationalInsuranceNumberRoute(index, CheckMode), Some(detailsUa.set(EstablisherHasNINOId(index), false).success.value)),
        row(EstablisherNINOId(index))(cyaDetailsRoute(index, NormalMode), Some(detailsUa.set(EstablisherNINOId(index), ReferenceValue("AB123456C")).success.value)),
        row(EstablisherNoNINOReasonId(index))(cyaDetailsRoute(index, NormalMode), Some(detailsUa.set(EstablisherNoNINOReasonId(index), "Reason").success.value)),
        row(EstablisherHasUTRId(index))(enterUniqueTaxpayerReferenceRoute(index, CheckMode), Some(detailsUa.set(EstablisherHasUTRId(index), true).success.value)),
        row(EstablisherHasUTRId(index))(reasonForNoUniqueTaxpayerReferenceRoute(index, CheckMode), Some(detailsUa.set(EstablisherHasUTRId(index), false).success.value)),
        row(EstablisherUTRId(index))(cyaDetailsRoute(index, NormalMode), Some(detailsUa.set(EstablisherUTRId(index), ReferenceValue("1234567890")).success.value)),
        row(EstablisherNoUTRReasonId(index))(cyaDetailsRoute(index, NormalMode), Some(detailsUa.set(EstablisherNoUTRReasonId(index), "Reason").success.value)),
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
