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

package helpers.cya

import base.SpecBase._
import controllers.adviser.routes
import identifiers.adviser._
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
import utils.Data.{credId, psaId, pstr}
import utils.{Data, Enumerable, UserAnswers}
import viewmodels.Message

class AdviserCYAHelperSpec extends AnyWordSpec with Matchers with TryValues with Enumerable.Implicits {

  val cyaHelper = new AdviserCYAHelper

  private val adviserName: String = "test"
  private val adviserAddress = Address("addr1", "addr2", None, None, Some("ZZ11ZZ"), "GB")

  case class Link(text: String, target: String, visuallyHiddenText: Option[Text] = None,
                  attributes: Map[String, String] = Map.empty)


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
  "AdviserCYAHelper" must {
    "return all rows with correct change links, values and visually hidden text when user has answered yes for all questions" in {
      val ua: UserAnswers = UserAnswers()
        .setOrException(AdviserNameId, adviserName)
        .setOrException(EnterEmailId, Data.email)
        .setOrException(EnterPhoneId, Data.phone)
        .setOrException(AddressId, adviserAddress)

      val result = cyaHelper.detailsRows(dataRequest(ua), messages)

      result.head mustBe Row(
        key = Key(msg"${Message("messages__adviser__name__cya").resolve}", classes = Seq("govuk-!-width-one-half")),
        value = Value(Literal(adviserName)),
        actions = Seq(Action(
          content = Html(s"<span aria-hidden=true >${messages("site.change")}</span>"),
          href = routes.AdviserNameController.onPageLoad(CheckMode).url,
          visuallyHiddenText = Some(Literal(Messages("site.change") + " " +
            Messages("messages__adviser__name__cya__visuallyHidden", adviserName))),
          attributes = Map("id" -> "cya-0-0-change")
        ))
      )

      result(1) mustBe Row(
        key = Key(msg"${Message("messages__enterEmail_cya_label", adviserName).resolve}", classes = Seq("govuk-!-width-one-half")),
        value = Value(Literal(Data.email)),
        actions = Seq(Action(
          content = Html(s"<span aria-hidden=true >${messages("site.change")}</span>"),
          href = routes.EnterEmailController.onPageLoad(CheckMode).url,
          visuallyHiddenText = Some(Literal(Messages("site.change") + " " +
            Messages("messages__enterEmail__cya__visuallyHidden", adviserName))),
          attributes = Map("id" -> "cya-0-1-change")
        ))
      )

      result(2) mustBe Row(
        key = Key(msg"${Message("messages__enterPhone_cya_label", adviserName).resolve}", classes = Seq("govuk-!-width-one-half")),
        value = Value(Literal(Data.phone)),
        actions = Seq(Action(
          content = Html(s"<span aria-hidden=true >${messages("site.change")}</span>"),
          href = routes.EnterPhoneController.onPageLoad(CheckMode).url,
          visuallyHiddenText = Some(Literal(Messages("site.change") + " " +
            Messages("messages__enterPhone__cya__visuallyHidden", adviserName))),
          attributes = Map("id" -> "cya-0-2-change")
        ))
      )

      result(3) mustBe summaryListRowHtml(key = messages("addressList_cya_label", adviserName),
        value = answerAddressTransform(adviserAddress), Some(Link(text = Messages("site.change"),
          target = routes.EnterPostcodeController.onPageLoad(CheckMode).url,
          visuallyHiddenText = Some(Literal(Messages("site.change") + " " +
            Messages("messages__visuallyHidden__address", adviserName))),
          attributes = Map("id" -> "cya-0-3-change")))
      )

    }
  }
}
