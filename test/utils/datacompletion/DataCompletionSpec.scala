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

import identifiers.aboutMembership.{CurrentMembersId, FutureMembersId}
import identifiers.beforeYouStart.{EstablishedCountryId, SchemeNameId, SchemeTypeId, WorkingKnowledgeId}
import models.{Members, SchemeType}
import org.scalatest.{MustMatchers, OptionValues, WordSpec}
import utils.Data.ua
import utils.{Enumerable, UserAnswers}

class DataCompletionSpec extends WordSpec with MustMatchers with OptionValues with Enumerable.Implicits {

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

    "isBeforeYouStartCompleted" must {
      "return true when all the answers are complete" in {
        val answers = UserAnswers().set(SchemeNameId, "name").flatMap(
          _.set(SchemeTypeId, SchemeType.SingleTrust).flatMap(
            _.set(EstablishedCountryId, "GB").flatMap(
              _.set(WorkingKnowledgeId, true)
            ))).get
        answers.isBeforeYouStartCompleted mustBe true
      }

      "return false when not all the answers are complete" in {
        val answers = UserAnswers().set(SchemeNameId, "name").flatMap(
          _.set(SchemeTypeId, SchemeType.SingleTrust).flatMap(
            _.set(EstablishedCountryId, "GB"))).get
        answers.isBeforeYouStartCompleted mustBe false
      }

    }

    "isMembersCompleted" must {
      "return true when all the answers are completed" in {
        val answers = UserAnswers().set(CurrentMembersId, Members.One).flatMap(
          _.set(FutureMembersId, Members.One)).get
        answers.isMembersCompleted.value mustBe true
      }

      "return false when all answers not completed" in {
        val answers = UserAnswers().set(CurrentMembersId, Members.One).get
        answers.isMembersCompleted.value mustBe false
      }

      "return None when there is no data" in {
        UserAnswers().isMembersCompleted mustBe None
      }
    }

  }
}
