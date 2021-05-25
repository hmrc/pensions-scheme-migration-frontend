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

  def getSchemeName[A](implicit ua: UserAnswers): String =
    ua.get(SchemeNameId).getOrElse(throw MandatoryAnswerMissingException)

  def taskList(implicit answers: UserAnswers): TaskList =
    TaskList(
      getSchemeName,
      beforeYouStartSection,
      aboutSection
    )

  private[helpers] def beforeYouStartSection(implicit userAnswers: UserAnswers): TaskListEntitySection = {
    TaskListEntitySection(None,
      spokeCreationService.getBeforeYouStartSpoke(userAnswers, getSchemeName),
      Some(Message("messages__schemeTaskList__before_you_start_header"))
    )
  }

  private[helpers] def aboutSection(implicit userAnswers: UserAnswers): TaskListEntitySection = {
    TaskListEntitySection(None,
      spokeCreationService.membershipDetailsSpoke(userAnswers, getSchemeName),
      Some(Message("messages__schemeTaskList__about_scheme_header"))
    )
  }



}


