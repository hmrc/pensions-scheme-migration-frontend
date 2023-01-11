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

package helpers.cya.trustees.company

import base.SpecBase._
import identifiers.beforeYouStart.SchemeNameId
import identifiers.trustees.company.CompanyDetailsId
import identifiers.trustees.company.address.{AddressId, AddressYearsId, PreviousAddressId, TradingTimeId}
import models.requests.DataRequest
import models.{Address, CheckMode, CompanyDetails, MigrationLock}
import org.scalatest.TryValues
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.i18n.Messages
import play.api.mvc.AnyContent
import uk.gov.hmrc.domain.PsaId
import uk.gov.hmrc.viewmodels.SummaryList._
import uk.gov.hmrc.viewmodels.Text.{Literal, Message => GovUKMsg}
import uk.gov.hmrc.viewmodels.{Html, SummaryList, Text}
import utils.Data.{credId, psaId, pstr, schemeName}
import utils.{Enumerable, UserAnswers}

class TrusteeCompanyAddressCYAHelperSpec extends AnyWordSpec with Matchers with TryValues with Enumerable.Implicits {

  val trusteeCompanyAddressCYAHelper = new TrusteeAddressCYAHelper

  private def dataRequest(ua: UserAnswers) = DataRequest[AnyContent](request = fakeRequest, userAnswers = ua,
    psaId = PsaId(psaId), lock = MigrationLock(pstr = pstr, credId = credId, psaId = psaId), viewOnly = false)

  private val trusteeCompanyName = CompanyDetails("test company")
  private val trusteeAddress = Address("addr1", "addr2", None, None, Some("ZZ11ZZ"), "GB")
  private val trusteePreviousAddress = Address("prevaddr1", "prevaddr2", None, None, Some("ZZ11ZZ"), "GB")

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

  private def addressAnswer(addr: Address)(implicit messages: Messages): Html = {
    def addrLineToHtml(l: String): String = s"""<span class="govuk-!-display-block">$l</span>"""

    Html(addrLineToHtml(addr.addressLine1) + addrLineToHtml(addr.addressLine2) + addr.addressLine3
      .fold("")(addrLineToHtml) + addr.addressLine4.fold("")(addrLineToHtml) + addr.postcode
      .fold("")(addrLineToHtml) + addrLineToHtml(messages("country." + addr.country)))
  }

  private def answerAddressTransform(addr: Address)(implicit messages: Messages): Html = addressAnswer(addr)

  // scalastyle:off magic.number
  "TrusteeompanyAddressCYAHelper" must {
    "return all rows with correct change link, value and visually hidden text" in {
      val ua: UserAnswers = UserAnswers()
        .setOrException(SchemeNameId, schemeName)
        .setOrException(CompanyDetailsId(0), trusteeCompanyName)
        .setOrException(AddressId(0), trusteeAddress)
        .setOrException(AddressYearsId(0), false)
        .setOrException(TradingTimeId(0), true)
        .setOrException(PreviousAddressId(0), trusteePreviousAddress)

      val result = trusteeCompanyAddressCYAHelper.rows(0)(dataRequest(ua), messages)

      result.head mustBe summaryListRowHtml(key = messages("messages__address__whatYouWillNeed_h1", trusteeCompanyName.companyName),
        value = answerAddressTransform(trusteeAddress), Some(Link(text = Messages("site.change"),
          target = controllers.trustees.company.address.routes.EnterPostcodeController.onPageLoad(0,CheckMode).url,
          visuallyHiddenText = Some(Literal(Messages("site.change") + " " + Messages("messages__visuallyHidden__address", trusteeCompanyName.companyName))),
          attributes = Map("id" -> "cya-0-0-change"))))

      result(1) mustBe summaryListRow(key = Messages("addressYears.title", trusteeCompanyName.companyName), valueMsgKey = "booleanAnswer.false",
        Some(Link(text = Messages("site.change"),
          target = controllers.trustees.company.address.routes.AddressYearsController.onPageLoad(0,CheckMode).url,
          visuallyHiddenText = Some(Literal(Messages("site.change") + " " +
            Messages("messages__visuallyhidden__addressYears", trusteeCompanyName.companyName))),
          attributes = Map("id" -> "cya-0-1-change"))))

      result(2) mustBe summaryListRow(key = Messages("tradingTime.title", trusteeCompanyName.companyName), valueMsgKey = "booleanAnswer.true",
        Some(Link(text = Messages("site.change"),
          target = controllers.trustees.company.address.routes.TradingTimeController.onPageLoad(0,CheckMode).url,
          visuallyHiddenText = Some(Literal(Messages("site.change") + " " +
            Messages("messages__visuallyhidden__TradingTime", trusteeCompanyName.companyName))),
          attributes = Map("id" -> "cya-0-2-change"))))

      result(3) mustBe summaryListRowHtml(key = messages("messages__previousAddress", trusteeCompanyName.companyName),
        value = answerAddressTransform(trusteePreviousAddress), Some(Link(text = Messages("site.change"),
          target = controllers.trustees.company.address.routes.EnterPreviousPostcodeController.onPageLoad(0,CheckMode).url,
          visuallyHiddenText = Some(Literal(Messages("site.change") + " " + Messages("messages__visuallyHidden__previousAddress",
            trusteeCompanyName.companyName))),
          attributes = Map("id" -> "cya-0-3-change"))))
    }
  }
}


