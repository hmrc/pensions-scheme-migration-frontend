/*
 * Copyright 2023 HM Revenue & Customs
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

package identifiers

import base.SpecBase
import identifiers.benefitsAndInsurance._
import models.Address
import org.scalatest.OptionValues
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.libs.json.Json
import utils.{Enumerable, UserAnswers}

class AreBenefitsSecuredIdSpec extends SpecBase with Matchers with ScalaCheckPropertyChecks with OptionValues with Enumerable.Implicits {

  "Cleanup" when {

    val answers = UserAnswers(Json.obj())
      .setOrException(AreBenefitsSecuredId, true)
      .setOrException(BenefitsInsuranceNameId, "test")
      .setOrException(BenefitsInsurancePolicyId, "test")
      .setOrException(InsurerEnterPostCodeId, Seq.empty)
      .setOrException(InsurerAddressId, Address("foo", "bar", None, None, None, "GB"))

    "`AreBenefitsSecuredId` is set to `false`" must {

      val result: UserAnswers = answers.setOrException(AreBenefitsSecuredId, false)

      "remove the data for `InsuranceCompanyName`" in {
        result.get(BenefitsInsuranceNameId).isDefined mustBe false
      }

      "remove the data for `InsurancePolicyNumber`" in {
        result.get(BenefitsInsurancePolicyId).isDefined mustBe false
      }

      "remove the data for `InsurerEnterPostCode`" in {
        result.get(InsurerEnterPostCodeId).isDefined mustBe false
      }

      "remove the data for `InsurerConfirmAddress`" in {
        result.get(InsurerAddressId).isDefined mustBe false
      }
    }

    "`AreBenefitsSecuredId` is set to `true`" must {

      val result: UserAnswers = answers.setOrException(AreBenefitsSecuredId, true)

      "not remove the data for `InsuranceCompanyName`" in {
        result.get(BenefitsInsuranceNameId).isDefined mustBe true
      }

      "not remove the data for `InsurancePolicyNumber`" in {
        result.get(BenefitsInsurancePolicyId).isDefined mustBe true
      }

      "not remove the data for `InsurerEnterPostCode`" in {
        result.get(InsurerEnterPostCodeId).isDefined mustBe true
      }

      "not remove the data for `InsurerConfirmAddress`" in {
        result.get(InsurerAddressId).isDefined mustBe true
      }
    }

    "`AreBenefitsSecuredId` is set to `None`" must {
      "not remove any data for answers" in {
        AreBenefitsSecuredId.cleanup(None, answers) mustBe answers
      }
    }
  }
}
