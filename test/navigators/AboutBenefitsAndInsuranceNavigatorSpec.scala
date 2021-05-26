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
import controllers.actions.FakeDataRetrievalAction
import controllers.benefitsAndInsurance.routes._
import identifiers._
import identifiers.benefitsAndInsurance.{IsOccupationalId, HowProvideBenefitsId, IsInvestmentRegulatedId}
import models._
import models.benefitsAndInsurance.BenefitsProvisionType.{DefinedBenefitsOnly, MoneyPurchaseOnly}
import org.scalatest.OptionValues
import org.scalatest.prop.TableFor3
import play.api.libs.json.{JsString, Writes, Json}
import play.api.mvc.Call
import utils.{UserAnswers, Enumerable}

class AboutBenefitsAndInsuranceNavigatorSpec extends SpecBase with NavigatorBehaviour {

  import AboutBenefitsAndInsuranceNavigatorSpec._

  private val navigator: CompoundNavigator = applicationBuilder(
    dataRetrievalAction =
      new FakeDataRetrievalAction(
        dataToReturn = Some(UserAnswers(Json.obj()))
      )
  ).build().injector.instanceOf[CompoundNavigator]

  "AboutBenefitsAndInsuranceNavigator" when {

    "in NormalMode" must {
      def navigation: TableFor3[Identifier, UserAnswers, Call] =
        Table(
          ("Id", "UserAnswers", "Next Page"),
          row(IsInvestmentRegulatedId)(occupationalPension, uaWithValue(IsInvestmentRegulatedId, false)),
          row(IsOccupationalId)(typesofBenefits, uaWithValue(IsOccupationalId, false)),
          row(HowProvideBenefitsId)(benefitsSecured, uaWithValue(HowProvideBenefitsId, DefinedBenefitsOnly)),
          row(HowProvideBenefitsId)(moneyPurchaseBenefits(), uaWithValue(HowProvideBenefitsId, MoneyPurchaseOnly))
     //     row(MoneyPurchaseBenefitsId)(Other, benefitsSecured),
     //     row(BenefitsSecuredByInsuranceId)(false, checkYouAnswers()),
    //      row(BenefitsSecuredByInsuranceId)(true, insuranceCompanyName(NormalMode)),
    //      row(BenefitsInsuranceNameId)(someStringValue, policyNumber()),
    //      row(InsurancePolicyNumberId)(someStringValue, insurerPostcode()),
    //      row(InsurerEnterPostCodeId)(someSeqTolerantAddress, insurerAddressList()),
    //      row(InsurerSelectAddressId)(someTolerantAddress, checkYouAnswers()),
     //     row(InsurerConfirmAddressId)(someAddress, checkYouAnswers())
        )
      behave like navigatorWithRoutesForMode(NormalMode)(navigator, navigation)
    }


  //  "in CheckMode" must {
  //    def navigation: TableFor3[Identifier, UserAnswers, Call] =
  //      Table(
  //        ("Id", "UserAnswers", "Next Page"),
  //        row(IsInvestmentRegulatedId)(checkYouAnswers(), uaWithValue(IsInvestmentRegulatedId, false)),
  //        row(IsOccupationalId)(checkYouAnswers(), uaWithValue(IsOccupationalId, false)),
  //        row(HowProvideBenefitsId)(checkYouAnswers(), uaWithValue(HowProvideBenefitsId, DefinedBenefitsOnly)),
  //        row(HowProvideBenefitsId)(moneyPurchaseBenefits(CheckMode), uaWithValue(HowProvideBenefitsId, MoneyPurchaseOnly)),
  //        //        row(MoneyPurchaseBenefitsId)(CashBalance, checkYouAnswers()),
  ////        row(BenefitsSecuredByInsuranceId)(false, checkYouAnswers()),
  ////        row(BenefitsSecuredByInsuranceId)(true, insuranceCompanyName(CheckMode)),
  ////        row(BenefitsInsuranceNameId)(someStringValue, policyNumber(NormalMode)),
  ////        row(InsurancePolicyNumberId)(someStringValue, checkYouAnswers()),
  ////        row(InsurerConfirmAddressId)(someAddress, checkYouAnswers())
  //      )
  //    behave like navigatorWithRoutesForMode(CheckMode)(navigator, navigation, None)
  //  }

  }

}

object AboutBenefitsAndInsuranceNavigatorSpec extends OptionValues {

  private implicit def writes[A: Enumerable]: Writes[A] = Writes(value => JsString(value.toString))

  private def uaWithValue[A](x:TypedIdentifier[A], y:A)(implicit writes: Writes[A]) = UserAnswers().set(x, y).toOption

  private def occupationalPension: Call                               = IsOccupationalController.onPageLoad(NormalMode)
  private def typesofBenefits: Call                                   = HowProvideBenefitsController.onPageLoad(NormalMode, None)
  private def moneyPurchaseBenefits(mode: Mode = NormalMode): Call    = BenefitsTypeController.onPageLoad(mode, None)
  private def benefitsSecured: Call                                   = AreBenefitsSecuredController.onPageLoad(NormalMode, None)
  private def insuranceCompanyName(mode: Mode): Call                  = BenefitsInsuranceNameController.onPageLoad(mode, None)
//  private def policyNumber(mode: Mode = NormalMode): Call             = InsurancePolicyNumberController.onPageLoad(mode, None)
//  private def insurerPostcode(mode: Mode = NormalMode): Call          = InsurerEnterPostcodeController.onPageLoad(mode, None)
//  private def insurerAddressList(mode: Mode = NormalMode): Call       = InsurerSelectAddressController.onPageLoad(mode, None)
//  private def checkYouAnswers(mode: Mode = NormalMode): Call          = CheckYourAnswersBenefitsAndInsuranceController.onPageLoad(mode, None)
}
