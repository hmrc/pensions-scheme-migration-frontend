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

import base.SpecBase._
import controllers.beforeYouStartSpoke.routes
import identifiers.beforeYouStart._
import models.{Link, SchemeType}
import org.scalatest.{MustMatchers, TryValues, WordSpec}
import utils.Data.schemeName
import utils.{CountryOptions, Enumerable, InputOption, UserAnswers}
import viewmodels.{AnswerRow, AnswerSection, CYAViewModel, Message}

class BeforeYouStartCYAHelperSpec
  extends WordSpec
    with MustMatchers
    with TryValues
    with Enumerable.Implicits {

  val options = Seq(InputOption("territory:AE-AZ", "Abu Dhabi"), InputOption("country:AF", "Afghanistan"))

  implicit val countryOptions: CountryOptions = new CountryOptions(options)

  val beforeYouStartCYAHelper = new BeforeYouStartCYAHelper

  val ua: UserAnswers = UserAnswers()
    .set(SchemeNameId, schemeName).success.value
    .set(SchemeTypeId, SchemeType.Other("other")).success.value
    .set(EstablishedCountryId, "AF").success.value
    .set(WorkingKnowledgeId, true).success.value

  "BeforeYouStartCYAHelper" must {
    "return all rows" in {
      val result = beforeYouStartCYAHelper.viewmodel(countryOptions, fakeDataRequest(ua), messages)

      val rows: Seq[AnswerSection] = Seq(
        AnswerSection(
          headingKey = None,
          rows = Seq(
            AnswerRow(
              label = Message("messages__cya__scheme_name").resolve,
              answer = Seq(schemeName),
              answerIsMessageKey = false,
              changeUrl = None
            ),
            AnswerRow(
              label = Message("messages__cya__scheme_type", schemeName).resolve,
              answer = Seq(Message("messages__scheme_type_other").resolve),
              answerIsMessageKey = true,
              changeUrl = Some(Link(
                text = "Change",
                target = routes.SchemeTypeController.onPageLoad().url,
                visuallyHiddenText = Some(Message("messages__visuallyhidden__schemeType", schemeName).resolve)
              ))
            ),
            AnswerRow(
              label = Message("messages__cya__country", schemeName).resolve,
              answer = Seq("AF"),
              answerIsMessageKey = false,
              changeUrl = Some(Link(
                text = "Change",
                target = routes.EstablishedCountryController.onPageLoad().url,
                visuallyHiddenText = Some(Message("messages__visuallyhidden__schemeEstablishedCountry", schemeName).resolve)
              ))
            ),
            AnswerRow(
              label = Message("messages__cya__working_knowledge").resolve,
              answer = Seq("site.yes"),
              answerIsMessageKey = true,
              changeUrl = Some(Link(
                text = "Change",
                target = routes.WorkingKnowledgeController.onPageLoad().url,
                visuallyHiddenText = Some(Message("messages__visuallyhidden__working_knowledge").resolve)
              ))
            )
          )
        )
      )

      result mustBe CYAViewModel(
        answerSections = rows,
        href = controllers.routes.TaskListController.onPageLoad(),
        schemeName = schemeName,
        hideEditLinks = false,
        hideSaveAndContinueButton = false
      )
    }
  }
}
