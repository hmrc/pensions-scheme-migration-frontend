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

package helpers.spokes.trustees.individual

import controllers.trustees.individual.address.routes.{CheckYourAnswersController, WhatYouWillNeedController}
import helpers.spokes.Spoke
import models.{Index, TaskListLink}
import play.api.i18n.Messages
import utils.UserAnswers


case class TrusteeIndividualAddress(
  index: Index,
  answers: UserAnswers
) extends Spoke {
  val messageKeyPrefix = "messages__schemeTaskList__trusteeIndividualAddress_"

  val linkKeyAndRoute: (String, String) = {
    if (completeFlag(answers).isDefined)
      (s"${messageKeyPrefix}changeLink", CheckYourAnswersController.onPageLoad(index).url)
    else
      (s"${messageKeyPrefix}addLink", WhatYouWillNeedController.onPageLoad(index).url)
  }

  override def changeLink(name: String)
    (implicit messages: Messages): TaskListLink =
    TaskListLink(
      text = Messages(linkKeyAndRoute._1, name),
      target = linkKeyAndRoute._2,
      visuallyHiddenText = None
    )

  override def completeFlag(answers: UserAnswers): Option[Boolean] =
    answers.isTrusteeIndividualAddressCompleted(index, answers)
}

