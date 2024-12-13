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

package helpers

import helpers.cya.BenefitsAndInsuranceCYAHelper
import identifiers.beforeYouStart.{SchemeNameId, SchemeTypeId}
import identifiers.benefitsAndInsurance._
import models.benefitsAndInsurance.{BenefitsProvisionType, BenefitsType}
import models.{Address, SchemeType}
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.Aliases.Text
import utils.Data.schemeName
import utils.UserAnswers

class BenefitsAndInsuranceCYAHelperSpec extends CYAHelperSpecBase {

  val benefitsAndInsuranceCYAHelper = new BenefitsAndInsuranceCYAHelper

  private val insurerName = "test insurer"
  private val insurerPolicyNo = "test"
  private val insurerAddress = Address("addr1", "addr2", None, None, Some("ZZ11ZZ"), "GB")


  // scalastyle:off magic.number
  "BenefitsAndInsuranceCYAHelper" must {
    "return all rows with correct change link, value and visually hidden text" in {
      val ua: UserAnswers = UserAnswers().setOrException(SchemeNameId, schemeName)
        .setOrException(SchemeTypeId, SchemeType.Other("other")).setOrException(IsInvestmentRegulatedId, true)
        .setOrException(IsOccupationalId, true)
        .setOrException(HowProvideBenefitsId, BenefitsProvisionType.MoneyPurchaseOnly)
        .setOrException(BenefitsTypeId, BenefitsType.CashBalanceBenefits).setOrException(AreBenefitsSecuredId, true)
        .setOrException(BenefitsInsuranceNameId, insurerName).setOrException(BenefitsInsurancePolicyId, insurerPolicyNo)
        .setOrException(InsurerAddressId, insurerAddress)

      val result = benefitsAndInsuranceCYAHelper.rows(dataRequest(ua), messages)

      result.head mustBe summaryListRow(key = Messages("isInvestmentRegulated.h1", schemeName),
        valueMsgKey = "booleanAnswer.true")
      result(1) mustBe summaryListRow(key = Messages("isOccupational.h1", schemeName),
        valueMsgKey = "booleanAnswer.true")

      result(2) mustBe summaryListRow(key = Messages("howProvideBenefits.h1", schemeName),
        valueMsgKey = "howProvideBenefits.moneyPurchaseOnly", Some(Link(text = Messages("site.change"),
          target = controllers.benefitsAndInsurance.routes.HowProvideBenefitsController.onPageLoad.url,
          visuallyHiddenText = Some(Text(Messages("site.change") + " " + Messages("howProvideBenefits.visuallyHidden", schemeName))),
          attributes = Map("id" -> "cya-0-2-change"))))

      result(3) mustBe summaryListRow(key = Messages("benefitsType.h1", schemeName),
        valueMsgKey = "benefitsType.cashBalanceBenefits", Some(Link(text = Messages("site.change"),
          target = controllers.benefitsAndInsurance.routes.BenefitsTypeController.onPageLoad.url,
          visuallyHiddenText = Some(Text(Messages("site.change") + " " + Messages("benefitsType.visuallyHidden", schemeName))),
          attributes = Map("id" -> "cya-0-3-change"))))





      result(4) mustBe summaryListRow(key = Messages("areBenefitsSecured.title"), valueMsgKey = "booleanAnswer.true",
        Some(Link(text = Messages("site.change"),
          target = controllers.benefitsAndInsurance.routes.AreBenefitsSecuredController.onPageLoad.url,
          visuallyHiddenText = Some(Text(Messages("site.change") + " " + Messages("areBenefitsSecured.visuallyHidden"))),
          attributes = Map("id" -> "cya-0-4-change"))))

      result(5) mustBe summaryListRowText(key = Messages("benefitsInsuranceName.title"), value = insurerName, Some(
        Link(text = Messages("site.change"),
          target = controllers.benefitsAndInsurance.routes.BenefitsInsuranceNameController.onPageLoad.url,
          visuallyHiddenText = Some(Text(Messages("site.change") + " " + Messages("benefitsInsuranceName.visuallyHidden"))),
          attributes = Map("id" -> "cya-0-5-change"))))

      result(6) mustBe summaryListRowText(key = Messages("benefitsInsurancePolicy.h1", insurerName),
        value = insurerPolicyNo, Some(Link(text = Messages("site.change"),
          target = controllers.benefitsAndInsurance.routes.BenefitsInsurancePolicyController.onPageLoad.url,
          visuallyHiddenText = Some(Text(Messages("site.change") + " " + Messages("benefitsInsurancePolicy.visuallyHidden"))),
          attributes = Map("id" -> "cya-0-6-change"))))

      result(7) mustBe summaryListRowHtml(key = messages("addressList.title", insurerName),
        value = answerAddressTransform(insurerAddress), Some(Link(text = Messages("site.change"),
          target = controllers.benefitsAndInsurance.routes.InsurerEnterPostcodeController.onPageLoad.url,
          visuallyHiddenText = Some(Text(Messages("site.change") + " " + Messages("addressList.visuallyHidden"))),
          attributes = Map("id" -> "cya-0-7-change"))))
    }
  }
}
