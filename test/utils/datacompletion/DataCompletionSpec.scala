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

package utils.datacompletion

import identifiers.beforeYouStart.{SchemeNameId, WorkingKnowledgeId}
import identifiers.benefitsAndInsurance.{AreBenefitsSecuredId, BenefitsInsurancePolicyId, BenefitsInsuranceNameId, InsurerAddressId, BenefitsTypeId, HowProvideBenefitsId, IsInvestmentRegulatedId, IsOccupationalId}
import models.benefitsAndInsurance.{BenefitsType, BenefitsProvisionType}
import org.scalatest.{OptionValues, MustMatchers, WordSpec}
import utils.Data.{ua, insurerName, insurerPolicyNo, insurerAddress}
import utils.{UserAnswers, Enumerable}

class DataCompletionSpec extends WordSpec with MustMatchers with OptionValues with Enumerable.Implicits {

  private val uaBenefitsSection = ua
    .setOrException(IsInvestmentRegulatedId, true)
    .setOrException(IsOccupationalId, true)
    .setOrException(AreBenefitsSecuredId, true)
    .setOrException(BenefitsInsuranceNameId, insurerName)
    .setOrException(BenefitsInsurancePolicyId, insurerPolicyNo)
    .setOrException(InsurerAddressId, insurerAddress)

  "All generic methods" when {
    "isComplete" must {
      "return Some(true) only when all values in list are true" in {
        UserAnswers().isComplete(Seq(Some(true), Some(true), Some(true))) mustBe Some(true)
        UserAnswers().isComplete(Seq(Some(true), Some(false), Some(true))) mustBe Some(false)
      }

      "return None when only all values in list are true" in {
        UserAnswers().isComplete(Seq(None, None, None, None)) mustBe None
        UserAnswers().isComplete(Seq(None, Some(false), Some(true))) mustBe Some(false)
      }

      "return false in every other case" in {
        UserAnswers().isComplete(Seq(Some(true), None, Some(false), None)) mustBe Some(false)
        UserAnswers().isComplete(Seq(None, Some(true), Some(true))) mustBe Some(false)
      }
    }

    "isListComplete" must {
      "return true only when all values in list are true" in {
        UserAnswers().isListComplete(Seq(true, true, true)) mustBe true
      }

      "return false in every other case" in {
        UserAnswers().isListComplete(Seq(true, false, true)) mustBe false
      }
    }

    "isAnswerComplete" must {
      "return None when answer is missing" in {
        UserAnswers().isAnswerComplete(WorkingKnowledgeId) mustBe None
      }

      "return Some(true) when answer is present" in {
        ua.isAnswerComplete(SchemeNameId) mustBe Some(true)
      }
    }

    "isBenefitsAndInsuranceCompleted" must {
      "return Some(true) when defined benefits chosen and no benefits type chosen" in {
        uaBenefitsSection
          .setOrException(HowProvideBenefitsId, BenefitsProvisionType.DefinedBenefitsOnly)
          .isBenefitsAndInsuranceCompleted mustBe Some(true)
      }

      "return Some(false) when defined benefits NOT chosen and no benefits type chosen" in {
        uaBenefitsSection
          .setOrException(HowProvideBenefitsId, BenefitsProvisionType.MoneyPurchaseOnly)
          .isBenefitsAndInsuranceCompleted mustBe Some(false)
      }

      "return Some(true) when defined benefits NOT chosen and a benefits type chosen" in {
        uaBenefitsSection
          .setOrException(HowProvideBenefitsId, BenefitsProvisionType.MoneyPurchaseOnly)
          .setOrException(BenefitsTypeId, BenefitsType.CashBalanceBenefits)
          .isBenefitsAndInsuranceCompleted mustBe Some(true)
      }

    }

  }
}
