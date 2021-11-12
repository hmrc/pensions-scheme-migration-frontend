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

package services

import base.SpecBase
import identifiers.beforeYouStart.SchemeNameId
import matchers.JsonMatchers
import models.NewTaskListLink
import org.mockito.MockitoSugar
import org.scalatest.BeforeAndAfterEach
import org.scalatest.TryValues.convertTryToSuccessOrFailure
import org.scalatest.concurrent.ScalaFutures
import uk.gov.hmrc.nunjucks.NunjucksSupport
import utils.Data.schemeName
import utils.UserAnswers

class TaskListServiceSpec extends SpecBase with BeforeAndAfterEach with MockitoSugar with ScalaFutures with NunjucksSupport with JsonMatchers {

  private val mockService = new TaskListService()
  private val basicDetailsSection = Some(NewTaskListLink("Change Test scheme name’s basic details",
    "/add-pension-scheme/check-your-answers-basic-scheme-details", None, false))
  private val membershipDetailsSection = Some(NewTaskListLink("Change Test scheme name’s membership details",
    "/add-pension-scheme/check-your-answers-members", None, false))
  private val benefitsAndInsuranceDetails = Some(NewTaskListLink("Change Test scheme name’s benefits and insurance details",
    "/add-pension-scheme/check-your-answers-benefits-insurance", None, false))
  private val establisherSection = Some(NewTaskListLink("Change Test scheme name’s establishers",
    "/add-pension-scheme/establisher/1/establisher-type", None, false))
  private val trusteeSection = Some(NewTaskListLink("Change Test scheme name’s trustees",
    "/add-pension-scheme/trustee/any-trustees", None, false))

  "taskSections " must {

    "return correct links " in {
      val ua: UserAnswers = UserAnswers()
              .set(SchemeNameId, schemeName).success.value
      val expectedTaskSection = Seq(basicDetailsSection, membershipDetailsSection,
        benefitsAndInsuranceDetails, establisherSection, trusteeSection)

      mockService.taskSections(ua, messages) mustBe expectedTaskSection
    }
  }

}




