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

import identifiers.establishers.EstablisherKindId
import identifiers.establishers.individual.EstablisherNameId
import identifiers.establishers.individual.address.{AddressYearsId, PreviousAddressId, AddressId}
import identifiers.establishers.individual.details._
import models.{PersonName, ReferenceValue}
import identifiers.establishers.individual.details._
import models.{PersonName, ReferenceValue}
import models.establishers.EstablisherKind
import org.scalatest.{OptionValues, TryValues, MustMatchers, WordSpec}
import utils.{UserAnswers, Enumerable, Data}
import org.scalatest.{MustMatchers, OptionValues, TryValues, WordSpec}
import utils.{Enumerable, UserAnswers}

import java.time.LocalDate

class DataCompletionEstablishersSpec
  extends WordSpec
    with MustMatchers
    with OptionValues
    with TryValues
    with Enumerable.Implicits {

  "Establisher Individual completion status should be returned correctly" when {
    "isEstablisherIndividualComplete" must {
      "return true when all answers are present" in {
        val ua =
          UserAnswers()
            .set(EstablisherKindId(0), EstablisherKind.Individual).success.value
            .set(EstablisherNameId(0), PersonName("a", "b")).success.value

        ua.isEstablisherIndividualComplete(0) mustBe true
      }

      "return false when some answer is missing" in {
        val ua =
          UserAnswers()
            .set(EstablisherKindId(0), EstablisherKind.Individual).success.value
            .set(EstablisherNameId(0), PersonName("a", "b")).success.value
            .set(EstablisherKindId(1), EstablisherKind.Individual).success.value

        ua.isEstablisherIndividualComplete(1) mustBe false
      }
    }

    "isEstablisherIndividualDetailsCompleted" must {
      "return true when all answers are present" in {
        val ua =
          UserAnswers()
            .set(EstablisherDOBId(0), LocalDate.parse("2001-01-01")).success.value

        val ua1 =
          ua
            .set(EstablisherHasNINOId(0), true).success.value
            .set(EstablisherNINOId(0), ReferenceValue("AB123456C")).success.value
            .set(EstablisherHasUTRId(0), true).success.value
            .set(EstablisherUTRId(0), ReferenceValue("1234567890")).success.value

        val ua2 =
          ua
            .set(EstablisherHasNINOId(0), false).success.value
            .set(EstablisherNoNINOReasonId(0), "Reason").success.value
            .set(EstablisherHasUTRId(0), false).success.value
            .set(EstablisherNoUTRReasonId(0), "Reason").success.value

        ua1.isEstablisherIndividualDetailsCompleted(0, ua1) mustBe true
        ua2.isEstablisherIndividualDetailsCompleted(0, ua2) mustBe true
      }

      "return false when some answer is missing" in {
        val ua =
          UserAnswers()
            .set(EstablisherDOBId(0), LocalDate.parse("2001-01-01")).success.value

        ua.isEstablisherIndividualDetailsCompleted(0, ua) mustBe false

      }
    }

    "isEstablisherIndividualAddressCompleted" must {
      "return true when all answers are present" in {
        val ua1 =
          UserAnswers()
            .setOrException(AddressId(0), Data.address)
            .setOrException(AddressYearsId(0), true)

        val ua2 =
          UserAnswers()
            .setOrException(AddressId(0), Data.address)
            .setOrException(AddressYearsId(0), false)
            .setOrException(PreviousAddressId(0), Data.address)

        ua1.isEstablisherIndividualAddressCompleted(0, ua1) mustBe Some(true)
        ua2.isEstablisherIndividualAddressCompleted(0, ua2) mustBe Some(true)
      }

      "return false when some answer is missing" in {
        val ua1 =
          UserAnswers()
            .setOrException(AddressId(0), Data.address)
            .setOrException(AddressYearsId(0), false)

        val ua2 =
          UserAnswers()
            .setOrException(AddressId(0), Data.address)

        ua1.isEstablisherIndividualAddressCompleted(0, ua1) mustBe Some(false)
        ua2.isEstablisherIndividualAddressCompleted(0, ua2) mustBe Some(false)
      }

      "return None when no answers present" in {
        UserAnswers().isEstablisherIndividualAddressCompleted(0, UserAnswers()) mustBe None
      }
    }
  }
}
