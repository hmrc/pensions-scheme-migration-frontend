/*
 * Copyright 2023 HM Revenue & Customs
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
import uk.gov.hmrc.viewmodels.SummaryList.{Action, Key, Row, Value}
import uk.gov.hmrc.viewmodels.Text.{Literal, Message}
import uk.gov.hmrc.viewmodels.{Html, MessageInterpolators}
import utils.Data.schemeName
import utils.{CountryOptions, Enumerable, InputOption, UserAnswers}

class BeforeYouStartCYAHelperSpec
  extends SpecBase
    with Matchers
    with TryValues
    with Enumerable.Implicits {

  val options = Seq(InputOption("territory:AE-AZ", "Abu Dhabi"), InputOption("country:AF", "Afghanistan"))

  override implicit val countryOptions: CountryOptions = new CountryOptions(options)

  val beforeYouStartCYAHelper = new BeforeYouStartCYAHelper

  val ua: UserAnswers = UserAnswers()
    .set(SchemeNameId, schemeName).success.value
    .set(SchemeTypeId, SchemeType.Other("messages__scheme_type_other")).success.value
    .set(EstablishedCountryId, "AF").success.value
    .set(WorkingKnowledgeId, true).success.value

  "BeforeYouStartCYAHelper" must {
    "return all rows" in {
      val result = beforeYouStartCYAHelper.rowsForCYA(isEnabledChange = false)(fakeDataRequest(ua), messages)
      val rows: Seq[Row] = Seq(Row(
        key = Key(Message(msg"messages__cya__scheme_name".resolve), classes = Seq("govuk-!-width-one-half")),
        value = Value(Literal(schemeName)),
        actions = Nil
      ),
        Row(
          key = Key(msg"messages__cya__scheme_type".withArgs(schemeName), classes = Seq("govuk-!-width-one-half")),
          value = Value(msg"messages__scheme_type_other", classes = Seq("govuk-!-width-one-third")),
          actions = Seq(Action(
            content = Html(s"<span aria-hidden=true >${messages("site.change")}</span>"),
            href =  controllers.beforeYouStartSpoke.routes.SchemeTypeController.onPageLoad.url,
            visuallyHiddenText = Some(Literal(Messages("site.change") + " " + Messages("messages__visuallyhidden__schemeType", schemeName))),
            attributes = Map("id" -> "cya-0-1-change")
          ))
        ),
        Row(
          key = Key(msg"messages__cya__country".withArgs(schemeName), classes = Seq("govuk-!-width-one-half")),
          value = Value(msg"country.AF", classes = Seq("govuk-!-width-one-third")),
          actions = Seq(Action(
            content = Html(s"<span aria-hidden=true >${messages("site.change")}</span>"),
            href =  controllers.beforeYouStartSpoke.routes.EstablishedCountryController.onPageLoad.url,
            visuallyHiddenText = Some(Literal(Messages("site.change") + " " + Messages("messages__visuallyhidden__schemeEstablishedCountry", schemeName))),
            attributes = Map("id" -> "cya-0-2-change")
          ))
        ),
        Row(
          key = Key(Message(msg"messages__cya__working_knowledge".resolve), classes = Seq("govuk-!-width-one-half")),
          value = Value(msg"site.yes"),
          actions = Seq(Action(
            content = Html(s"<span aria-hidden=true >${messages("site.change")}</span>"),
            href =  controllers.beforeYouStartSpoke.routes.WorkingKnowledgeController.onPageLoad.url,
            visuallyHiddenText = Some(Literal(Messages("site.change") + " " + Messages("messages__visuallyhidden__working_knowledge"))),
            attributes = Map("id" -> "cya-0-3-change")
          ))
        )
      )

      result mustBe rows
    }
  }
}
