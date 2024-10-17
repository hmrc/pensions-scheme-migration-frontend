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

import controllers.establishers.company.director.address.{routes => addressRoutes}
import controllers.establishers.company.director.contact.{routes => contactRoutes}
import controllers.establishers.company.director.details.{routes => detailsRoutes}
import controllers.establishers.company.director.routes
import helpers.CYAHelperSpecBase
import helpers.cya.establishers.company.EstablisherCompanyDirectorDetailsCYAHelper
import identifiers.beforeYouStart.SchemeNameId
import identifiers.establishers.company.director.DirectorNameId
import identifiers.establishers.company.director.address.{AddressId, AddressYearsId, PreviousAddressId}
import identifiers.establishers.company.director.contact.{EnterEmailId, EnterPhoneId}
import identifiers.establishers.company.director.details._
import models._
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.Aliases._
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.Key
import utils.Data.schemeName
import utils.UserAnswers

import java.time.LocalDate

class EstablisherCompanyDirectorDetailsCYAHelperSpec extends CYAHelperSpecBase {

  val cyaHelper = new EstablisherCompanyDirectorDetailsCYAHelper

  private val personName: PersonName = PersonName("Jane", "Doe")
  private val directorName: String = personName.fullName
  private val directorAddress = Address("addr1", "addr2", None, None, Some("ZZ11ZZ"), "GB")
  private val directorPreviousAddress = Address("prevaddr1", "prevaddr2", None, None, Some("ZZ11ZZ"), "GB")
  private val formData: LocalDate =
    LocalDate.parse("2000-01-01")
  
  // scalastyle:off magic.number
  "EstablisherCompanyDirectorDetailsCYAHelper" must {
    "return all rows with correct change links, values and visually hidden text when user has answered yes for all questions" in {
      val ua: UserAnswers = UserAnswers()
        .setOrException(SchemeNameId, schemeName)
        .setOrException(DirectorNameId(0, 0), personName)
        .setOrException(DirectorDOBId(0, 0), formData)
        .setOrException(DirectorHasNINOId(0, 0), true)
        .setOrException(DirectorHasUTRId(0, 0), true)
        .setOrException(DirectorNINOId(0, 0), ReferenceValue("AB123456C"))
        .setOrException(DirectorEnterUTRId(0, 0), ReferenceValue("1234567890"))
        .setOrException(AddressId(0, 0), directorAddress)
        .setOrException(AddressYearsId(0, 0), true)
        .setOrException(EnterEmailId(0, 0), "test@test.com")
        .setOrException(EnterPhoneId(0, 0), "123")

      val result = cyaHelper.detailsRows(0, 0)(dataRequest(ua), messages)

      result.head mustBe SummaryListRow(
        key = Key(Text(Messages("messages__director__name", directorName)), classes = "govuk-!-width-one-half"),
        value = Value(Text(Messages(directorName))),
        actions = Some(Actions( items = Seq(ActionItem(
          content = HtmlContent(s"<span aria-hidden=true >${messages("site.change")}</span>"),
          href = routes.DirectorNameController.onPageLoad(0, 0, CheckMode).url,
          visuallyHiddenText = Some(Messages("site.change") + " " +
            Messages("messages__director__name__cya__visuallyHidden", directorName)),
          attributes = Map("id" -> "cya-0-0-change")
        ))))
      )

      result(1) mustBe SummaryListRow(
        key = Key(Text(Messages("messages__dob__h1", directorName)), classes = "govuk-!-width-one-half"),
        value = Value(Text("1-1-2000")),
        actions = Some(Actions( items = Seq(ActionItem(
          content = HtmlContent(s"<span aria-hidden=true >${messages("site.change")}</span>"),
          href = detailsRoutes.DirectorDOBController.onPageLoad(0, 0, CheckMode).url,
          visuallyHiddenText = Some(Messages("site.change") + " " +
            Messages("messages__dob__cya__visuallyHidden", directorName)),
          attributes = Map("id" -> "cya-0-1-change")
        ))))
      )


      result(2) mustBe summaryListRow(key = Messages("messages__hasNINO", directorName),
        valueMsgKey = "booleanAnswer.true",
        Some(Link(text = Messages("site.change"),
          target = detailsRoutes.DirectorHasNINOController.onPageLoad(0, 0, CheckMode).url,
          visuallyHiddenText = Some(Text(Messages("site.change") + " " +
            Messages("messages__hasNINO__cya__visuallyHidden", directorName))),
          attributes = Map("id" -> "cya-0-2-change")
        ))
      )

      result(3) mustBe SummaryListRow(
        key = Key(Text(Messages("messages__enterNINO__cya", directorName)), classes = "govuk-!-width-one-half"),
        value = Value(Text("AB123456C")),
        actions = Some(Actions( items = Seq(ActionItem(
          content = HtmlContent(s"<span aria-hidden=true >${messages("site.change")}</span>"),
          href = detailsRoutes.DirectorEnterNINOController.onPageLoad(0, 0, CheckMode).url,
          visuallyHiddenText = Some(Messages("site.change") + " " +
            Messages("messages__hasNINO__cya__visuallyHidden", directorName)),
          attributes = Map("id" -> "cya-0-3-change")
        ))))
      )

      result(4) mustBe summaryListRow(key = Messages("messages__hasUTR", directorName),
        valueMsgKey = "booleanAnswer.true",
        Some(Link(text = Messages("site.change"),
          target = detailsRoutes.DirectorHasUTRController.onPageLoad(0, 0, CheckMode).url,
          visuallyHiddenText = Some(Text(Messages("site.change") + " " +
            Messages("messages__hasUTR__cya__visuallyHidden", directorName))),
          attributes = Map("id" -> "cya-0-4-change")
        ))
      )

      result(5) mustBe SummaryListRow(
        key = Key(Text(Messages("messages__enterUTR__cya_label", directorName)), classes = "govuk-!-width-one-half"),
        value = Value(Text("1234567890")),
        actions = Some(Actions( items = Seq(ActionItem(
          content = HtmlContent(s"<span aria-hidden=true >${messages("site.change")}</span>"),
          href = detailsRoutes.DirectorEnterUTRController.onPageLoad(0, 0, CheckMode).url,
          visuallyHiddenText = Some(Messages("site.change") + " " +
            Messages("messages__hasUTR__cya__visuallyHidden", directorName)),
          attributes = Map("id" -> "cya-0-5-change")
        ))))
      )

      result(6) mustBe summaryListRowHtml(key = messages("addressList_cya_label", directorName),
        value = answerAddressTransform(directorAddress), Some(Link(text = Messages("site.change"),
          target = addressRoutes.EnterPostcodeController.onPageLoad(0, 0, CheckMode).url,
          visuallyHiddenText = Some(Text(Messages("site.change") + " " +
            Messages("messages__visuallyHidden__address", directorName))),
          attributes = Map("id" -> "cya-0-6-change")))
      )

      result(7) mustBe summaryListRow(key = messages("addressYears.title", directorName), valueMsgKey = "booleanAnswer.true",
        Some(Link(text = Messages("site.change"),
          target = addressRoutes.AddressYearsController.onPageLoad(0, 0, CheckMode).url,
          visuallyHiddenText = Some(Text(Messages("site.change") + " " +
            Messages("messages__visuallyhidden__addressYears", directorName))),
          attributes = Map("id" -> "cya-0-7-change")))
      )

      result(8) mustBe SummaryListRow(
        key = Key(Text(Messages("messages__enterEmail_cya_label", directorName)), classes = "govuk-!-width-one-half"),
        value = Value(Text("test@test.com")),
        actions = Some(Actions( items = Seq(ActionItem(
          content = HtmlContent(s"<span aria-hidden=true >${messages("site.change")}</span>"),
          href = contactRoutes.EnterEmailController.onPageLoad(0, 0, CheckMode).url,
          visuallyHiddenText = Some(Messages("site.change") + " " +
            Messages("messages__enterEmail__cya__visuallyHidden", directorName)),
          attributes = Map("id" -> "cya-0-8-change")
        ))))
      )

      result(9) mustBe SummaryListRow(
        key = Key(Text(Messages("messages__enterPhone_cya_label", directorName)), classes = "govuk-!-width-one-half"),
        value = Value(Text("123")),
        actions = Some(Actions( items = Seq(ActionItem(
          content = HtmlContent(s"<span aria-hidden=true >${messages("site.change")}</span>"),
          href = contactRoutes.EnterPhoneNumberController.onPageLoad(0, 0, CheckMode).url,
          visuallyHiddenText = Some(Messages("site.change") + " " +
            Messages("messages__enterPhone__cya__visuallyHidden", directorName)),
          attributes = Map("id" -> "cya-0-9-change")
        ))))
      )

    }

    "return all rows with correct change links, values and visually hidden text when user has answered no for all questions" in {
      val ua: UserAnswers = UserAnswers()
        .setOrException(SchemeNameId, schemeName)
        .setOrException(DirectorNameId(0, 0), personName)
        .setOrException(DirectorDOBId(0, 0), formData)
        .setOrException(DirectorHasNINOId(0, 0), false)
        .setOrException(DirectorHasUTRId(0, 0), false)
        .setOrException(DirectorNoNINOReasonId(0, 0), "test")
        .setOrException(DirectorNoUTRReasonId(0, 0), "test")
        .setOrException(AddressId(0, 0), directorAddress)
        .setOrException(AddressYearsId(0, 0), false)
        .setOrException(PreviousAddressId(0, 0), directorPreviousAddress)
        .setOrException(EnterEmailId(0, 0), "test@test.com")
        .setOrException(EnterPhoneId(0, 0), "123")

      val result = cyaHelper.detailsRows(0, 0)(dataRequest(ua), messages)

      result.head mustBe SummaryListRow(
        key = Key(Text(Messages("messages__director__name", directorName)), classes = "govuk-!-width-one-half"),
        value = Value(Text(Messages(directorName))),
        actions = Some(Actions( items = Seq(ActionItem(
          content = HtmlContent(s"<span aria-hidden=true >${messages("site.change")}</span>"),
          href = routes.DirectorNameController.onPageLoad(0, 0, CheckMode).url,
          visuallyHiddenText = Some(Messages("site.change") + " " +
            Messages("messages__director__name__cya__visuallyHidden", directorName)),
          attributes = Map("id" -> "cya-0-0-change")
        ))))
      )

      result(1) mustBe SummaryListRow(
        key = Key(Text(Messages("messages__dob__h1", directorName)), classes = "govuk-!-width-one-half"),
        value = Value(Text("1-1-2000")),
        actions = Some(Actions( items = Seq(ActionItem(
          content = HtmlContent(s"<span aria-hidden=true >${messages("site.change")}</span>"),
          href = detailsRoutes.DirectorDOBController.onPageLoad(0, 0, CheckMode).url,
          visuallyHiddenText = Some(Messages("site.change") + " " +
            Messages("messages__dob__cya__visuallyHidden", directorName)),
          attributes = Map("id" -> "cya-0-1-change")
        ))))
      )


      result(2) mustBe summaryListRow(key = Messages("messages__hasNINO", directorName),
        valueMsgKey = "booleanAnswer.false",
        Some(Link(text = Messages("site.change"),
          target = detailsRoutes.DirectorHasNINOController.onPageLoad(0, 0, CheckMode).url,
          visuallyHiddenText = Some(Text(Messages("site.change") + " " +
            Messages("messages__hasNINO__cya__visuallyHidden", directorName))),
          attributes = Map("id" -> "cya-0-2-change")
        ))
      )

      result(3) mustBe SummaryListRow(
        key = Key(Text(Messages("messages__whyNoNINO", directorName)), classes = "govuk-!-width-one-half"),
        value = Value(Text("test")),
        actions = Some(Actions( items = Seq(ActionItem(
          content = HtmlContent(s"<span aria-hidden=true >${messages("site.change")}</span>"),
          href = detailsRoutes.DirectorNoNINOReasonController.onPageLoad(0, 0, CheckMode).url,
          visuallyHiddenText = Some(Messages("site.change") + " " +
            Messages("messages__whyNoNINO__cya__visuallyHidden", directorName)),
          attributes = Map("id" -> "cya-0-3-change")
        ))))
      )


      result(4) mustBe summaryListRow(key = Messages("messages__hasUTR", directorName),
        valueMsgKey = "booleanAnswer.false",
        Some(Link(text = Messages("site.change"),
          target = detailsRoutes.DirectorHasUTRController.onPageLoad(0, 0, CheckMode).url,
          visuallyHiddenText = Some(Text(Messages("site.change") + " " +
            Messages("messages__hasUTR__cya__visuallyHidden", directorName))),
          attributes = Map("id" -> "cya-0-4-change")
        ))
      )

      result(5) mustBe SummaryListRow(
        key = Key(Text(Messages("messages__whyNoUTR", directorName)), classes = "govuk-!-width-one-half"),
        value = Value(Text("test")),
        actions = Some(Actions( items = Seq(ActionItem(
          content = HtmlContent(s"<span aria-hidden=true >${messages("site.change")}</span>"),
          href = detailsRoutes.DirectorNoUTRReasonController.onPageLoad(0, 0, CheckMode).url,
          visuallyHiddenText = Some(Messages("site.change") + " " +
            Messages("messages__whyNoUTR__cya__visuallyHidden", directorName)),
          attributes = Map("id" -> "cya-0-5-change")
        ))))
      )

      result(6) mustBe summaryListRowHtml(key = messages("addressList_cya_label", directorName),
        value = answerAddressTransform(directorAddress), Some(Link(text = Messages("site.change"),
          target = addressRoutes.EnterPostcodeController.onPageLoad(0, 0, CheckMode).url,
          visuallyHiddenText = Some(Text(Messages("site.change") + " " +
            Messages("messages__visuallyHidden__address", directorName))),
          attributes = Map("id" -> "cya-0-6-change")))
      )

      result(7) mustBe summaryListRow(key = messages("addressYears.title", directorName), valueMsgKey = "booleanAnswer.false",
        Some(Link(text = Messages("site.change"),
          target = addressRoutes.AddressYearsController.onPageLoad(0, 0, CheckMode).url,
          visuallyHiddenText = Some(Text(Messages("site.change") + " " +
            Messages("messages__visuallyhidden__addressYears", directorName))),
          attributes = Map("id" -> "cya-0-7-change")))
      )


      result(8) mustBe summaryListRowHtml(key = messages("previousAddressList_cya_label", directorName),
        value = answerAddressTransform(directorPreviousAddress), Some(Link(text = Messages("site.change"),
          target = addressRoutes.EnterPreviousPostcodeController.onPageLoad(0, 0, CheckMode).url,
          visuallyHiddenText = Some(Text(Messages("site.change") + " " +
            Messages("messages__visuallyHidden__previousAddress", directorName))),
          attributes = Map("id" -> "cya-0-8-change")))
      )

      result(9) mustBe SummaryListRow(
        key = Key(Text(Messages("messages__enterEmail_cya_label", directorName)), classes = "govuk-!-width-one-half"),
        value = Value(Text("test@test.com")),
        actions = Some(Actions( items = Seq(ActionItem(
          content = HtmlContent(s"<span aria-hidden=true >${messages("site.change")}</span>"),
          href = contactRoutes.EnterEmailController.onPageLoad(0, 0, CheckMode).url,
          visuallyHiddenText = Some(Messages("site.change") + " " +
            Messages("messages__enterEmail__cya__visuallyHidden", directorName)),
          attributes = Map("id" -> "cya-0-9-change")
        ))))
      )

      result(10) mustBe SummaryListRow(
        key = Key(Text(Messages("messages__enterPhone_cya_label", directorName)), classes = "govuk-!-width-one-half"),
        value = Value(Text("123")),
        actions = Some(Actions( items = Seq(ActionItem(
          content = HtmlContent(s"<span aria-hidden=true >${messages("site.change")}</span>"),
          href = contactRoutes.EnterPhoneNumberController.onPageLoad(0, 0, CheckMode).url,
          visuallyHiddenText = Some(Messages("site.change") + " " +
            Messages("messages__enterPhone__cya__visuallyHidden", directorName)),
          attributes = Map("id" -> "cya-0-10-change")
        ))))
      )

    }
  }
}
