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
import controllers.establishers.company.details.{routes => detailsRoutes}
import controllers.establishers.company.address.{routes => addressRoutes}
import identifiers.{Identifier, TypedIdentifier}
import identifiers.establishers.company.CompanyDetailsId
import identifiers.establishers.company.address._
import identifiers.establishers.company.details._
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

  private def companyNumber(mode: Mode = NormalMode): Call = detailsRoutes.CompanyNumberController.onPageLoad(index, mode)
  private def noCompanyNumber(mode: Mode = NormalMode): Call = detailsRoutes.NoCompanyNumberReasonController.onPageLoad(index, mode)
  private def haveUtr(mode: Mode = NormalMode): Call = detailsRoutes.HaveUTRController.onPageLoad(index, mode)
  private def utr(mode: Mode = NormalMode): Call = detailsRoutes.UTRController.onPageLoad(index, mode)
  private def noUtr(mode: Mode = NormalMode): Call = detailsRoutes.NoUTRReasonController.onPageLoad(index, mode)
  private def haveVat(mode: Mode = NormalMode): Call = detailsRoutes.HaveVATController.onPageLoad(index, mode)
  private def vat(mode: Mode = NormalMode): Call = detailsRoutes.VATController.onPageLoad(index, mode)
  private def havePaye(mode: Mode = NormalMode): Call = detailsRoutes.HavePAYEController.onPageLoad(index, mode)
  private def paye(mode: Mode = NormalMode): Call = detailsRoutes.PAYEController.onPageLoad(index, mode)
  private val cyaDetails: Call = detailsRoutes.CheckYourAnswersController.onPageLoad(index)

  private val cyaAddress: Call = addressRoutes.CheckYourAnswersController.onPageLoad(index)

  private def enterPreviousPostcode: Call = addressRoutes.EnterPreviousPostcodeController.onPageLoad(index)

  private def tradingTime: Call = addressRoutes.TradingTimeController.onPageLoad(index)

  private def selectAddress: Call = addressRoutes.SelectAddressController.onPageLoad(index)

  private def selectPreviousAddress: Call = addressRoutes.SelectPreviousAddressController.onPageLoad(index)

  private def addressYears: Call = addressRoutes.AddressYearsController.onPageLoad(index)

  "EstablishersCompanyNavigator" when {
    def navigation: TableFor3[Identifier, UserAnswers, Call] =
      Table(
        ("Id", "Next Page", "UserAnswers (Optional)"),
         row(CompanyDetailsId(index))(addEstablisherPage),
        row(HaveCompanyNumberId(index))(companyNumber(), addressUAWithValue(HaveCompanyNumberId(index), true)),
        row(HaveCompanyNumberId(index))(noCompanyNumber(), addressUAWithValue(HaveCompanyNumberId(index), false)),
        row(CompanyNumberId(index))(haveUtr()),
        row(NoCompanyNumberReasonId(index))(haveUtr()),
        row(HaveUTRId(index))(utr(), addressUAWithValue(HaveUTRId(index), true)),
        row(HaveUTRId(index))(noUtr(), addressUAWithValue(HaveUTRId(index), false)),
        row(CompanyUTRId(index))(haveVat()),
        row(NoUTRReasonId(index))(haveVat()),
        row(HaveVATId(index))(vat(), addressUAWithValue(HaveVATId(index), true)),
        row(HaveVATId(index))(havePaye(), addressUAWithValue(HaveVATId(index), false)),
        row(VATId(index))(havePaye()),
        row(HavePAYEId(index))(paye(), addressUAWithValue(HavePAYEId(index), true)),
        row(HavePAYEId(index))(cyaDetails, addressUAWithValue(HavePAYEId(index), false)),
        row(PAYEId(index))(cyaDetails),
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

      )

    def editNavigation: TableFor3[Identifier, UserAnswers, Call] =
      Table(
        ("Id", "Next Page", "UserAnswers (Optional)"),
        row(CompanyDetailsId(index))(controllers.routes.IndexController.onPageLoad()),
        row(HaveCompanyNumberId(index))(companyNumber(CheckMode), addressUAWithValue(HaveCompanyNumberId(index), true)),
        row(HaveCompanyNumberId(index))(noCompanyNumber(CheckMode), addressUAWithValue(HaveCompanyNumberId(index), false)),
        row(CompanyNumberId(index))(cyaDetails),
        row(NoCompanyNumberReasonId(index))(cyaDetails),
        row(HaveUTRId(index))(utr(CheckMode), addressUAWithValue(HaveUTRId(index), true)),
        row(HaveUTRId(index))(noUtr(CheckMode), addressUAWithValue(HaveUTRId(index), false)),
        row(CompanyUTRId(index))(cyaDetails),
        row(NoUTRReasonId(index))(cyaDetails),
        row(HaveVATId(index))(vat(CheckMode), addressUAWithValue(HaveVATId(index), true)),
        row(HaveVATId(index))(havePaye(CheckMode), addressUAWithValue(HaveVATId(index), false)),
        row(VATId(index))(cyaDetails),
        row(HavePAYEId(index))(paye(CheckMode), addressUAWithValue(HavePAYEId(index), true)),
        row(HavePAYEId(index))(cyaDetails, addressUAWithValue(HavePAYEId(index), false)),
        row(PAYEId(index))(cyaDetails),
      )

    "in NormalMode" must {
      behave like navigatorWithRoutesForMode(NormalMode)(navigator, navigation)
    }

    "CheckMode" must {
      behave like navigatorWithRoutesForMode(CheckMode)(navigator, editNavigation)
    }
  }
}
