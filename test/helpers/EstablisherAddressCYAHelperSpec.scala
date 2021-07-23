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

package helpers

import base.SpecBase._
import identifiers.beforeYouStart.SchemeNameId
import identifiers.establishers.individual.EstablisherNameId
import identifiers.establishers.individual.address.{AddressId, AddressYearsId, PreviousAddressId}
import models.requests.DataRequest
import models.{Address, MigrationLock, PersonName}
import org.scalatest.{MustMatchers, TryValues, WordSpec}
import play.api.i18n.Messages
import play.api.mvc.AnyContent
import uk.gov.hmrc.domain.PsaId
import uk.gov.hmrc.viewmodels.SummaryList._
import uk.gov.hmrc.viewmodels.Text.{Literal, Message => GovUKMsg}
import uk.gov.hmrc.viewmodels.{Html, SummaryList, Text}
import utils.Data.{credId, psaId, pstr, schemeName}
import utils.{Enumerable, UserAnswers}

class EstablisherAddressCYAHelperSpec extends WordSpec with MustMatchers with TryValues with Enumerable.Implicits {

  val establisherAddressCYAHelper = new EstablisherAddressCYAHelper

  private def dataRequest(ua: UserAnswers) = DataRequest[AnyContent](request = fakeRequest, userAnswers = ua,
    psaId = PsaId(psaId), lock = MigrationLock(pstr = pstr, credId = credId, psaId = psaId), viewOnly = false)

  private val establisherName = PersonName("test", "establisher")
  private val establisherAddress = Address("addr1", "addr2", None, None, Some("ZZ11ZZ"), "GB")
  private val establisherPreviousAddress = Address("prevaddr1", "prevaddr2", None, None, Some("ZZ11ZZ"), "GB")

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
  "EstablisherAddressCYAHelper" must {
    "return all rows with correct change link, value and visually hidden text" in {
      val ua: UserAnswers = UserAnswers()
        .setOrException(SchemeNameId, schemeName)
        .setOrException(EstablisherNameId(0), establisherName)
        .setOrException(AddressId(0), establisherAddress)
        .setOrException(AddressYearsId(0), false)
        .setOrException(PreviousAddressId(0), establisherPreviousAddress)

      val result = establisherAddressCYAHelper.rows(0)(dataRequest(ua), messages)

      result.head mustBe summaryListRowHtml(key = messages("messages__establisherAddress__whatYouWillNeed_h1", establisherName.fullName),
        value = answerAddressTransform(establisherAddress), Some(Link(text = Messages("site.change"),
          target = controllers.establishers.individual.address.routes.EnterPostcodeController.onPageLoad(0).url,
          visuallyHiddenText = Some(Literal(Messages("site.change") + " " + Messages("messages__visuallyHidden__address", establisherName.fullName))),
          attributes = Map("id" -> "cya-0-0-change"))))

      result(1) mustBe summaryListRow(key = Messages("establisherAddressYears.title", establisherName.fullName), valueMsgKey = "booleanAnswer.false",
        Some(Link(text = Messages("site.change"),
          target = controllers.establishers.individual.address.routes.AddressYearsController.onPageLoad(0).url,
          visuallyHiddenText = Some(Literal(Messages("site.change") + " " +
            Messages("messages__visuallyhidden__establisherAddressYears", establisherName.fullName))),
          attributes = Map("id" -> "cya-0-1-change"))))

      result(2) mustBe summaryListRowHtml(key = messages("messages__establisherPreviousAddress"),
        value = answerAddressTransform(establisherPreviousAddress), Some(Link(text = Messages("site.change"),
          target = controllers.establishers.individual.address.routes.EnterPreviousPostcodeController.onPageLoad(0).url,
          visuallyHiddenText = Some(Literal(Messages("site.change") + " " + Messages("messages__visuallyHidden__previousAddress", establisherName.fullName))),
          attributes = Map("id" -> "cya-0-2-change"))))
    }
  }
}
