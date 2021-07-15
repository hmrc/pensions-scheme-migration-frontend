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

import identifiers.trustees.individual.contact.{EnterEmailId, EnterPhoneId}
import identifiers.trustees.individual.details._
import models.ReferenceValue
import org.scalatest.{MustMatchers, OptionValues, TryValues, WordSpec}
import utils.{Enumerable, UserAnswers}

import java.time.LocalDate

class DataCompletionTrusteesSpec
  extends WordSpec
    with MustMatchers
    with OptionValues
    with TryValues
    with Enumerable.Implicits {

  "Trustee Individual completion status should be returned correctly" when {

    "isTrusteeIndividualDetailsCompleted" must {
      "return true when all answers are present" in {
        val ua =
          UserAnswers()
            .set(TrusteeDOBId(0), LocalDate.parse("2001-01-01")).success.value

        val ua1 =
          ua
            .set(TrusteeHasNINOId(0), true).success.value
            .set(TrusteeNINOId(0), ReferenceValue("AB123456C")).success.value
            .set(TrusteeHasUTRId(0), true).success.value
            .set(TrusteeUTRId(0), ReferenceValue("1234567890")).success.value

        val ua2 =
          ua
            .set(TrusteeHasNINOId(0), false).success.value
            .set(TrusteeNoNINOReasonId(0), "Reason").success.value
            .set(TrusteeHasUTRId(0), false).success.value
            .set(TrusteeNoUTRReasonId(0), "Reason").success.value

        ua1.isTrusteeIndividualDetailsCompleted(0) mustBe true
        ua2.isTrusteeIndividualDetailsCompleted(0) mustBe true
      }

      "return false when some answer is missing" in {
        val ua =
          UserAnswers()
            .set(TrusteeDOBId(0), LocalDate.parse("2001-01-01")).success.value

        ua.isTrusteeIndividualDetailsCompleted(0) mustBe false

      }
    }

    "isTrusteeIndividualContactDetailsCompleted" must {
      "return true when all answers are present" in {
        val ua =
          UserAnswers()
            .set(EnterEmailId(0), "test@test.com").success.value
            .set(EnterPhoneId(0), "123").success.value

        ua.isTrusteeIndividualContactDetailsCompleted(0).value mustBe true
        ua.isTrusteeIndividualContactDetailsCompleted(0).value mustBe true
      }

      "return false when some answer is missing" in {
        val ua =
          UserAnswers()
            .set(EnterEmailId(0), "test@test.com").success.value

        ua.isTrusteeIndividualContactDetailsCompleted(0).value mustBe false
      }

      "return None when no answer is present" in {
        UserAnswers().isEstablisherIndividualContactDetailsCompleted(0) mustBe None
      }
    }
  }
}