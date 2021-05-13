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

import controllers.beforeYouStartSpoke.routes
import identifiers.beforeYouStart.{EstablishedCountryId, SchemeNameId, SchemeTypeId, WorkingKnowledgeId}
import models.requests.DataRequest
import play.api.i18n.Messages
import play.api.mvc.AnyContent
import utils.{CountryOptions, UserAnswers}
import viewmodels.{AnswerRow, AnswerSection, CYAViewModel}

class BeforeYouStartCYAHelper extends CYAHelper {

  def viewmodel(
                 implicit countryOptions: CountryOptions,
                 request: DataRequest[AnyContent],
                 messages: Messages
               ): CYAViewModel = {
    implicit val ua: UserAnswers = request.userAnswers
    val schemeName = getAnswer(SchemeNameId)

    val beforeYouStart = AnswerSection(
      headingKey = None,
      rows = Seq(
        AnswerRow(
          label = "messages__cya__scheme_name",
          answer = Seq(schemeName),
          answerIsMessageKey = false,
          changeUrl = None
        ),
        AnswerRow(
          label = messages("messages__cya__scheme_type", schemeName),
          answer = Seq(s"messages__scheme_type_${getAnswer(SchemeTypeId)}"),
          answerIsMessageKey = true,
          changeUrl = changeLink(
            url = routes.SchemeTypeController.onPageLoad().url,
            visuallyHiddenText = Some(messages("messages__visuallyhidden__schemeType", schemeName))
          )
        ),
        AnswerRow(
          label = messages("messages__cya__country", schemeName),
          answer = Seq(countryOptions.getCountryNameFromCode(getAnswer(EstablishedCountryId))),
          answerIsMessageKey = false,
          changeUrl = changeLink(
            url = routes.EstablishedCountryController.onPageLoad().url,
            visuallyHiddenText = Some(messages("messages__visuallyhidden__schemeEstablishedCountry", schemeName))
          )
        ),
        boolAnswerOrAddLink(
          id = WorkingKnowledgeId,
          message = "messages__cya__working_knowledge",
          url = routes.WorkingKnowledgeController.onPageLoad().url,
          visuallyHiddenText = Some(messages("messages__visuallyhidden__working_knowledge"))
        )
      )
    )

    CYAViewModel(
      answerSections = Seq(beforeYouStart),
      href = controllers.routes.TaskListController.onPageLoad(),
      schemeName = schemeName,
      hideEditLinks = request.viewOnly,
      hideSaveAndContinueButton = false
    )
  }
}
