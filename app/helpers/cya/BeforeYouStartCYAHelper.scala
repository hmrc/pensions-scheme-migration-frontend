/*
 * Copyright 2022 HM Revenue & Customs
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

package helpers.cya

import controllers.beforeYouStartSpoke.routes
import helpers.CountriesHelper
import identifiers.beforeYouStart._
import models.SchemeType
import models.SchemeType.Other
import models.requests.DataRequest
import play.api.i18n.Messages
import play.api.mvc.AnyContent
import uk.gov.hmrc.viewmodels.MessageInterpolators
import uk.gov.hmrc.viewmodels.SummaryList.{Key, Row, Value}
import utils.UserAnswers
import viewmodels.Message
import identifiers.beforeYouStart.IsSchemeTypeOtherId

class BeforeYouStartCYAHelper extends CYAHelper with CountriesHelper {
  //scalastyle:off method.length
  def rows(implicit request: DataRequest[AnyContent],
            messages: Messages
          ): Seq[Row] = {
    implicit val ua: UserAnswers = request.userAnswers
    val schemeName = CYAHelper.getAnswer(SchemeNameId)
    val schemeTypeAnswer = ua.get(SchemeTypeId)(SchemeType.optionalReads)
    val schemeTypeRow = {
      val url: String = routes.SchemeTypeController.onPageLoad().url
      val visuallyHiddenText = msg"messages__visuallyhidden__schemeType".withArgs(schemeName)
      schemeTypeAnswer match {
        case None => Row(
          key = Key(msg"messages__cya__scheme_type".withArgs(schemeName), classes = Seq("govuk-!-width-one-half")),
          value = Value(msg"site.not_entered", classes = Seq("govuk-!-width-one-third")),
          actions = actionAdd(Some(url), Some(visuallyHiddenText))
        )
        case Some(Other(details)) if details.equals("") => Row(
          key = Key(msg"messages__cya__scheme_type".withArgs(schemeName), classes = Seq("govuk-!-width-one-half")),
          value = Value(msg"messages__schemeTaskList__incomplete", classes = Seq("govuk-!-width-one-third")),
          actions = actionAdd(Some(url), Some(visuallyHiddenText))
        )
        case Some(Other(details)) if details.nonEmpty => Row(
          key = Key(msg"messages__cya__scheme_type".withArgs(schemeName), classes = Seq("govuk-!-width-one-half")),
          value = Value(msg"$details", classes = Seq("govuk-!-width-one-third")),
          actions = actionChange(Some(url), Some(visuallyHiddenText))
        )
        case Some(value) if request.userAnswers.get(IsSchemeTypeOtherId).nonEmpty => Row(
          key = Key(msg"messages__cya__scheme_type".withArgs(schemeName), classes = Seq("govuk-!-width-one-half")),
          value = Value(msg"messages__scheme_type_$value", classes = Seq("govuk-!-width-one-third")),
          actions = actionChange(Some(url), Some(visuallyHiddenText))
        )
        case Some(value) => Row(
          key = Key(msg"messages__cya__scheme_type".withArgs(schemeName), classes = Seq("govuk-!-width-one-half")),
          value = Value(msg"messages__scheme_type_$value", classes = Seq("govuk-!-width-one-third")),
          actions = Seq.empty
        )
      }
    }

     val seqRowSchemeNameAndType = Seq(
      answerRow(messages("messages__cya__scheme_name"), schemeName),
      schemeTypeRow)

    val seqRowCountryWorkingKnowledge = Seq(
      answerRow(messages("messages__cya__country", schemeName),
        messages(s"country.${CYAHelper.getAnswer(EstablishedCountryId)}"),
        Some(routes.EstablishedCountryController.onPageLoad().url),
        Some(msg"messages__visuallyhidden__schemeEstablishedCountry".withArgs(schemeName))),
      answerOrAddRow(
        id = WorkingKnowledgeId,
        message = Message("messages__cya__working_knowledge").resolve,
        url = Some(routes.WorkingKnowledgeController.onPageLoad().url),
        visuallyHiddenText = Some(msg"messages__visuallyhidden__working_knowledge"),
        answerTransform = Some(booleanToContent)
      )
    )
    val beforeYouStart = seqRowSchemeNameAndType ++ seqRowCountryWorkingKnowledge

      rowsWithDynamicIndices(beforeYouStart)
  }
}
