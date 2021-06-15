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
import models.PersonName
import models.establishers.EstablisherKind
import org.scalatest.{MustMatchers, OptionValues, WordSpec}
import utils.{Enumerable, UserAnswers}

class DataCompletionEstablishersSpec extends WordSpec with MustMatchers with OptionValues with Enumerable.Implicits {

  "Establisher Individual completion status should be returned correctly" when {
    "isEstablisherIndividualComplete" must {
      "return true when all answers are present" in {
        val ua = UserAnswers().set(EstablisherKindId(0), EstablisherKind.Individual).flatMap(_.set(EstablisherNameId(0), PersonName("a", "b"))).get
        ua.isEstablisherIndividualComplete(0) mustBe true
      }

      "return false when some answer is missing" in {
        val ua = UserAnswers().set(EstablisherKindId(0), EstablisherKind.Individual).flatMap(
          _.set(EstablisherNameId(0), PersonName("a", "b")).flatMap(
            _.set(EstablisherKindId(1), EstablisherKind.Individual)
          )).get
        ua.isEstablisherIndividualComplete(1) mustBe false
      }
    }
  }
  }
