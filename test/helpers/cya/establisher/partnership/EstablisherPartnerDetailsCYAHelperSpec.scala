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

package helpers.cya.establisher.partnership

import controllers.establishers.partnership.partner.address.{routes => addressRoutes}
import controllers.establishers.partnership.partner.contact.{routes => contactRoutes}
import controllers.establishers.partnership.partner.details.{routes => detailsRoutes}
import controllers.establishers.partnership.partner.routes
import helpers.CYAHelperSpecBase
import helpers.cya.establishers.partnership.EstablisherPartnerDetailsCYAHelper
import identifiers.beforeYouStart.SchemeNameId
import identifiers.establishers.partnership.partner.PartnerNameId
import identifiers.establishers.partnership.partner.address.{AddressId, AddressYearsId, PreviousAddressId}
import identifiers.establishers.partnership.partner.contact.{EnterEmailId, EnterPhoneId}
import identifiers.establishers.partnership.partner.details._
import models._
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.Aliases._
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.HtmlContent
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import utils.Data.schemeName
import utils.UserAnswers

import java.time.LocalDate

class EstablisherPartnerDetailsCYAHelperSpec extends CYAHelperSpecBase {

  val cyaHelper = new EstablisherPartnerDetailsCYAHelper

  private val personName: PersonName = PersonName("Jane", "Doe")
  private val partnerName: String = personName.fullName
  private val partnerAddress = Address("addr1", "addr2", None, None, Some("ZZ11ZZ"), "GB")
  private val partnerPreviousAddress = Address("prevaddr1", "prevaddr2", None, None, Some("ZZ11ZZ"), "GB")
  private val formData: LocalDate =
    LocalDate.parse("2000-01-01")

  // scalastyle:off magic.number
  "EstablisherPartnerDetailsCYAHelper" must {
    "return all rows with correct change links, values and visually hidden text when user has answered yes for all questions" in {
      val ua: UserAnswers = UserAnswers()
        .setOrException(SchemeNameId, schemeName)
        .setOrException(PartnerNameId(0, 0), personName)
        .setOrException(PartnerDOBId(0, 0), formData)
        .setOrException(PartnerHasNINOId(0, 0), true)
        .setOrException(PartnerHasUTRId(0, 0), true)
        .setOrException(PartnerNINOId(0, 0), ReferenceValue("AB123456C"))
        .setOrException(PartnerEnterUTRId(0, 0), ReferenceValue("1234567890"))
        .setOrException(AddressId(0, 0), partnerAddress)
        .setOrException(AddressYearsId(0, 0), true)
        .setOrException(EnterEmailId(0, 0), "test@test.com")
        .setOrException(EnterPhoneId(0, 0), "123")

      val result = cyaHelper.detailsRows(0, 0)(dataRequest(ua), messages)

      result.head mustBe SummaryListRow(
        key = Key(Text(Messages("messages__partner__name", partnerName)), classes = "govuk-!-width-one-half"),
        value = Value(Text(Messages(partnerName))),
        actions = Some(Actions( items = Seq(ActionItem(
          content = HtmlContent(s"<span aria-hidden=true >${messages("site.change")}</span>"),
          href = routes.PartnerNameController.onPageLoad(0, 0, CheckMode).url,
          visuallyHiddenText = Some(Messages("site.change") + " " +
            Messages("messages__partner__name__cya__visuallyHidden", partnerName)),
          attributes = Map("id" -> "cya-0-0-change")
        ))))
      )

      result(1) mustBe SummaryListRow(
        key = Key(Text(Messages("messages__dob__h1", partnerName)), classes = "govuk-!-width-one-half"),
        value = Value(Text("1-1-2000")),
        actions = Some(Actions( items = Seq(ActionItem(
          content = HtmlContent(s"<span aria-hidden=true >${messages("site.change")}</span>"),
          href = detailsRoutes.PartnerDOBController.onPageLoad(0, 0, CheckMode).url,
          visuallyHiddenText = Some(Messages("site.change") + " " +
            Messages("messages__dob__cya__visuallyHidden", partnerName)),
          attributes = Map("id" -> "cya-0-1-change")
        ))))
      )


      result(2) mustBe summaryListRow(key = Messages("messages__hasNINO", partnerName),
        valueMsgKey = "booleanAnswer.true",
        Some(Link(text = Messages("site.change"),
          target = detailsRoutes.PartnerHasNINOController.onPageLoad(0, 0, CheckMode).url,
          visuallyHiddenText = Some(Text(Messages("site.change") + " " +
            Messages("messages__hasNINO__cya__visuallyHidden", partnerName))),
          attributes = Map("id" -> "cya-0-2-change")
        ))
      )

      result(3) mustBe SummaryListRow(
        key = Key(Text(Messages("messages__enterNINO__cya", partnerName)), classes = "govuk-!-width-one-half"),
        value = Value(Text("AB123456C")),
        actions = Some(Actions( items = Seq(ActionItem(
          content = HtmlContent(s"<span aria-hidden=true >${messages("site.change")}</span>"),
          href = detailsRoutes.PartnerEnterNINOController.onPageLoad(0, 0, CheckMode).url,
          visuallyHiddenText = Some(Messages("site.change") + " " +
            Messages("messages__hasNINO__cya__visuallyHidden", partnerName)),
          attributes = Map("id" -> "cya-0-3-change")
        ))))
      )

      result(4) mustBe summaryListRow(key = Messages("messages__hasUTR", partnerName),
        valueMsgKey = "booleanAnswer.true",
        Some(Link(text = Messages("site.change"),
          target = detailsRoutes.PartnerHasUTRController.onPageLoad(0, 0, CheckMode).url,
          visuallyHiddenText = Some(Text(Messages("site.change") + " " +
            Messages("messages__hasUTR__cya__visuallyHidden", partnerName))),
          attributes = Map("id" -> "cya-0-4-change")
        ))
      )

      result(5) mustBe SummaryListRow(
        key = Key(Text(Messages("messages__enterUTR__cya_label", partnerName)), classes = "govuk-!-width-one-half"),
        value = Value(Text("1234567890")),
        actions = Some(Actions( items = Seq(ActionItem(
          content = HtmlContent(s"<span aria-hidden=true >${messages("site.change")}</span>"),
          href = detailsRoutes.PartnerEnterUTRController.onPageLoad(0, 0, CheckMode).url,
          visuallyHiddenText = Some(Messages("site.change") + " " +
            Messages("messages__hasUTR__cya__visuallyHidden", partnerName)),
          attributes = Map("id" -> "cya-0-5-change")
        ))))
      )

      result(6) mustBe summaryListRowHtml(key = messages("addressList_cya_label", partnerName),
        value = answerAddressTransform(partnerAddress), Some(Link(text = Messages("site.change"),
          target = addressRoutes.EnterPostcodeController.onPageLoad(0, 0, CheckMode).url,
          visuallyHiddenText = Some(Text(Messages("site.change") + " " +
            Messages("messages__visuallyHidden__address", partnerName))),
          attributes = Map("id" -> "cya-0-6-change")))
      )

      result(7) mustBe summaryListRow(key = messages("addressYears.title", partnerName), valueMsgKey = "booleanAnswer.true",
        Some(Link(text = Messages("site.change"),
          target = addressRoutes.AddressYearsController.onPageLoad(0, 0, CheckMode).url,
          visuallyHiddenText = Some(Text(Messages("site.change") + " " +
            Messages("messages__visuallyhidden__addressYears", partnerName))),
          attributes = Map("id" -> "cya-0-7-change")))
      )

      result(8) mustBe SummaryListRow(
        key = Key(Text(Messages("messages__enterEmail_cya_label", partnerName)), classes = "govuk-!-width-one-half"),
        value = Value(Text("test@test.com")),
        actions = Some(Actions( items = Seq(ActionItem(
          content = HtmlContent(s"<span aria-hidden=true >${messages("site.change")}</span>"),
          href = contactRoutes.EnterEmailController.onPageLoad(0, 0, CheckMode).url,
          visuallyHiddenText = Some(Messages("site.change") + " " +
            Messages("messages__enterEmail__cya__visuallyHidden", partnerName)),
          attributes = Map("id" -> "cya-0-8-change")
        ))))
      )

      result(9) mustBe SummaryListRow(
        key = Key(Text(Messages("messages__enterPhone_cya_label", partnerName)), classes = "govuk-!-width-one-half"),
        value = Value(Text("123")),
        actions = Some(Actions( items = Seq(ActionItem(
          content = HtmlContent(s"<span aria-hidden=true >${messages("site.change")}</span>"),
          href = contactRoutes.EnterPhoneNumberController.onPageLoad(0, 0, CheckMode).url,
          visuallyHiddenText = Some(Messages("site.change") + " " +
            Messages("messages__enterPhone__cya__visuallyHidden", partnerName)),
          attributes = Map("id" -> "cya-0-9-change")
        ))))
      )

    }

    "return all rows with correct change links, values and visually hidden text when user has answered no for all questions" in {
      val ua: UserAnswers = UserAnswers()
        .setOrException(SchemeNameId, schemeName)
        .setOrException(PartnerNameId(0, 0), personName)
        .setOrException(PartnerDOBId(0, 0), formData)
        .setOrException(PartnerHasNINOId(0, 0), false)
        .setOrException(PartnerHasUTRId(0, 0), false)
        .setOrException(PartnerNoNINOReasonId(0, 0), "test")
        .setOrException(PartnerNoUTRReasonId(0, 0), "test")
        .setOrException(AddressId(0, 0), partnerAddress)
        .setOrException(AddressYearsId(0, 0), false)
        .setOrException(PreviousAddressId(0, 0), partnerPreviousAddress)
        .setOrException(EnterEmailId(0, 0), "test@test.com")
        .setOrException(EnterPhoneId(0, 0), "123")

      val result = cyaHelper.detailsRows(0, 0)(dataRequest(ua), messages)

      result.head mustBe SummaryListRow(
        key = Key(Text(Messages("messages__partner__name", partnerName)), classes = "govuk-!-width-one-half"),
        value = Value(Text(Messages(partnerName))),
        actions = Some(Actions( items = Seq(ActionItem(
          content = HtmlContent(s"<span aria-hidden=true >${messages("site.change")}</span>"),
          href = routes.PartnerNameController.onPageLoad(0, 0, CheckMode).url,
          visuallyHiddenText = Some(Messages("site.change") + " " +
            Messages("messages__partner__name__cya__visuallyHidden", partnerName)),
          attributes = Map("id" -> "cya-0-0-change")
        ))))
      )

      result(1) mustBe SummaryListRow(
        key = Key(Text(Messages("messages__dob__h1", partnerName)), classes = "govuk-!-width-one-half"),
        value = Value(Text("1-1-2000")),
        actions = Some(Actions( items = Seq(ActionItem(
          content = HtmlContent(s"<span aria-hidden=true >${messages("site.change")}</span>"),
          href = detailsRoutes.PartnerDOBController.onPageLoad(0, 0, CheckMode).url,
          visuallyHiddenText = Some(Messages("site.change") + " " +
            Messages("messages__dob__cya__visuallyHidden", partnerName)),
          attributes = Map("id" -> "cya-0-1-change")
        ))))
      )


      result(2) mustBe summaryListRow(key = Messages("messages__hasNINO", partnerName),
        valueMsgKey = "booleanAnswer.false",
        Some(Link(text = Messages("site.change"),
          target = detailsRoutes.PartnerHasNINOController.onPageLoad(0, 0, CheckMode).url,
          visuallyHiddenText = Some(Text(Messages("site.change") + " " +
            Messages("messages__hasNINO__cya__visuallyHidden", partnerName))),
          attributes = Map("id" -> "cya-0-2-change")
        ))
      )

      result(3) mustBe SummaryListRow(
        key = Key(Text(Messages("messages__whyNoNINO", partnerName)), classes = "govuk-!-width-one-half"),
        value = Value(Text("test")),
        actions = Some(Actions( items = Seq(ActionItem(
          content = HtmlContent(s"<span aria-hidden=true >${messages("site.change")}</span>"),
          href = detailsRoutes.PartnerNoNINOReasonController.onPageLoad(0, 0, CheckMode).url,
          visuallyHiddenText = Some(Messages("site.change") + " " +
            Messages("messages__whyNoNINO__cya__visuallyHidden", partnerName)),
          attributes = Map("id" -> "cya-0-3-change")
        ))))
      )


      result(4) mustBe summaryListRow(key = Messages("messages__hasUTR", partnerName),
        valueMsgKey = "booleanAnswer.false",
        Some(Link(text = Messages("site.change"),
          target = detailsRoutes.PartnerHasUTRController.onPageLoad(0, 0, CheckMode).url,
          visuallyHiddenText = Some(Text(Messages("site.change") + " " +
            Messages("messages__hasUTR__cya__visuallyHidden", partnerName))),
          attributes = Map("id" -> "cya-0-4-change")
        ))
      )

      result(5) mustBe SummaryListRow(
        key = Key(Text(Messages("messages__whyNoUTR", partnerName)), classes = "govuk-!-width-one-half"),
        value = Value(Text("test")),
        actions = Some(Actions( items = Seq(ActionItem(
          content = HtmlContent(s"<span aria-hidden=true >${messages("site.change")}</span>"),
          href = detailsRoutes.PartnerNoUTRReasonController.onPageLoad(0, 0, CheckMode).url,
          visuallyHiddenText = Some(Messages("site.change") + " " +
            Messages("messages__whyNoUTR__cya__visuallyHidden", partnerName)),
          attributes = Map("id" -> "cya-0-5-change")
        ))))
      )

      result(6) mustBe summaryListRowHtml(key = messages("addressList_cya_label", partnerName),
        value = answerAddressTransform(partnerAddress), Some(Link(text = Messages("site.change"),
          target = addressRoutes.EnterPostcodeController.onPageLoad(0, 0, CheckMode).url,
          visuallyHiddenText = Some(Text(Messages("site.change") + " " +
            Messages("messages__visuallyHidden__address", partnerName))),
          attributes = Map("id" -> "cya-0-6-change")))
      )

      result(7) mustBe summaryListRow(key = messages("addressYears.title", partnerName), valueMsgKey = "booleanAnswer.false",
        Some(Link(text = Messages("site.change"),
          target = addressRoutes.AddressYearsController.onPageLoad(0, 0, CheckMode).url,
          visuallyHiddenText = Some(Text(Messages("site.change") + " " +
            Messages("messages__visuallyhidden__addressYears", partnerName))),
          attributes = Map("id" -> "cya-0-7-change")))
      )


      result(8) mustBe summaryListRowHtml(key = messages("previousAddressList_cya_label", partnerName),
        value = answerAddressTransform(partnerPreviousAddress), Some(Link(text = Messages("site.change"),
          target = addressRoutes.EnterPreviousPostcodeController.onPageLoad(0, 0, CheckMode).url,
          visuallyHiddenText = Some(Text(Messages("site.change") + " " +
            Messages("messages__visuallyHidden__previousAddress", partnerName))),
          attributes = Map("id" -> "cya-0-8-change")))
      )

      result(9) mustBe SummaryListRow(
        key = Key(Text(Messages("messages__enterEmail_cya_label", partnerName)), classes = "govuk-!-width-one-half"),
        value = Value(Text("test@test.com")),
        actions = Some(Actions( items = Seq(ActionItem(
          content = HtmlContent(s"<span aria-hidden=true >${messages("site.change")}</span>"),
          href = contactRoutes.EnterEmailController.onPageLoad(0, 0, CheckMode).url,
          visuallyHiddenText = Some(Messages("site.change") + " " +
            Messages("messages__enterEmail__cya__visuallyHidden", partnerName)),
          attributes = Map("id" -> "cya-0-9-change")
        ))))
      )

      result(10) mustBe SummaryListRow(
        key = Key(Text(Messages("messages__enterPhone_cya_label", partnerName)), classes = "govuk-!-width-one-half"),
        value = Value(Text("123")),
        actions = Some(Actions( items = Seq(ActionItem(
          content = HtmlContent(s"<span aria-hidden=true >${messages("site.change")}</span>"),
          href = contactRoutes.EnterPhoneNumberController.onPageLoad(0, 0, CheckMode).url,
          visuallyHiddenText = Some(Messages("site.change") + " " +
            Messages("messages__enterPhone__cya__visuallyHidden", partnerName)),
          attributes = Map("id" -> "cya-0-10-change")
        ))))
      )

    }
  }
}
