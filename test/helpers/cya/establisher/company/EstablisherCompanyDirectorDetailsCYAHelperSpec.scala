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

package helpers.cya.establisher.company

import base.SpecBase._
import controllers.establishers.company.director.address.{routes => addressRoutes}
import controllers.establishers.company.director.contact.{routes => contactRoutes}
import controllers.establishers.company.director.details.{routes => detailsRoutes}
import controllers.establishers.company.director.routes
import helpers.cya.establishers.company.EstablisherCompanyDirectorDetailsCYAHelper
import identifiers.beforeYouStart.SchemeNameId
import identifiers.establishers.company.director.DirectorNameId
import identifiers.establishers.company.director.address.{AddressId, AddressYearsId, PreviousAddressId}
import identifiers.establishers.company.director.contact.{EnterEmailId, EnterPhoneId}
import identifiers.establishers.company.director.details._
import models._
import models.requests.DataRequest
import org.scalatest.TryValues
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.i18n.Messages
import play.api.mvc.AnyContent
import uk.gov.hmrc.domain.PsaId
import uk.gov.hmrc.viewmodels.SummaryList.{Action, Key, Row, Value}
import uk.gov.hmrc.viewmodels.Text.{Literal, Message => GovUKMsg}
import uk.gov.hmrc.viewmodels.{Html, MessageInterpolators, SummaryList, Text}
import utils.Data.{credId, psaId, pstr, schemeName}
import utils.{Enumerable, UserAnswers}
import viewmodels.Message

import java.time.LocalDate

class EstablisherCompanyDirectorDetailsCYAHelperSpec extends AnyWordSpec with Matchers with TryValues with Enumerable.Implicits {

  val cyaHelper = new EstablisherCompanyDirectorDetailsCYAHelper

  private val personName: PersonName = PersonName("Jane", "Doe")
  private val directorName: String = personName.fullName
  private val directorAddress = Address("addr1", "addr2", None, None, Some("ZZ11ZZ"), "GB")
  private val directorPreviousAddress = Address("prevaddr1", "prevaddr2", None, None, Some("ZZ11ZZ"), "GB")
  private val formData: LocalDate =
    LocalDate.parse("2000-01-01")

  case class Link(text: String, target: String, visuallyHiddenText: Option[Text] = None,
                  attributes: Map[String, String] = Map.empty)

  private def summaryListRow(key: String, valueMsgKey: String, target: Option[Link]): Row = {
    SummaryList.Row(Key(GovUKMsg(key), List("govuk-!-width-one-half")), Value(GovUKMsg(valueMsgKey)), target.toSeq.map(
      t => Action(content = Html(s"<span aria-hidden=true >${t.text}</span>"), href = t.target,
        visuallyHiddenText = t.visuallyHiddenText, attributes = t.attributes)))
  }

  private def summaryListRowHtml(key: String, value: Html, target: Option[Link]): Row = {
    SummaryList.Row(Key(GovUKMsg(key), List("govuk-!-width-one-half")), Value(value), target.toSeq.map(
      t => Action(content = Html(s"<span aria-hidden=true >${t.text}</span>"), href = t.target,
        visuallyHiddenText = t.visuallyHiddenText, attributes = t.attributes)))
  }
  private def answerAddressTransform(addr: Address)(implicit messages: Messages): Html = addressAnswer(addr)

  private def addressAnswer(addr: Address)(implicit messages: Messages): Html = {
    def addrLineToHtml(l: String): String = s"""<span class="govuk-!-display-block">$l</span>"""

    Html(addrLineToHtml(addr.addressLine1) + addrLineToHtml(addr.addressLine2) + addr.addressLine3
      .fold("")(addrLineToHtml) + addr.addressLine4.fold("")(addrLineToHtml) + addr.postcode
      .fold("")(addrLineToHtml) + addrLineToHtml(messages("country." + addr.country)))
  }

  private def dataRequest(ua: UserAnswers): DataRequest[AnyContent] = DataRequest[AnyContent](request = fakeRequest, userAnswers = ua,
    psaId = PsaId(psaId), lock = MigrationLock(pstr = pstr, credId = credId, psaId = psaId), viewOnly = false)

  // scalastyle:off magic.number
  "EstablisherCompanyDetailsCYAHelper" must {
    "return all rows with correct change links, values and visually hidden text when user has answered yes for all questions" in {
      val ua: UserAnswers = UserAnswers()
        .setOrException(SchemeNameId, schemeName)
        .setOrException(DirectorNameId(0,0), personName)
        .setOrException(DirectorDOBId(0,0), formData)
        .setOrException(DirectorHasNINOId(0,0), true)
        .setOrException(DirectorHasUTRId(0,0), true)
        .setOrException(DirectorNINOId(0,0), ReferenceValue("AB123456C"))
        .setOrException(DirectorEnterUTRId(0,0), ReferenceValue("1234567890"))
        .setOrException(AddressId(0,0), directorAddress)
        .setOrException(AddressYearsId(0,0), false)
        .setOrException(PreviousAddressId(0,0), directorPreviousAddress)
        .setOrException(EnterEmailId(0,0), "test@test.com")
        .setOrException(EnterPhoneId(0,0), "123")

      val result = cyaHelper.detailsRows(0,0)(dataRequest(ua), messages)

      result.head mustBe Row(
        key = Key(msg"${Message("messages__director__name", directorName).resolve}", classes = Seq("govuk-!-width-one-half")),
        value = Value(msg"${Message(directorName).resolve}"),
        actions = Seq(Action(
          content = Html(s"<span aria-hidden=true >${messages("site.change")}</span>"),
          href = routes.DirectorNameController.onPageLoad(0, 0, CheckMode).url,
          visuallyHiddenText = Some(Literal(Messages("site.change") + " " +
            Messages("messages__director__name__cya__visuallyHidden", directorName))),
          attributes = Map("id" -> "cya-0-0-change")
        ))
      )

      result(1) mustBe Row(
        key = Key(msg"${Message("messages__dob__h1", directorName).resolve}", classes = Seq("govuk-!-width-one-half")),
        value = Value(Literal("1-1-2000")),
        actions = Seq(Action(
          content = Html(s"<span aria-hidden=true >${messages("site.change")}</span>"),
          href = detailsRoutes.DirectorDOBController.onPageLoad(0, 0, CheckMode).url,
          visuallyHiddenText = Some(Literal(Messages("site.change") + " " +
            Messages("messages__dob__cya__visuallyHidden", directorName))),
          attributes = Map("id" -> "cya-0-1-change")
        ))
      )


      result(2) mustBe summaryListRow(key = Messages("messages__hasNINO", directorName),
        valueMsgKey = "booleanAnswer.true",
        Some(Link(text = Messages("site.change"),
        target = detailsRoutes.DirectorHasNINOController.onPageLoad(0, 0, CheckMode).url,
          visuallyHiddenText = Some(Literal(Messages("site.change") + " " +
            Messages("messages__hasNINO__cya__visuallyHidden", directorName))),
          attributes = Map("id" -> "cya-0-2-change")
        ))
      )

      result(3) mustBe Row(
        key = Key(msg"${Message("messages__enterNINO__cya", directorName).resolve}", classes = Seq("govuk-!-width-one-half")),
        value = Value(msg"AB123456C"),
        actions = Seq(Action(
          content = Html(s"<span aria-hidden=true >${messages("site.change")}</span>"),
          href = detailsRoutes.DirectorEnterNINOController.onPageLoad(0,0, CheckMode).url,
          visuallyHiddenText = Some(Literal(Messages("site.change") + " " +
            Messages("messages__hasNINO__cya__visuallyHidden", directorName))),
          attributes = Map("id" -> "cya-0-3-change")
        ))
      )


      result(4) mustBe Row(
        key = Key(msg"${Message("messages__hasUTR__cya_label", directorName).resolve}", classes = Seq("govuk-!-width-one-half")),
        value = Value(Literal("Yes")),
        actions = Seq(Action(
          content = Html(s"<span aria-hidden=true >${messages("site.change")}</span>"),
          href = detailsRoutes.DirectorHasUTRController.onPageLoad(0, 0, CheckMode).url,
          visuallyHiddenText = Some(Literal(Messages("site.change") + " " +
            Messages("messages__hasUTR__cya__visuallyHidden", directorName))),
          attributes = Map("id" -> "cya-0-4-change")
        ))
      )

      result(5) mustBe Row(
        key = Key(msg"${Message("messages__enterUTR__cya_label", directorName).resolve}", classes = Seq("govuk-!-width-one-half")),
        value = Value(msg"1234567890"),
        actions = Seq(Action(
          content = Html(s"<span aria-hidden=true >${messages("site.change")}</span>"),
          href = detailsRoutes.DirectorEnterUTRController.onPageLoad(0,0, CheckMode).url,
          visuallyHiddenText = Some(Literal(Messages("site.change") + " " +
            Messages("messages__hasUTR__cya__visuallyHidden", directorName))),
          attributes = Map("id" -> "cya-0-5-change")
        ))
      )

      result(6) mustBe summaryListRowHtml(key = messages("addressList_cya_label", directorName),
        value = answerAddressTransform(directorAddress), Some(Link(text = Messages("site.change"),
          target = addressRoutes.EnterPostcodeController.onPageLoad(0, 0,CheckMode).url,
          visuallyHiddenText = Some(Literal(Messages("site.change") + " " +
            Messages("messages__visuallyHidden__address", directorName))),
          attributes = Map("id" -> "cya-0-6-change")))
      )

      result(7) mustBe summaryListRow(key = messages("aaddressYears.title", directorName), valueMsgKey = "booleanAnswer.false",
        Some(Link(text = Messages("site.change"),
          target = addressRoutes.AddressYearsController.onPageLoad(0, 0,CheckMode).url,
          visuallyHiddenText = Some(Literal(Messages("site.change") + " " +
            Messages("messages__visuallyhidden__addressYears", directorName))),
          attributes = Map("id" -> "cya-0-7-change")))
      )


      result(8) mustBe summaryListRowHtml(key = messages("previousAddressList_cya_label", directorName),
        value = answerAddressTransform(directorPreviousAddress), Some(Link(text = Messages("site.change"),
          target = addressRoutes.EnterPreviousPostcodeController.onPageLoad(0, 0,CheckMode).url,
          visuallyHiddenText = Some(Literal(Messages("site.change") + " " +
            Messages("messages__visuallyHidden__previousAddress", directorName))),
          attributes = Map("id" -> "cya-0-8-change")))
      )

      result(9) mustBe Row(
        key = Key(msg"${Message("messages__enterEmail_cya_label", directorName).resolve}", classes = Seq("govuk-!-width-one-half")),
        value = Value(Literal("test@test.com")),
        actions = Seq(Action(
          content = Html(s"<span aria-hidden=true >${messages("site.change")}</span>"),
          href = contactRoutes.EnterEmailController.onPageLoad(0, 0, CheckMode).url,
          visuallyHiddenText = Some(Literal(Messages("site.change") + " " +
            Messages("messages__enterEmail__cya__visuallyHidden", directorName))),
          attributes = Map("id" -> "cya-0-0-change")
        ))
      )

      result(10) mustBe Row(
        key = Key(msg"${Message("messages__enterPhone_cya_label", directorName).resolve}", classes = Seq("govuk-!-width-one-half")),
        value = Value(Literal("123")),
        actions = Seq(Action(
          content = Html(s"<span aria-hidden=true >${messages("site.change")}</span>"),
          href = contactRoutes.EnterPhoneNumberController.onPageLoad(0, 0,CheckMode).url,
          visuallyHiddenText = Some(Literal(Messages("site.change") + " " +
            Messages("messages__enterPhone__cya__visuallyHidden", directorName))),
          attributes = Map("id" -> "cya-0-1-change")
        ))
      )

    }

//    "return all rows with correct change links, values and visually hidden text when user has answered no for all questions" in {
//      val ua: UserAnswers = UserAnswers()
//        .setOrException(SchemeNameId, schemeName)
//        .setOrException(CompanyDetailsId(0), company)
//        .setOrException(HaveCompanyNumberId(0), false)
//        .setOrException(HaveUTRId(0), false)
//        .setOrException(HaveVATId(0), false)
//        .setOrException(HavePAYEId(0), false)
//        .setOrException(NoCompanyNumberReasonId(0), "reason-company-number")
//        .setOrException(NoUTRReasonId(0), "reason-utr")
//
//      val result = cyaHelper.detailsRows(0,0)(dataRequest(ua), messages)
//
//      result.head mustBe Row(
//        key = Key(msg"${Message("messages__haveCompanyNumber", personName).resolve}", classes = Seq("govuk-!-width-one-half")),
//        value = Value(Literal("No")),
//        actions = Seq(Action(
//          content = Html(s"<span aria-hidden=true >${messages("site.change")}</span>"),
//          href = routes.HaveCompanyNumberController.onPageLoad(0, CheckMode).url,
//          visuallyHiddenText = Some(Literal(Messages("site.change") + " " +
//            Messages("messages__haveCompanyNumber__cya__visuallyHidden", personName))),
//          attributes = Map("id" -> "cya-0-0-change")
//        ))
//      )
//
//      result(1) mustBe Row(
//        key = Key(msg"${Message("messages__whyNoCompanyNumber", personName).resolve}", classes = Seq("govuk-!-width-one-half")),
//        value = Value(Literal("reason-company-number")),
//        actions = Seq(Action(
//          content = Html(s"<span aria-hidden=true >${messages("site.change")}</span>"),
//          href = routes.NoCompanyNumberReasonController.onPageLoad(0, CheckMode).url,
//          visuallyHiddenText = Some(Literal(Messages("site.change") + " " +
//            Messages("messages__whyNoCompanyNumber__cya__visuallyHidden", personName))),
//          attributes = Map("id" -> "cya-0-1-change")
//        ))
//      )
//
//      result(2) mustBe Row(
//        key = Key(msg"${Message("messages__hasUTR", personName).resolve}", classes = Seq("govuk-!-width-one-half")),
//        value = Value(Literal("No")),
//        actions = Seq(Action(
//          content = Html(s"<span aria-hidden=true >${messages("site.change")}</span>"),
//          href = routes.HaveUTRController.onPageLoad(0, CheckMode).url,
//          visuallyHiddenText = Some(Literal(Messages("site.change") + " " +
//            Messages("messages__hasUTR__cya__visuallyHidden", personName))),
//          attributes = Map("id" -> "cya-0-2-change")
//        ))
//      )
//
//      result(3) mustBe Row(
//        key = Key(msg"${Message("messages__whyNoUTR", personName).resolve}", classes = Seq("govuk-!-width-one-half")),
//        value = Value(Literal("reason-utr")),
//        actions = Seq(Action(
//          content = Html(s"<span aria-hidden=true >${messages("site.change")}</span>"),
//          href = routes.NoUTRReasonController.onPageLoad(0, CheckMode).url,
//          visuallyHiddenText = Some(Literal(Messages("site.change") + " " +
//            Messages("messages__whyNoUTR__cya__visuallyHidden", personName))),
//          attributes = Map("id" -> "cya-0-3-change")
//        ))
//      )
//
//      result(4) mustBe Row(
//        key = Key(msg"${Message("messages__haveVAT", personName).resolve}", classes = Seq("govuk-!-width-one-half")),
//        value = Value(Literal("No")),
//        actions = Seq(Action(
//          content = Html(s"<span aria-hidden=true >${messages("site.change")}</span>"),
//          href = routes.HaveVATController.onPageLoad(0, CheckMode).url,
//          visuallyHiddenText = Some(Literal(Messages("site.change") + " " +
//            Messages("messages__haveVAT__cya__visuallyHidden", personName))),
//          attributes = Map("id" -> "cya-0-4-change")
//        ))
//      )
//
//      result(5) mustBe Row(
//        key = Key(msg"${Message("messages__havePAYE", personName).resolve}", classes = Seq("govuk-!-width-one-half")),
//        value = Value(Literal("No")),
//        actions = Seq(Action(
//          content = Html(s"<span aria-hidden=true >${messages("site.change")}</span>"),
//          href = routes.HavePAYEController.onPageLoad(0, CheckMode).url,
//          visuallyHiddenText = Some(Literal(Messages("site.change") + " " +
//            Messages("messages__havePAYE__cya__visuallyHidden", personName))),
//          attributes = Map("id" -> "cya-0-5-change")
//        ))
//      )
//    }
  }
}
