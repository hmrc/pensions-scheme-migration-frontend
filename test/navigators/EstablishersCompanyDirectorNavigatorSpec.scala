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
import controllers.establishers.company.director.details
import identifiers.establishers.company.director.address._
import identifiers.establishers.company.director.contact.{EnterEmailId, EnterPhoneId}
import identifiers.establishers.company.director.details._
import identifiers.establishers.company.director.{ConfirmDeleteDirectorId, DirectorNameId, TrusteeAlsoDirectorId, TrusteesAlsoDirectorsId}
import identifiers.{Identifier, TypedIdentifier}
import models._
import org.scalatest.TryValues
import org.scalatest.prop.TableFor3
import play.api.libs.json.Writes
import play.api.mvc.Call
import utils.Data.ua
import utils.{Data, Enumerable, UserAnswers}

import java.time.LocalDate

class EstablishersCompanyDirectorNavigatorSpec
  extends SpecBase
    with NavigatorBehaviour
    with Enumerable.Implicits
    with TryValues {

  private val navigator: CompoundNavigator = injector.instanceOf[CompoundNavigator]
  private val establisherIndex: Index = Index(0)
  private val directorIndex: Index = Index(0)
  private val directorDOBPage: Call = controllers.establishers.company.director.details.routes.DirectorDOBController.onPageLoad(establisherIndex,directorIndex,NormalMode)

  private val detailsUa: UserAnswers =
    ua.set(DirectorNameId(establisherIndex ,directorIndex), PersonName("Jane", "Doe")).success.value
  private def hasNinoPage(mode: Mode): Call =
    details.routes.DirectorHasNINOController.onPageLoad(establisherIndex,directorIndex, mode)
  private def enterNinoPage(mode: Mode): Call =
    details.routes.DirectorEnterNINOController.onPageLoad(establisherIndex,directorIndex, mode)
  private def noNinoPage(mode: Mode): Call =
    details.routes.DirectorNoNINOReasonController.onPageLoad(establisherIndex,directorIndex, mode)
  private def hasUtrPage(mode: Mode): Call =
    details.routes.DirectorHasUTRController.onPageLoad(establisherIndex,directorIndex, mode)
  private def enterUtrPage(mode: Mode): Call =
    details.routes.DirectorEnterUTRController.onPageLoad(establisherIndex,directorIndex, mode)
  private def noUtrPage(mode: Mode): Call =
    details.routes.DirectorNoUTRReasonController.onPageLoad(establisherIndex,directorIndex, mode)
  private val cya: Call =
    details.routes.CheckYourAnswersController.onPageLoad(establisherIndex,directorIndex)

  private def addressUAWithValue[A](idType:TypedIdentifier[A], idValue:A)(implicit writes: Writes[A]) =
    detailsUa.set(idType, idValue).toOption

  private val seqAddresses = Seq(
    TolerantAddress(Some("1"),Some("1"),Some("c"),Some("d"), Some("zz11zz"), Some("GB")),
    TolerantAddress(Some("2"),Some("2"),Some("c"),Some("d"), Some("zz11zz"), Some("GB")),
  )

  val address = Address("addr1", "addr2", None, None, Some("ZZ11ZZ"), "GB")
  /*private val johnDoe = PersonName("John", "Doe")
  private def validData(directors: PersonName*): JsObject = {
    Json.obj(
      EstablishersId.toString -> Json.arr(
        Json.obj(
          CompanyDetailsId.toString -> CompanyDetails("test company name"),
          "director" -> directors.map(d => Json.obj(DirectorNameId.toString -> Json.toJson(d)))
        )
      )
    )
  }*/

  private def addAddCompanyDirectorsPage(establisherIndex:Int,mode:Mode): Call = controllers.establishers.company.routes.AddCompanyDirectorsController.onPageLoad(establisherIndex,mode)
  private def postcode(establisherIndex:Int,directorIndex:Int,mode: Mode): Call = controllers.establishers.company.director.address.routes.EnterPostcodeController.onPageLoad(establisherIndex, directorIndex, mode)
  private def enterPreviousPostcode(estruetablisherIndex:Int,directorIndex:Int,mode:Mode): Call =
    controllers.establishers.company.director.address.routes.EnterPreviousPostcodeController.onPageLoad(establisherIndex,directorIndex,mode)

  private def selectAddress(establisherIndex:Int,directorIndex:Int,mode:Mode): Call =
    controllers.establishers.company.director.address.routes.SelectAddressController.onPageLoad(establisherIndex,directorIndex,mode)

  private def selectPreviousAddress(establisherIndex:Int,directorIndex:Int,mode:Mode): Call =
    controllers.establishers.company.director.address.routes.SelectPreviousAddressController.onPageLoad(establisherIndex,directorIndex,mode)

  private def addressYears(establisherIndex:Int,directorIndex:Int,mode:Mode): Call =
    controllers.establishers.company.director.address.routes.AddressYearsController.onPageLoad(establisherIndex,directorIndex,mode)

  private def enterPhonePage(establisherIndex:Int,directorIndex:Int,mode:Mode): Call =
    controllers.establishers.company.director.contact.routes.EnterPhoneNumberController.onPageLoad(establisherIndex,directorIndex,mode)

  private def enterEmailPage(establisherIndex:Int,directorIndex:Int,mode:Mode): Call =
    controllers.establishers.company.director.contact.routes.EnterEmailController.onPageLoad(establisherIndex,directorIndex,mode)

  private def directorNamePage(establisherIndex:Int, directorIndex:Int, mode:Mode): Call =
    controllers.establishers.company.director.routes.DirectorNameController.onPageLoad(establisherIndex,directorIndex,mode)

  private def whatYouWillNeedPage(establisherIndex:Int): Call =
    controllers.establishers.company.director.details.routes.WhatYouWillNeedController.onPageLoad(establisherIndex)

  "EstablishersCompanyDirectorNavigator" when {
    def navigation: TableFor3[Identifier, UserAnswers, Call] =
      Table(
        ("Id", "Next Page", "UserAnswers (Optional)"),
        row(DirectorNameId(establisherIndex,directorIndex))(directorDOBPage),
        row(DirectorDOBId(establisherIndex,directorIndex))(hasNinoPage(NormalMode),Some(detailsUa.set(DirectorDOBId(establisherIndex,directorIndex), LocalDate.parse("2000-01-01")).success.value)),
        row(DirectorHasNINOId(establisherIndex,directorIndex))(enterNinoPage(NormalMode), Some(detailsUa.set(DirectorHasNINOId(establisherIndex,directorIndex), true).success.value)),
        row(DirectorHasNINOId(establisherIndex,directorIndex))(noNinoPage(NormalMode), Some(detailsUa.set(DirectorHasNINOId(establisherIndex,directorIndex), false).success.value)),
        row(DirectorNINOId(establisherIndex,directorIndex))(hasUtrPage(NormalMode), Some(detailsUa.set(DirectorNINOId(establisherIndex,directorIndex), ReferenceValue("AB123456C")).success.value)),
        row(DirectorNoNINOReasonId(establisherIndex,directorIndex))(hasUtrPage(NormalMode), Some(detailsUa.set(DirectorNoNINOReasonId(establisherIndex,directorIndex), "Reason").success.value)),
        row(DirectorHasUTRId(establisherIndex,directorIndex))(enterUtrPage(NormalMode), Some(detailsUa.set(DirectorHasUTRId(establisherIndex,directorIndex), true).success.value)),
        row(DirectorHasUTRId(establisherIndex,directorIndex))(noUtrPage(NormalMode), Some(detailsUa.set(DirectorHasUTRId(establisherIndex,directorIndex), false).success.value)),
        row(DirectorEnterUTRId(establisherIndex,directorIndex))(postcode(establisherIndex,directorIndex,NormalMode), Some(detailsUa.set(DirectorEnterUTRId(establisherIndex,directorIndex), ReferenceValue("1234567890")).success.value)),
        row(DirectorNoUTRReasonId(establisherIndex,directorIndex))(postcode(establisherIndex,directorIndex,NormalMode), Some(detailsUa.set(DirectorNoUTRReasonId(establisherIndex,directorIndex), "Reason").success.value)),
        row(AddressId(establisherIndex,directorIndex))(addressYears(establisherIndex,directorIndex,NormalMode), addressUAWithValue(AddressId(establisherIndex,directorIndex), address)),
        row(AddressListId(establisherIndex,directorIndex))(addressYears(establisherIndex,directorIndex,NormalMode), addressUAWithValue(AddressListId(establisherIndex,directorIndex), Data.tolerantAddress)),
        row(AddressYearsId(establisherIndex,directorIndex))(enterEmailPage(establisherIndex,directorIndex,NormalMode), addressUAWithValue(AddressYearsId(establisherIndex,directorIndex), true)),
        row(AddressYearsId(establisherIndex,directorIndex))(enterPreviousPostcode(establisherIndex,directorIndex,NormalMode), addressUAWithValue(AddressYearsId(establisherIndex,directorIndex), false)),
        row(EnterPostCodeId(establisherIndex,directorIndex))(selectAddress(establisherIndex,directorIndex,NormalMode), addressUAWithValue(EnterPostCodeId(establisherIndex,directorIndex), seqAddresses)),
        row(EnterPreviousPostCodeId(establisherIndex,directorIndex))(selectPreviousAddress(establisherIndex,directorIndex,NormalMode), addressUAWithValue(EnterPreviousPostCodeId(establisherIndex,directorIndex), seqAddresses)),
        row(PreviousAddressListId(establisherIndex,directorIndex))(enterEmailPage(establisherIndex,directorIndex,NormalMode), addressUAWithValue(PreviousAddressListId(establisherIndex,directorIndex), Data.tolerantAddress)),
        row(PreviousAddressId(establisherIndex,directorIndex))(enterEmailPage(establisherIndex,directorIndex,NormalMode), addressUAWithValue(PreviousAddressId(establisherIndex,directorIndex), address)),
        row(EnterEmailId(establisherIndex,directorIndex))(enterPhonePage(establisherIndex,directorIndex,NormalMode), Some(detailsUa.set(EnterEmailId(establisherIndex,directorIndex), "test@test.com").success.value)),
        row(EnterPhoneId(establisherIndex,directorIndex))(cya, Some(detailsUa.set(EnterPhoneId(establisherIndex,directorIndex), "1234").success.value)),
        row(ConfirmDeleteDirectorId(directorIndex))(addAddCompanyDirectorsPage(directorIndex,NormalMode), Some(detailsUa.set(ConfirmDeleteDirectorId(directorIndex),true).success.value)),
        row(TrusteeAlsoDirectorId(establisherIndex))(directorNamePage(establisherIndex, directorIndex = 1,NormalMode), Some(detailsUa.set(TrusteeAlsoDirectorId(directorIndex), 11).success.value)),
        row(TrusteeAlsoDirectorId(establisherIndex))(whatYouWillNeedPage(establisherIndex), Some(ua.set(TrusteeAlsoDirectorId(directorIndex), value = 11).success.value)),
        row(TrusteeAlsoDirectorId(establisherIndex))(addAddCompanyDirectorsPage(directorIndex,NormalMode), Some(ua.set(TrusteeAlsoDirectorId(directorIndex), value = 2).success.value)),
        row(TrusteesAlsoDirectorsId(establisherIndex))(directorNamePage(establisherIndex, directorIndex = 1,NormalMode), Some(detailsUa.set(TrusteesAlsoDirectorsId(directorIndex), Seq(11)).success.value)),
        row(TrusteesAlsoDirectorsId(establisherIndex))(whatYouWillNeedPage(establisherIndex), Some(ua.set(TrusteesAlsoDirectorsId(directorIndex), value = Seq(11)).success.value)),
        row(TrusteesAlsoDirectorsId(establisherIndex))(addAddCompanyDirectorsPage(directorIndex,NormalMode), Some(ua.set(TrusteesAlsoDirectorsId(directorIndex), value = Seq(2)).success.value))
      )

    def editNavigation: TableFor3[Identifier, UserAnswers, Call] =
      Table(
        ("Id", "Next Page", "UserAnswers (Optional)"),
        row(DirectorNameId(establisherIndex,directorIndex))(cya, Some(detailsUa.set(DirectorNameId(establisherIndex,directorIndex),PersonName("Jane", "Doe")).success.value)),
        row(DirectorDOBId(establisherIndex,directorIndex))(cya, Some(detailsUa.set(DirectorDOBId(establisherIndex,directorIndex), LocalDate.parse("2000-01-01")).success.value)),
        row(DirectorHasNINOId(establisherIndex,directorIndex))(enterNinoPage(CheckMode), Some(detailsUa.set(DirectorHasNINOId(establisherIndex,directorIndex), true).success.value)),
        row(DirectorHasNINOId(establisherIndex,directorIndex))(noNinoPage(CheckMode), Some(detailsUa.set(DirectorHasNINOId(establisherIndex,directorIndex), false).success.value)),
        row(DirectorNINOId(establisherIndex,directorIndex))(cya, Some(detailsUa.set(DirectorNINOId(establisherIndex,directorIndex), ReferenceValue("AB123456C")).success.value)),
        row(DirectorNoNINOReasonId(establisherIndex,directorIndex))(cya, Some(detailsUa.set(DirectorNoNINOReasonId(establisherIndex,directorIndex), "Reason").success.value)),
        row(DirectorHasUTRId(establisherIndex,directorIndex))(enterUtrPage(CheckMode), Some(detailsUa.set(DirectorHasUTRId(establisherIndex,directorIndex), true).success.value)),
        row(DirectorHasUTRId(establisherIndex,directorIndex))(noUtrPage(CheckMode), Some(detailsUa.set(DirectorHasUTRId(establisherIndex,directorIndex), false).success.value)),
        row(DirectorEnterUTRId(establisherIndex,directorIndex))(cya, Some(detailsUa.set(DirectorEnterUTRId(establisherIndex,directorIndex), ReferenceValue("1234567890")).success.value)),
        row(DirectorNoUTRReasonId(establisherIndex,directorIndex))(cya, Some(detailsUa.set(DirectorNoUTRReasonId(establisherIndex,directorIndex), "Reason").success.value)),
        row(AddressId(establisherIndex,directorIndex))(cya, addressUAWithValue(AddressId(establisherIndex,directorIndex), address)),
        row(AddressListId(establisherIndex,directorIndex))(cya, addressUAWithValue(AddressListId(establisherIndex,directorIndex), Data.tolerantAddress)),
        row(AddressYearsId(establisherIndex,directorIndex))(cya, addressUAWithValue(AddressYearsId(establisherIndex,directorIndex), true)),
        row(AddressYearsId(establisherIndex,directorIndex))(enterPreviousPostcode(establisherIndex,directorIndex,CheckMode), addressUAWithValue(AddressYearsId(establisherIndex,directorIndex), false)),
        row(EnterPostCodeId(establisherIndex,directorIndex))(selectAddress(establisherIndex,directorIndex,CheckMode), addressUAWithValue(EnterPostCodeId(establisherIndex,directorIndex), seqAddresses)),
        row(EnterPreviousPostCodeId(establisherIndex,directorIndex))(selectPreviousAddress(establisherIndex,directorIndex,CheckMode), addressUAWithValue(EnterPreviousPostCodeId(establisherIndex,directorIndex), seqAddresses)),
        row(PreviousAddressListId(establisherIndex,directorIndex))(cya, addressUAWithValue(PreviousAddressListId(establisherIndex,directorIndex), Data.tolerantAddress)),
        row(PreviousAddressId(establisherIndex,directorIndex))(cya, addressUAWithValue(PreviousAddressId(establisherIndex,directorIndex), address)),
        row(EnterEmailId(establisherIndex,directorIndex))(cya, Some(detailsUa.set(EnterEmailId(establisherIndex,directorIndex), "test@test.com").success.value)),
        row(EnterPhoneId(establisherIndex,directorIndex))(cya, Some(detailsUa.set(EnterPhoneId(establisherIndex,directorIndex), "1234").success.value))
      )

    "in NormalMode" must {
      behave like navigatorWithRoutesForMode(NormalMode)(navigator, navigation)
    }

    "CheckMode" must {
      behave like navigatorWithRoutesForMode(CheckMode)(navigator, editNavigation)
    }
  }
}
