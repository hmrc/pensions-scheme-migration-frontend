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

package helpers

import base.SpecBase
import helpers.cya.BeforeYouStartCYAHelper
import identifiers.beforeYouStart._
import models.SchemeType
import org.scalatest.TryValues
import org.scalatest.matchers.must.Matchers
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.{HtmlContent, Text}
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist._
import utils.Data.schemeName
import utils.{Enumerable, UserAnswers}

class BeforeYouStartCYAHelperSpec
  extends SpecBase
    with Matchers
    with TryValues
    with Enumerable.Implicits {

  private val beforeYouStartCYAHelper = new BeforeYouStartCYAHelper

  private val ua: UserAnswers = UserAnswers()
    .set(SchemeNameId, schemeName).success.value
    .set(SchemeTypeId, SchemeType.Other("messages__scheme_type_other")).success.value
    .set(EstablishedCountryId, "AF").success.value
    .set(WorkingKnowledgeId, true).success.value

  "BeforeYouStartCYAHelper" must {
    "return all rows" in {
      val result = beforeYouStartCYAHelper.rowsForCYA(isEnabledChange = false)(fakeDataRequest(ua), messages)
      val rows: Seq[SummaryListRow] = Seq(SummaryListRow(
        key = Key(Text(Messages("messages__cya__scheme_name")), classes = "govuk-!-width-one-half"),
        value = Value(Text(schemeName)),
        actions = None
      ),
        SummaryListRow(
          key = Key(Text(Messages("messages__cya__scheme_type",schemeName)), classes = "govuk-!-width-one-half"),
          value = Value(Text(Messages("messages__scheme_type_other")), classes = "govuk-!-width-one-third"),
          actions = Some(Actions(
            items = Seq(
              ActionItem(
                content = HtmlContent(s"<span aria-hidden=true >${messages("site.change")}</span>"),
                href =  controllers.beforeYouStartSpoke.routes.SchemeTypeController.onPageLoad.url,
                visuallyHiddenText = Some(Messages("site.change") + " " + Messages("messages__visuallyhidden__schemeType", schemeName)),
                attributes = Map("id" -> "cya-0-1-change")
              )
            )
          ))
        ),
        SummaryListRow(
          key = Key(Text(Messages("messages__cya__country",schemeName)), classes = "govuk-!-width-one-half"),
          value = Value(Text(Messages("country.AF")), classes = "govuk-!-width-one-third"),
          actions = Some(Actions(
            items = Seq(
              ActionItem(
                content = HtmlContent(s"<span aria-hidden=true >${messages("site.change")}</span>"),
                href =  controllers.beforeYouStartSpoke.routes.EstablishedCountryController.onPageLoad.url,
                visuallyHiddenText = Some(Messages("site.change") + " " + Messages("messages__visuallyhidden__schemeEstablishedCountry", schemeName)),
                attributes = Map("id" -> "cya-0-2-change")
              )
            )
          ))
        ),
        SummaryListRow(
          key = Key(Text(Messages("messages__cya__working_knowledge")), classes = "govuk-!-width-one-half"),
          value = Value(Text(Messages("site.yes"))),
          actions = Some(Actions(
            items = Seq(
              ActionItem(
                content = HtmlContent(s"<span aria-hidden=true >${messages("site.change")}</span>"),
                href =  controllers.beforeYouStartSpoke.routes.WorkingKnowledgeController.onPageLoad.url,
                visuallyHiddenText = Some(Messages("site.change") + " " + Messages("messages__visuallyhidden__working_knowledge")),
                attributes = Map("id" -> "cya-0-3-change")
              )
            )
          ))
        )
      )

      result mustBe rows
    }
  }
}
