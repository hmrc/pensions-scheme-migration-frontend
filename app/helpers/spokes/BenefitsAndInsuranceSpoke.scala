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

package helpers.spokes

import models.TaskListLink
import play.api.i18n.Messages
import utils.UserAnswers

case object BenefitsAndInsuranceSpoke extends Spoke {

  override def changeLink(name: String)(implicit messages: Messages): TaskListLink =
    TaskListLink(
      messages("messages__schemeTaskList__about_benefits_and_insurance_link_text", name),
      controllers.benefitsAndInsurance.routes.CheckYourAnswersController.onPageLoad.url
    )

  override def completeFlag(answers: UserAnswers): Option[Boolean] = answers.isBenefitsAndInsuranceComplete
}





