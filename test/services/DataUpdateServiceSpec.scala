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

package services

import base.SpecBase
import matchers.JsonMatchers
import models.prefill.IndividualDetails
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks.forAll
import utils.{Enumerable, UaJsValueGenerators, UserAnswers}

import java.time.LocalDate

class DataUpdateServiceSpec extends SpecBase with JsonMatchers with Enumerable.Implicits with UaJsValueGenerators {
  private val dataUpdateService = new DataUpdateService()

  "findMatchingTrustee" must {
    "return the trustee which is non deleted and their nino, name and dob matched with the directors" in {
      forAll(uaJsValueWithTrusteeMatching){
        ua => {
          val result = dataUpdateService.findMatchingTrustee(0,0)(UserAnswers(ua)).get
          result mustBe (IndividualDetails("Test", "User 1", false, Some("CS700100A"), Some(LocalDate.parse("1999-01-13")), 0, true, None))
        }
      }
    }

    "return no trustees when their nino, name and dob is not matching with the director" in {
      forAll(uaJsValueWithNoTrusteeMatching) {
        ua => {
          val result = dataUpdateService.findMatchingTrustee(0,0)(UserAnswers(ua))
          result mustBe None
        }
      }
    }
  }

  "findMatchingDirectors" must {
    "return the list of Directors which is non deleted and their nino, name and dob matched with the trustee" in {
      forAll(uaJsValueWithDirectorsMatching){
        ua => {
          val result = dataUpdateService.findMatchingDirectors(0)(UserAnswers(ua))
          result mustBe Seq(IndividualDetails("Test", "User 1", false, Some("CS700100A"), Some(LocalDate.parse("1999-01-13")), 0, true, mainIndex = Some(0)))
        }
      }
    }

    "return empty list when their nino, name and dob is not matching with the trustee" in {
      forAll(uaJsValueWithNoDirectorMatching) {
        ua => {
          val result = dataUpdateService.findMatchingDirectors(0)(UserAnswers(ua))
          result mustBe Nil
        }
      }
    }
  }
}





