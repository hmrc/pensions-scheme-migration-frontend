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

package helpers.cya.trustees.company

import base.SpecBase._
import controllers.trustees.company.details.routes
import identifiers.beforeYouStart.SchemeNameId
import identifiers.trustees.company.CompanyDetailsId
import identifiers.trustees.company.details._
import models.requests.DataRequest
import models.{CheckMode, MigrationLock, ReferenceValue}
import org.scalatest.TryValues
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.i18n.Messages
import play.api.mvc.AnyContent
import uk.gov.hmrc.domain.PsaId
import uk.gov.hmrc.viewmodels.SummaryList.{Action, Key, Row, Value}
import uk.gov.hmrc.viewmodels.Text.Literal
import uk.gov.hmrc.viewmodels.{Html, MessageInterpolators}
import utils.Data.{companyDetails, credId, psaId, pstr, schemeName}
import utils.{Enumerable, UserAnswers}
import viewmodels.Message

class TrusteeCompanyDetailsCYAHelperSpec extends AnyWordSpec with Matchers with TryValues with Enumerable.Implicits {

  val cyaHelper = new TrusteeCompanyDetailsCYAHelper

  private def dataRequest(ua: UserAnswers): DataRequest[AnyContent] = DataRequest[AnyContent](request = fakeRequest, userAnswers = ua,
    psaId = PsaId(psaId), lock = MigrationLock(pstr = pstr, credId = credId, psaId = psaId), viewOnly = false)

  // scalastyle:off magic.number
  "TrusteeCompanyDetailsCYAHelper" must {
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

      result.head mustBe Row(
        key = Key(msg"${Message("messages__haveCompanyNumber", companyDetails.companyName).resolve}", classes = Seq("govuk-!-width-one-half")),
        value = Value(Literal("Yes")),
        actions = Seq(Action(
          content = Html(s"<span aria-hidden=true >${messages("site.change")}</span>"),
          href = routes.HaveCompanyNumberController.onPageLoad(0, CheckMode).url,
          visuallyHiddenText = Some(Literal(Messages("site.change") + " " +
            Messages("messages__haveCompanyNumber__cya__visuallyHidden", companyDetails.companyName))),
          attributes = Map("id" -> "cya-0-0-change")
        ))
      )

      result(1) mustBe Row(
        key = Key(msg"${Message("messages__companyNumber__cya", companyDetails.companyName).resolve}", classes = Seq("govuk-!-width-one-half")),
        value = Value(msg"12345678"),
        actions = Seq(Action(
          content = Html(s"<span aria-hidden=true >${messages("site.change")}</span>"),
          href = routes.CompanyNumberController.onPageLoad(0, CheckMode).url,
          visuallyHiddenText = Some(Literal(Messages("site.change") + " " +
            Messages("messages__companyNumber__cya__visuallyHidden", companyDetails.companyName))),
          attributes = Map("id" -> "cya-0-1-change")
        ))
      )

      result(2) mustBe Row(
        key = Key(msg"${Message("messages__hasUTR", companyDetails.companyName).resolve}", classes = Seq("govuk-!-width-one-half")),
        value = Value(Literal("Yes")),
        actions = Seq(Action(
          content = Html(s"<span aria-hidden=true >${messages("site.change")}</span>"),
          href = routes.HaveUTRController.onPageLoad(0, CheckMode).url,
          visuallyHiddenText = Some(Literal(Messages("site.change") + " " +
            Messages("messages__hasUTR__cya__visuallyHidden", companyDetails.companyName))),
          attributes = Map("id" -> "cya-0-2-change")
        ))
      )

      result(3) mustBe Row(
        key = Key(msg"${Message("messages__enterUTR__cya_label", companyDetails.companyName).resolve}", classes = Seq("govuk-!-width-one-half")),
        value = Value(msg"12345678"),
        actions = Seq(Action(
          content = Html(s"<span aria-hidden=true >${messages("site.change")}</span>"),
          href = routes.UTRController.onPageLoad(0, CheckMode).url,
          visuallyHiddenText = Some(Literal(Messages("site.change") + " " +
            Messages("messages__enterUTR__cya__visuallyHidden", companyDetails.companyName))),
          attributes = Map("id" -> "cya-0-3-change")
        ))
      )

      result(4) mustBe Row(
        key = Key(msg"${Message("messages__haveVAT", companyDetails.companyName).resolve}", classes = Seq("govuk-!-width-one-half")),
        value = Value(Literal("Yes")),
        actions = Seq(Action(
          content = Html(s"<span aria-hidden=true >${messages("site.change")}</span>"),
          href = routes.HaveVATController.onPageLoad(0, CheckMode).url,
          visuallyHiddenText = Some(Literal(Messages("site.change") + " " +
            Messages("messages__haveVAT__cya__visuallyHidden", companyDetails.companyName))),
          attributes = Map("id" -> "cya-0-4-change")
        ))
      )

      result(5) mustBe Row(
        key = Key(msg"${Message("messages__vat__cya", companyDetails.companyName).resolve}", classes = Seq("govuk-!-width-one-half")),
        value = Value(msg"12345678"),
        actions = Seq(Action(
          content = Html(s"<span aria-hidden=true >${messages("site.change")}</span>"),
          href = routes.VATController.onPageLoad(0, CheckMode).url,
          visuallyHiddenText = Some(Literal(Messages("site.change") + " " +
            Messages("messages__vat__cya__visuallyHidden", companyDetails.companyName))),
          attributes = Map("id" -> "cya-0-5-change")
        ))
      )

      result(6) mustBe Row(
        key = Key(msg"${Message("messages__havePAYE", companyDetails.companyName).resolve}", classes = Seq("govuk-!-width-one-half")),
        value = Value(Literal("Yes")),
        actions = Seq(Action(
          content = Html(s"<span aria-hidden=true >${messages("site.change")}</span>"),
          href = routes.HavePAYEController.onPageLoad(0, CheckMode).url,
          visuallyHiddenText = Some(Literal(Messages("site.change") + " " +
            Messages("messages__havePAYE__cya__visuallyHidden", companyDetails.companyName))),
          attributes = Map("id" -> "cya-0-6-change")
        ))
      )

      result(7) mustBe Row(
        key = Key(msg"${Message("messages__paye_cya", companyDetails.companyName).resolve}", classes = Seq("govuk-!-width-one-half")),
        value = Value(msg"12345678"),
        actions = Seq(Action(
          content = Html(s"<span aria-hidden=true >${messages("site.change")}</span>"),
          href = routes.PAYEController.onPageLoad(0, CheckMode).url,
          visuallyHiddenText = Some(Literal(Messages("site.change") + " " +
            Messages("messages__paye__cya__visuallyHidden", companyDetails.companyName))),
          attributes = Map("id" -> "cya-0-7-change")
        ))
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

      result.head mustBe Row(
        key = Key(msg"${Message("messages__haveCompanyNumber", companyDetails.companyName).resolve}", classes = Seq("govuk-!-width-one-half")),
        value = Value(Literal("No")),
        actions = Seq(Action(
          content = Html(s"<span aria-hidden=true >${messages("site.change")}</span>"),
          href = routes.HaveCompanyNumberController.onPageLoad(0, CheckMode).url,
          visuallyHiddenText = Some(Literal(Messages("site.change") + " " +
            Messages("messages__haveCompanyNumber__cya__visuallyHidden", companyDetails.companyName))),
          attributes = Map("id" -> "cya-0-0-change")
        ))
      )

      result(1) mustBe Row(
        key = Key(msg"${Message("messages__whyNoCompanyNumber", companyDetails.companyName).resolve}", classes = Seq("govuk-!-width-one-half")),
        value = Value(Literal("reason-company-number")),
        actions = Seq(Action(
          content = Html(s"<span aria-hidden=true >${messages("site.change")}</span>"),
          href = routes.NoCompanyNumberReasonController.onPageLoad(0, CheckMode).url,
          visuallyHiddenText = Some(Literal(Messages("site.change") + " " +
            Messages("messages__whyNoCompanyNumber__cya__visuallyHidden", companyDetails.companyName))),
          attributes = Map("id" -> "cya-0-1-change")
        ))
      )

      result(2) mustBe Row(
        key = Key(msg"${Message("messages__hasUTR", companyDetails.companyName).resolve}", classes = Seq("govuk-!-width-one-half")),
        value = Value(Literal("No")),
        actions = Seq(Action(
          content = Html(s"<span aria-hidden=true >${messages("site.change")}</span>"),
          href = routes.HaveUTRController.onPageLoad(0, CheckMode).url,
          visuallyHiddenText = Some(Literal(Messages("site.change") + " " +
            Messages("messages__hasUTR__cya__visuallyHidden", companyDetails.companyName))),
          attributes = Map("id" -> "cya-0-2-change")
        ))
      )

      result(3) mustBe Row(
        key = Key(msg"${Message("messages__whyNoUTR", companyDetails.companyName).resolve}", classes = Seq("govuk-!-width-one-half")),
        value = Value(Literal("reason-utr")),
        actions = Seq(Action(
          content = Html(s"<span aria-hidden=true >${messages("site.change")}</span>"),
          href = routes.NoUTRReasonController.onPageLoad(0, CheckMode).url,
          visuallyHiddenText = Some(Literal(Messages("site.change") + " " +
            Messages("messages__whyNoUTR__cya__visuallyHidden", companyDetails.companyName))),
          attributes = Map("id" -> "cya-0-3-change")
        ))
      )

      result(4) mustBe Row(
        key = Key(msg"${Message("messages__haveVAT", companyDetails.companyName).resolve}", classes = Seq("govuk-!-width-one-half")),
        value = Value(Literal("No")),
        actions = Seq(Action(
          content = Html(s"<span aria-hidden=true >${messages("site.change")}</span>"),
          href = routes.HaveVATController.onPageLoad(0, CheckMode).url,
          visuallyHiddenText = Some(Literal(Messages("site.change") + " " +
            Messages("messages__haveVAT__cya__visuallyHidden", companyDetails.companyName))),
          attributes = Map("id" -> "cya-0-4-change")
        ))
      )

      result(5) mustBe Row(
        key = Key(msg"${Message("messages__havePAYE", companyDetails.companyName).resolve}", classes = Seq("govuk-!-width-one-half")),
        value = Value(Literal("No")),
        actions = Seq(Action(
          content = Html(s"<span aria-hidden=true >${messages("site.change")}</span>"),
          href = routes.HavePAYEController.onPageLoad(0, CheckMode).url,
          visuallyHiddenText = Some(Literal(Messages("site.change") + " " +
            Messages("messages__havePAYE__cya__visuallyHidden", companyDetails.companyName))),
          attributes = Map("id" -> "cya-0-5-change")
        ))
      )
    }
  }
}
