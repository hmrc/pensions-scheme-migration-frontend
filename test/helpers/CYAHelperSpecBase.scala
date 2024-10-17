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

package helpers

import base.SpecBase
import models.requests.DataRequest
import models.{Address, MigrationLock}
import org.scalatest.TryValues
import org.scalatest.matchers.must.Matchers
import play.api.i18n.Messages
import play.api.mvc.AnyContent
import uk.gov.hmrc.domain.PsaId
import uk.gov.hmrc.govukfrontend.views.Aliases.{HtmlContent, Key, Text}
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.{ActionItem, Actions, SummaryListRow, Value}
import utils.Data.{credId, psaId, pstr}
import utils.{Enumerable, UserAnswers}

class CYAHelperSpecBase extends SpecBase with Matchers with TryValues with Enumerable.Implicits {
  case class Link(text: String, target: String, visuallyHiddenText: Option[Text] = None,
                  attributes: Map[String, String] = Map.empty)

  def dataRequest(ua: UserAnswers) = DataRequest[AnyContent](request = fakeRequest, userAnswers = ua,
    psaId = PsaId(psaId), lock = MigrationLock(pstr = pstr, credId = credId, psaId = psaId), viewOnly = false)
  private def actions(target: Option[Link]) = {
    target.map { t =>
      Actions(
        items =
          Seq(ActionItem(
            content = HtmlContent(s"<span aria-hidden=true >${t.text}</span>"), href = t.target,
            visuallyHiddenText = t.visuallyHiddenText.map(_.value), attributes = t.attributes
          ))
      )
    }
  }

  def summaryListRow(key: String, valueMsgKey: String, target: Option[Link] = None): SummaryListRow = {
    SummaryListRow(key = Key(Text(Messages(key)), classes ="govuk-!-width-one-half"), value = Value(Text(Messages(valueMsgKey))),
      actions = actions(target)
    )
  }

  def summaryListRowText(key: String, value: String, target: Option[Link]): SummaryListRow = {
    SummaryListRow(key = Key(Text(Messages(key)), classes ="govuk-!-width-one-half"), value = Value(Text(value)),
      actions = actions(target)
    )
  }

  def summaryListRowHtml(key: String, value: HtmlContent, target: Option[Link]): SummaryListRow = {
    SummaryListRow(key = Key(Text(Messages(key)), classes ="govuk-!-width-one-half"), value = Value(value),
      actions = actions(target)
    )
  }

  private def addressAnswer(addr: Address)(implicit messages: Messages): HtmlContent = {
    def addrLineToHtml(l: String): String = s"""<span class="govuk-!-display-block">$l</span>"""

    HtmlContent(addrLineToHtml(addr.addressLine1) + addrLineToHtml(addr.addressLine2) + addr.addressLine3
      .fold("")(addrLineToHtml) + addr.addressLine4.fold("")(addrLineToHtml) + addr.postcode
      .fold("")(addrLineToHtml) + addrLineToHtml(messages("country." + addr.country)))
  }

  def answerAddressTransform(addr: Address)(implicit messages: Messages): HtmlContent = addressAnswer(addr)

}
