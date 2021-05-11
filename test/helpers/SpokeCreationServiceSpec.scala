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

package helpers

import identifiers.beforeYouStart.{EstablishedCountryId, SchemeTypeId, WorkingKnowledgeId}
import models.{EntitySpoke, _}
import org.scalatest.{MustMatchers, OptionValues, WordSpec}
import utils.Data.{schemeName, ua}
import viewmodels.Message

class SpokeCreationServiceSpec
  extends WordSpec
    with MustMatchers
    with OptionValues {

  val spokeCreationService = new SpokeCreationService()

  "getBeforeYouStartSpoke" must {

    "display the spoke with link to cya page with complete status if the spoke is completed" in {
      val userAnswers = ua.set(SchemeTypeId, SchemeType.SingleTrust).get
        .set(WorkingKnowledgeId, true).get
        .set(EstablishedCountryId, "GB").get

      val expectedSpoke = Seq(EntitySpoke(TaskListLink(Message("messages__schemeTaskList__before_you_start_link_text", schemeName),
        controllers.beforeYouStartSpoke.routes.CheckYourAnswersController.onPageLoad.url), Some(true)))

      val result = spokeCreationService.getBeforeYouStartSpoke(userAnswers, schemeName)
      result mustBe expectedSpoke
    }
  }


}