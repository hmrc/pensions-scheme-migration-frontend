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

package helpers.cya.trustees.individual

import base.SpecBase._
import helpers.routes.TrusteesIndividualRoutes
import identifiers.beforeYouStart.SchemeNameId
import identifiers.trustees.individual.TrusteeNameId
import identifiers.trustees.individual.details._
import models.requests.DataRequest
import models.{CheckMode, MigrationLock, PersonName, ReferenceValue}
import org.scalatest.TryValues
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.i18n.Messages
import play.api.mvc.AnyContent
import uk.gov.hmrc.domain.PsaId
import uk.gov.hmrc.viewmodels.SummaryList.{Action, Key, Row, Value}
import uk.gov.hmrc.viewmodels.Text.Literal
import uk.gov.hmrc.viewmodels.{Html, MessageInterpolators}
import utils.Data.{credId, psaId, pstr, schemeName}
import utils.{Enumerable, UserAnswers}
import viewmodels.Message

import java.time.LocalDate

class TrusteeDetailsCYAHelperSpec extends AnyWordSpec with Matchers with TryValues with Enumerable.Implicits {

  private val cyaHelper = new TrusteeDetailsCYAHelper
  private val individualName = PersonName("test", "name")

  private def dataRequest(ua: UserAnswers): DataRequest[AnyContent] = DataRequest[AnyContent](request = fakeRequest, userAnswers = ua,
    psaId = PsaId(psaId), lock = MigrationLock(pstr = pstr, credId = credId, psaId = psaId), viewOnly = false)

  // scalastyle:off magic.number
  "TrusteeDetailsCYAHelper" must {
    "return all rows with correct change links, values and visually hidden text when user has answered yes for all questions" in {
      val ua: UserAnswers = UserAnswers()
        .setOrException(SchemeNameId, schemeName)
        .setOrException(TrusteeNameId(0), individualName)
        .setOrException(TrusteeDOBId(0), LocalDate.of(1980, 6, 9))
        .setOrException(TrusteeHasNINOId(0), true)
        .setOrException(TrusteeNINOId(0), ReferenceValue("CS700100A"))
        .setOrException(TrusteeHasUTRId(0), true)
        .setOrException(TrusteeUTRId(0), ReferenceValue("12345678"))

      val result = cyaHelper.detailsRows(0)(dataRequest(ua), messages)

      result.head mustBe Row(
        key = Key(msg"${Message("messages__dob__title", individualName.fullName).resolve}", classes = Seq("govuk-!-width-one-half")),
        value = Value(Literal("9-6-1980")),
        actions = Seq(Action(
          content = Html(s"<span aria-hidden=true >${messages("site.change")}</span>"),
          href = TrusteesIndividualRoutes.dateOfBirthRoute(0, CheckMode).url,
          visuallyHiddenText = Some(Literal(Messages("site.change") + " " +
            Messages("messages__dob__cya__visuallyHidden", individualName.fullName))),
          attributes = Map("id" -> "cya-0-0-change")
        ))
      )

      result(1) mustBe Row(
        key = Key(msg"${Message("messages__hasNINO", individualName.fullName).resolve}", classes = Seq("govuk-!-width-one-half")),
        value = Value(msg"booleanAnswer.true"),
        actions = Seq(Action(
          content = Html(s"<span aria-hidden=true >${messages("site.change")}</span>"),
          href = TrusteesIndividualRoutes.haveNationalInsuranceNumberRoute(0, CheckMode).url,
          visuallyHiddenText = Some(Literal(Messages("site.change") + " " +
            Messages("messages__hasNINO__cya__visuallyHidden", individualName.fullName))),
          attributes = Map("id" -> "cya-0-1-change")
        ))
      )

      result(2) mustBe Row(
        key = Key(msg"${Message("messages__hasNINO__cya", individualName).resolve}", classes = Seq("govuk-!-width-one-half")),
        value = Value(msg"CS700100A"),
        actions = Seq(Action(
          content = Html(s"<span aria-hidden=true >${messages("site.change")}</span>"),
          href = TrusteesIndividualRoutes.enterNationaInsuranceNumberRoute(0, CheckMode).url,
          visuallyHiddenText = Some(Literal(Messages("site.change") + " " +
            Messages("messages__enterNINO__cya_visuallyHidden", individualName.fullName))),
          attributes = Map("id" -> "cya-0-2-change")
        ))
      )

      result(3) mustBe Row(
        key = Key(msg"${Message("messages__hasUTR", individualName.fullName).resolve}", classes = Seq("govuk-!-width-one-half")),
        value = Value(msg"booleanAnswer.true"),
        actions = Seq(Action(
          content = Html(s"<span aria-hidden=true >${messages("site.change")}</span>"),
          href = TrusteesIndividualRoutes.haveUniqueTaxpayerReferenceRoute(0, CheckMode).url,
          visuallyHiddenText = Some(Literal(Messages("site.change") + " " +
            Messages("messages__hasUTR__cya__visuallyHidden", individualName.fullName))),
          attributes = Map("id" -> "cya-0-3-change")
        ))
      )

      result(4) mustBe Row(
        key = Key(msg"${Message("messages__hasUTR__cya", individualName.fullName).resolve}", classes = Seq("govuk-!-width-one-half")),
        value = Value(msg"12345678"),
        actions = Seq(Action(
          content = Html(s"<span aria-hidden=true >${messages("site.change")}</span>"),
          href = TrusteesIndividualRoutes.enterUniqueTaxpayerReferenceRoute(0, CheckMode).url,
          visuallyHiddenText = Some(Literal(Messages("site.change") + " " +
            Messages("messages__enterUTR__cya__visuallyHidden", individualName.fullName))),
          attributes = Map("id" -> "cya-0-4-change")
        ))
      )
    }

    "return all rows with correct change links, values and visually hidden text when user has answered no for all questions" in {
      val ua: UserAnswers = UserAnswers()
        .setOrException(SchemeNameId, schemeName)
        .setOrException(TrusteeNameId(0), individualName)
        .setOrException(TrusteeDOBId(0), LocalDate.of(1980, 6, 9))
        .setOrException(TrusteeHasNINOId(0), false)
        .setOrException(TrusteeNoNINOReasonId(0), "no nino reason")
        .setOrException(TrusteeHasUTRId(0), false)
        .setOrException(TrusteeNoUTRReasonId(0), "no utr reason")

      val result = cyaHelper.detailsRows(0)(dataRequest(ua), messages)

      result.head mustBe Row(
        key = Key(msg"${Message("messages__dob__title", individualName.fullName).resolve}", classes = Seq("govuk-!-width-one-half")),
        value = Value(Literal("9-6-1980")),
        actions = Seq(Action(
          content = Html(s"<span aria-hidden=true >${messages("site.change")}</span>"),
          href = TrusteesIndividualRoutes.dateOfBirthRoute(0, CheckMode).url,
          visuallyHiddenText = Some(Literal(Messages("site.change") + " " +
            Messages("messages__dob__cya__visuallyHidden", individualName.fullName))),
          attributes = Map("id" -> "cya-0-0-change")
        ))
      )

      result(1) mustBe Row(
        key = Key(msg"${Message("messages__hasNINO", individualName.fullName).resolve}", classes = Seq("govuk-!-width-one-half")),
        value = Value(msg"booleanAnswer.false"),
        actions = Seq(Action(
          content = Html(s"<span aria-hidden=true >${messages("site.change")}</span>"),
          href = TrusteesIndividualRoutes.haveNationalInsuranceNumberRoute(0, CheckMode).url,
          visuallyHiddenText = Some(Literal(Messages("site.change") + " " +
            Messages("messages__hasNINO__cya__visuallyHidden", individualName.fullName))),
          attributes = Map("id" -> "cya-0-1-change")
        ))
      )


      result(2) mustBe Row(
        key = Key(msg"${Message("messages__whyNoNINO", individualName.fullName).resolve}", classes = Seq("govuk-!-width-one-half")),
        value = Value(Literal("no nino reason")),
        actions = Seq(Action(
          content = Html(s"<span aria-hidden=true >${messages("site.change")}</span>"),
          href = TrusteesIndividualRoutes.reasonForNoNationalInsuranceNumberRoute(0, CheckMode).url,
          visuallyHiddenText = Some(Literal(Messages("site.change") + " " +
            Messages("messages__whyNoNINO__cya__visuallyHidden", individualName.fullName))),
          attributes = Map("id" -> "cya-0-2-change")
        ))
      )

      result(3) mustBe Row(
        key = Key(msg"${Message("messages__hasUTR", individualName.fullName).resolve}", classes = Seq("govuk-!-width-one-half")),
        value = Value(msg"booleanAnswer.false"),
        actions = Seq(Action(
          content = Html(s"<span aria-hidden=true >${messages("site.change")}</span>"),
          href = TrusteesIndividualRoutes.haveUniqueTaxpayerReferenceRoute(0, CheckMode).url,
          visuallyHiddenText = Some(Literal(Messages("site.change") + " " +
            Messages("messages__hasUTR__cya__visuallyHidden", individualName.fullName))),
          attributes = Map("id" -> "cya-0-3-change")
        ))
      )

      result(4) mustBe Row(
        key = Key(msg"${Message("messages__whyNoUTR", individualName.fullName).resolve}", classes = Seq("govuk-!-width-one-half")),
        value = Value(Literal("no utr reason")),
        actions = Seq(Action(
          content = Html(s"<span aria-hidden=true >${messages("site.change")}</span>"),
          href = TrusteesIndividualRoutes.reasonForNoUniqueTaxpayerReferenceRoute(0, CheckMode).url,
          visuallyHiddenText = Some(Literal(Messages("site.change") + " " +
            Messages("messages__whyNoUTR__cya__visuallyHidden", individualName.fullName))),
          attributes = Map("id" -> "cya-0-4-change")
        ))
      )
    }
  }
}
