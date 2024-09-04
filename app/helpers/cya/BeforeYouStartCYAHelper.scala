/*
 * Copyright 2024 HM Revenue & Customs
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

import helpers.CountriesHelper
import identifiers.beforeYouStart._
import models.SchemeType
import models.SchemeType.Other
import models.requests.DataRequest
import play.api.i18n.Messages
import play.api.mvc.AnyContent
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.HtmlContent
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.{Actions, SummaryListRow}
import utils.UserAnswers
import viewmodels.Message

class BeforeYouStartCYAHelper extends CYAHelperForTwirl with CountriesHelper {
  //scalastyle:off method.length
  //scalastyle:off cyclomatic.complexity
  def rowsForCYA(isEnabledChange: Boolean)(implicit request: DataRequest[AnyContent],
                                           messages: Messages
  ): Seq[SummaryListRow] = {
    implicit val ua: UserAnswers = request.userAnswers
    val schemeName = CYAHelper.getAnswer(SchemeNameId)
    val schemeTypeAnswer = ua.get(SchemeTypeId)(SchemeType.optionalReads)

    val schemeTypeRow = {
      val url: String = controllers.beforeYouStartSpoke.routes.SchemeTypeController.onPageLoad.url
      val visuallyHiddenText = Message("messages__visuallyhidden__schemeType", schemeName)
      schemeTypeAnswer match {
        case None => SummaryListRow(
          key = KeyViewModel(HtmlContent(Messages("messages__cya__scheme_type", schemeName))).withCssClass("govuk-!-width-one-half"),
          value = ValueViewModel(HtmlContent(Messages("site.incomplete"))).withCssClass("govuk-!-width-one-third"),
          actions = Some(Actions(items = actionAdd(Some(url), Some(visuallyHiddenText))))
        )
        case Some(Other(details)) if details.equals("") => SummaryListRow(
          key = KeyViewModel(HtmlContent(Messages("messages__cya__scheme_type", schemeName))).withCssClass("govuk-!-width-one-half"),
          value = ValueViewModel(HtmlContent(Messages("site.incomplete"))).withCssClass("govuk-!-width-one-third"),
          actions = Some(Actions(items = actionAdd(Some(url), Some(visuallyHiddenText))))
        )
        case Some(Other(details)) if details.nonEmpty => SummaryListRow(
          key = KeyViewModel(HtmlContent(Messages("messages__cya__scheme_type", schemeName))).withCssClass("govuk-!-width-one-half"),
          value = ValueViewModel(HtmlContent(Messages(details))).withCssClass("govuk-!-width-one-third"),
          actions = actionChange(Some(url), Some(visuallyHiddenText))
        )
        case Some(value) if request.userAnswers.get(IsSchemeTypeOtherId).nonEmpty => SummaryListRow(
          key = KeyViewModel(HtmlContent(Messages("messages__cya__scheme_type", schemeName))).withCssClass("govuk-!-width-one-half"),
          value = ValueViewModel(HtmlContent(Messages(s"messages__scheme_type_$value"))).withCssClass("govuk-!-width-one-third"),
          actions = actionChange(Some(url), Some(visuallyHiddenText))
        )
        case Some(value) if isEnabledChange => SummaryListRow(
          key = KeyViewModel(HtmlContent(Messages("messages__cya__scheme_type", schemeName))).withCssClass("govuk-!-width-one-half"),
          value = ValueViewModel(HtmlContent(Messages(s"messages__scheme_type_$value"))).withCssClass("govuk-!-width-one-third"),
          actions = actionChange(Some(url), Some(visuallyHiddenText))
        )
        case Some(value) => SummaryListRow(
          key = KeyViewModel(HtmlContent(Messages("messages__cya__scheme_type", schemeName))).withCssClass("govuk-!-width-one-half"),
          value = ValueViewModel(HtmlContent(Messages(s"messages__scheme_type_$value"))).withCssClass("govuk-!-width-one-third"),
          actions = None
        )
      }
    }

    val seqRowSchemeNameAndType = Seq(
      answerRow(messages("messages__cya__scheme_name"), schemeName),
      schemeTypeRow)

    val country = request.userAnswers.get(EstablishedCountryId)
    val countryRow = {
      val url: String = controllers.beforeYouStartSpoke.routes.EstablishedCountryController.onPageLoad.url
      val visuallyHiddenText = Message("messages__visuallyhidden__schemeEstablishedCountry", schemeName)
      country match {
        case None => SummaryListRow(
          key = KeyViewModel(HtmlContent(Messages("messages__cya__country", schemeName))).withCssClass("govuk-!-width-one-half"),
          value = ValueViewModel(HtmlContent(Messages("site.incomplete"))).withCssClass("govuk-!-width-one-third"),
          actions = Some(Actions(items = actionAdd(Some(url), Some(visuallyHiddenText)))))
        case Some(details) => SummaryListRow(
          key = KeyViewModel(HtmlContent(Messages("messages__cya__country", schemeName))).withCssClass("govuk-!-width-one-half"),
          value = ValueViewModel(HtmlContent(Messages(s"country.$details"))).withCssClass("govuk-!-width-one-third"),
          actions = actionChange(Some(url), Some(visuallyHiddenText))
        )
      }
    }
    val seqRowCountryWorkingKnowledge = Seq(
      countryRow,
      answerOrAddRow(
        id = WorkingKnowledgeId,
        message = Message("messages__cya__working_knowledge").resolve,
        url = Some(controllers.beforeYouStartSpoke.routes.WorkingKnowledgeController.onPageLoad.url),
        visuallyHiddenText = Some(Message("messages__visuallyhidden__working_knowledge")),
        answerTransform = Some(booleanToContent)
      )
    )
    val beforeYouStart = seqRowSchemeNameAndType ++ seqRowCountryWorkingKnowledge

    rowsWithDynamicIndices(beforeYouStart)
  }
}
