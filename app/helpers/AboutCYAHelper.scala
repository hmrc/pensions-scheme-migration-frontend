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

import identifiers.aboutMembership.{FutureMembersId, CurrentMembersId}
import identifiers.beforeYouStart.SchemeNameId
import models.Members
import models.requests.DataRequest
import play.api.i18n.Messages
import play.api.mvc.AnyContent
import uk.gov.hmrc.viewmodels.{MessageInterpolators, SummaryList, Text}
import utils.{UserAnswers, Enumerable}
import viewmodels.Message

class AboutCYAHelper extends CYAHelper with Enumerable.Implicits{

  def membershipRows(implicit request: DataRequest[AnyContent],
                messages: Messages
               ): Seq[SummaryList.Row] = {
    implicit val ua: UserAnswers = request.userAnswers
    val schemeName = getAnswer(SchemeNameId)

    val answerTransform: Option[Members => Text] = Some(opt => msg"members.${opt.toString}")

    Seq(answerOrAddRow(CurrentMembersId, Message("currentMembers.title", schemeName).resolve,
        controllers.aboutMembership.routes.CurrentMembersController.onPageLoad().url,
        Some(msg"messages__visuallyhidden__currentMembers"), answerTransform
      ),
        answerOrAddRow(FutureMembersId, Message("futureMembers.title", schemeName).resolve,
          controllers.aboutMembership.routes.FutureMembersController.onPageLoad().url,
          Some(msg"messages__visuallyhidden__futureMembers"), answerTransform
        )
      )
  }
}
