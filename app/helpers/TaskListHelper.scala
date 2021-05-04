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

import com.google.inject.Inject
import identifiers.beforeYouStart.SchemeNameId
import utils.UserAnswers
import viewmodels._

class TaskListHelper @Inject()(spokeCreationService: SpokeCreationService) {

  def taskList(answers: UserAnswers): TaskList =
    TaskList(
      answers.get(SchemeNameId).getOrElse(""),
      beforeYouStartSection(answers)
    )

  private[helpers] def beforeYouStartSection(userAnswers: UserAnswers): TaskListEntitySection = {
    TaskListEntitySection(None,
      spokeCreationService.getBeforeYouStartSpoke(userAnswers, userAnswers.get(SchemeNameId).getOrElse("")),
      Some(Message("messages__schemeTaskList__before_you_start_header"))
    )
  }



}


