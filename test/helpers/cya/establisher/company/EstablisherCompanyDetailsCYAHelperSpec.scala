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

package helpers.cya.establisher.company

import controllers.establishers.company.details.routes
import helpers.CYAHelperSpecBase
import helpers.cya.establishers.company.EstablisherCompanyDetailsCYAHelper
import identifiers.beforeYouStart.SchemeNameId
import identifiers.establishers.company.CompanyDetailsId
import identifiers.establishers.company.details._
import models.{CheckMode, ReferenceValue}
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.Aliases.{HtmlContent, Text}
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist._
import utils.Data.{companyDetails, schemeName}
import utils.UserAnswers

class EstablisherCompanyDetailsCYAHelperSpec extends CYAHelperSpecBase {

  val cyaHelper = new EstablisherCompanyDetailsCYAHelper

  // scalastyle:off magic.number
  "EstablisherCompanyDetailsCYAHelper" must {
    "return all rows with correct change links, values and visually hidden text when user has answered yes for all questions" in {
      val ua: UserAnswers = UserAnswers()
        .setOrException(SchemeNameId, schemeName)
        .setOrException(CompanyDetailsId(0), companyDetails)
        .setOrException(HaveCompanyNumberId(0), true)
        .setOrException(HaveUTRId(0), true)
        .setOrException(HaveVATId(0), true)
        .setOrException(HavePAYEId(0), true)
        .setOrException(CompanyNumberId(0), ReferenceValue("12345678"))
        .setOrException(CompanyUTRId(0), ReferenceValue("12345678"))
        .setOrException(VATId(0), ReferenceValue("12345678"))
        .setOrException(PAYEId(0), ReferenceValue("12345678"))

      val result = cyaHelper.detailsRows(0)(dataRequest(ua), messages)

      result.head mustBe SummaryListRow(
        key = Key(Text(Messages("messages__haveCompanyNumber", companyDetails.companyName)), classes = "govuk-!-width-one-half"),
        value = Value(Text("Yes")),
        actions = Some(Actions( items = Seq(ActionItem(
          content = HtmlContent(s"<span aria-hidden=true >${messages("site.change")}</span>"),
          href = routes.HaveCompanyNumberController.onPageLoad(0, CheckMode).url,
          visuallyHiddenText = Some(Messages("site.change") + " " +
            Messages("messages__haveCompanyNumber__cya__visuallyHidden", companyDetails.companyName)),
          attributes = Map("id" -> "cya-0-0-change")
        ))))
      )

      result(1) mustBe SummaryListRow(
        key = Key(Text(Messages("messages__companyNumber__cya", companyDetails.companyName)), classes = "govuk-!-width-one-half"),
        value = Value(Text("12345678")),
        actions = Some(Actions( items = Seq(ActionItem(
          content = HtmlContent(s"<span aria-hidden=true >${messages("site.change")}</span>"),
          href = routes.CompanyNumberController.onPageLoad(0, CheckMode).url,
          visuallyHiddenText = Some(Messages("site.change") + " " +
            Messages("messages__companyNumber__cya__visuallyHidden", companyDetails.companyName)),
          attributes = Map("id" -> "cya-0-1-change")
        ))))
      )

      result(2) mustBe SummaryListRow(
        key = Key(Text(Messages("messages__hasUTR", companyDetails.companyName)), classes = "govuk-!-width-one-half"),
        value = Value(Text("Yes")),
        actions = Some(Actions( items = Seq(ActionItem(
          content = HtmlContent(s"<span aria-hidden=true >${messages("site.change")}</span>"),
          href = routes.HaveUTRController.onPageLoad(0, CheckMode).url,
          visuallyHiddenText = Some(Messages("site.change") + " " +
            Messages("messages__hasUTR__cya__visuallyHidden", companyDetails.companyName)),
          attributes = Map("id" -> "cya-0-2-change")
        ))))
      )

      result(3) mustBe SummaryListRow(
        key = Key(Text(Messages("messages__enterUTR__cya_label", companyDetails.companyName)), classes = "govuk-!-width-one-half"),
        value = Value(Text("12345678")),
        actions = Some(Actions( items = Seq(ActionItem(
          content = HtmlContent(s"<span aria-hidden=true >${messages("site.change")}</span>"),
          href = routes.UTRController.onPageLoad(0, CheckMode).url,
          visuallyHiddenText = Some(Messages("site.change") + " " +
            Messages("messages__enterUTR__cya__visuallyHidden", companyDetails.companyName)),
          attributes = Map("id" -> "cya-0-3-change")
        ))))
      )

      result(4) mustBe SummaryListRow(
        key = Key(Text(Messages("messages__haveVAT", companyDetails.companyName)), classes = "govuk-!-width-one-half"),
        value = Value(Text("Yes")),
        actions = Some(Actions( items = Seq(ActionItem(
          content = HtmlContent(s"<span aria-hidden=true >${messages("site.change")}</span>"),
          href = routes.HaveVATController.onPageLoad(0, CheckMode).url,
          visuallyHiddenText = Some(Messages("site.change") + " " +
            Messages("messages__haveVAT__cya__visuallyHidden", companyDetails.companyName)),
          attributes = Map("id" -> "cya-0-4-change")
        ))))
      )

      result(5) mustBe SummaryListRow(
        key = Key(Text(Messages("messages__vat__cya", companyDetails.companyName)), classes = "govuk-!-width-one-half"),
        value = Value(Text("12345678")),
        actions = Some(Actions( items = Seq(ActionItem(
          content = HtmlContent(s"<span aria-hidden=true >${messages("site.change")}</span>"),
          href = routes.VATController.onPageLoad(0, CheckMode).url,
          visuallyHiddenText = Some(Messages("site.change") + " " +
            Messages("messages__vat__cya__visuallyHidden", companyDetails.companyName)),
          attributes = Map("id" -> "cya-0-5-change")
        ))))
      )

      result(6) mustBe SummaryListRow(
        key = Key(Text(Messages("messages__havePAYE", companyDetails.companyName)), classes = "govuk-!-width-one-half"),
        value = Value(Text("Yes")),
        actions = Some(Actions( items = Seq(ActionItem(
          content = HtmlContent(s"<span aria-hidden=true >${messages("site.change")}</span>"),
          href = routes.HavePAYEController.onPageLoad(0, CheckMode).url,
          visuallyHiddenText = Some(Messages("site.change") + " " +
            Messages("messages__havePAYE__cya__visuallyHidden", companyDetails.companyName)),
          attributes = Map("id" -> "cya-0-6-change")
        ))))
      )

      result(7) mustBe SummaryListRow(
        key = Key(Text(Messages("messages__paye_cya", companyDetails.companyName)), classes = "govuk-!-width-one-half"),
        value = Value(Text("12345678")),
        actions = Some(Actions( items = Seq(ActionItem(
          content = HtmlContent(s"<span aria-hidden=true >${messages("site.change")}</span>"),
          href = routes.PAYEController.onPageLoad(0, CheckMode).url,
          visuallyHiddenText = Some(Messages("site.change") + " " +
            Messages("messages__paye__cya__visuallyHidden", companyDetails.companyName)),
          attributes = Map("id" -> "cya-0-7-change")
        ))))
      )

    }

    "return all rows with correct change links, values and visually hidden text when user has answered no for all questions" in {
      val ua: UserAnswers = UserAnswers()
        .setOrException(SchemeNameId, schemeName)
        .setOrException(CompanyDetailsId(0), companyDetails)
        .setOrException(HaveCompanyNumberId(0), false)
        .setOrException(HaveUTRId(0), false)
        .setOrException(HaveVATId(0), false)
        .setOrException(HavePAYEId(0), false)
        .setOrException(NoCompanyNumberReasonId(0), "reason-company-number")
        .setOrException(NoUTRReasonId(0), "reason-utr")

      val result = cyaHelper.detailsRows(0)(dataRequest(ua), messages)

      result.head mustBe SummaryListRow(
        key = Key(Text(Messages("messages__haveCompanyNumber", companyDetails.companyName)), classes = "govuk-!-width-one-half"),
        value = Value(Text("No")),
        actions = Some(Actions( items = Seq(ActionItem(
          content = HtmlContent(s"<span aria-hidden=true >${messages("site.change")}</span>"),
          href = routes.HaveCompanyNumberController.onPageLoad(0, CheckMode).url,
          visuallyHiddenText = Some(Messages("site.change") + " " +
            Messages("messages__haveCompanyNumber__cya__visuallyHidden", companyDetails.companyName)),
          attributes = Map("id" -> "cya-0-0-change")
        ))))
      )

      result(1) mustBe SummaryListRow(
        key = Key(Text(Messages("messages__whyNoCompanyNumber", companyDetails.companyName)), classes = "govuk-!-width-one-half"),
        value = Value(Text("reason-company-number")),
        actions = Some(Actions( items = Seq(ActionItem(
          content = HtmlContent(s"<span aria-hidden=true >${messages("site.change")}</span>"),
          href = routes.NoCompanyNumberReasonController.onPageLoad(0, CheckMode).url,
          visuallyHiddenText = Some(Messages("site.change") + " " +
            Messages("messages__whyNoCompanyNumber__cya__visuallyHidden", companyDetails.companyName)),
          attributes = Map("id" -> "cya-0-1-change")
        ))))
      )

      result(2) mustBe SummaryListRow(
        key = Key(Text(Messages("messages__hasUTR", companyDetails.companyName)), classes = "govuk-!-width-one-half"),
        value = Value(Text("No")),
        actions = Some(Actions( items = Seq(ActionItem(
          content = HtmlContent(s"<span aria-hidden=true >${messages("site.change")}</span>"),
          href = routes.HaveUTRController.onPageLoad(0, CheckMode).url,
          visuallyHiddenText = Some(Messages("site.change") + " " +
            Messages("messages__hasUTR__cya__visuallyHidden", companyDetails.companyName)),
          attributes = Map("id" -> "cya-0-2-change")
        ))))
      )

      result(3) mustBe SummaryListRow(
        key = Key(Text(Messages("messages__whyNoUTR", companyDetails.companyName)), classes = "govuk-!-width-one-half"),
        value = Value(Text("reason-utr")),
        actions = Some(Actions( items = Seq(ActionItem(
          content = HtmlContent(s"<span aria-hidden=true >${messages("site.change")}</span>"),
          href = routes.NoUTRReasonController.onPageLoad(0, CheckMode).url,
          visuallyHiddenText = Some(Messages("site.change") + " " +
            Messages("messages__whyNoUTR__cya__visuallyHidden", companyDetails.companyName)),
          attributes = Map("id" -> "cya-0-3-change")
        ))))
      )

      result(4) mustBe SummaryListRow(
        key = Key(Text(Messages("messages__haveVAT", companyDetails.companyName)), classes = "govuk-!-width-one-half"),
        value = Value(Text("No")),
        actions = Some(Actions( items = Seq(ActionItem(
          content = HtmlContent(s"<span aria-hidden=true >${messages("site.change")}</span>"),
          href = routes.HaveVATController.onPageLoad(0, CheckMode).url,
          visuallyHiddenText = Some(Messages("site.change") + " " +
            Messages("messages__haveVAT__cya__visuallyHidden", companyDetails.companyName)),
          attributes = Map("id" -> "cya-0-4-change")
        ))))
      )

      result(5) mustBe SummaryListRow(
        key = Key(Text(Messages("messages__havePAYE", companyDetails.companyName)), classes = "govuk-!-width-one-half"),
        value = Value(Text("No")),
        actions = Some(Actions( items = Seq(ActionItem(
          content = HtmlContent(s"<span aria-hidden=true >${messages("site.change")}</span>"),
          href = routes.HavePAYEController.onPageLoad(0, CheckMode).url,
          visuallyHiddenText = Some(Messages("site.change") + " " +
            Messages("messages__havePAYE__cya__visuallyHidden", companyDetails.companyName)),
          attributes = Map("id" -> "cya-0-5-change")
        ))))
      )
    }
  }
}
