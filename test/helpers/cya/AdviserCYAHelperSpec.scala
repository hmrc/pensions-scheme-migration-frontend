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

package helpers.cya

import controllers.adviser.routes
import helpers.CYAHelperSpecBase
import identifiers.adviser._
import models._
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.{HtmlContent, Text}
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist._
import utils.{Data, UserAnswers}

class AdviserCYAHelperSpec extends CYAHelperSpecBase {

  val cyaHelper = new AdviserCYAHelper

  private val adviserName: String = "test"
  private val adviserAddress = Address("addr1", "addr2", None, None, Some("ZZ11ZZ"), "GB")

  // scalastyle:off magic.number
  "AdviserCYAHelper" must {
    "return all rows with correct change links, values and visually hidden text when user has answered yes for all questions" in {
      val ua: UserAnswers = UserAnswers()
        .setOrException(AdviserNameId, adviserName)
        .setOrException(EnterEmailId, Data.email)
        .setOrException(EnterPhoneId, Data.phone)
        .setOrException(AddressId, adviserAddress)

      val result = cyaHelper.detailsRows(dataRequest(ua), messages)

      result.head mustBe SummaryListRow(
        key = Key(Text(Messages("messages__adviser__name__cya")), classes = "govuk-!-width-one-half"),
        value = Value(Text(adviserName)),
        actions = Some(
         Actions(
           items = Seq(
             ActionItem(
               content = HtmlContent(s"<span aria-hidden=true >${messages("site.change")}</span>"),
               href = routes.AdviserNameController.onPageLoad(CheckMode).url,
               visuallyHiddenText = Some(Messages("site.change") + " " +
                 Messages("messages__adviser__name__cya__visuallyHidden", adviserName)),
               attributes = Map("id" -> "cya-0-0-change")
             )
           )
         )
        )
      )

      result(1) mustBe SummaryListRow(
        key = Key(Text(Messages("messages__enterEmail_cya_label", adviserName)), classes = "govuk-!-width-one-half"),
        value = Value(Text(Data.email)),
        actions = Some(
          Actions(
            items = Seq(
              ActionItem(
                content = HtmlContent(s"<span aria-hidden=true >${messages("site.change")}</span>"),
                href = routes.EnterEmailController.onPageLoad(CheckMode).url,
                visuallyHiddenText = Some(Messages("site.change") + " " +
                  Messages("messages__enterEmail__cya__visuallyHidden", adviserName)),
                attributes = Map("id" -> "cya-0-1-change")
              )
            )
          )
        )
      )

      result(2) mustBe SummaryListRow(
        key = Key(Text(Messages("messages__enterPhone_cya_label", adviserName)), classes = "govuk-!-width-one-half"),
        value = Value(Text(Data.phone)),
        actions = Some(
          Actions(
            items = Seq(
              ActionItem(
                content = HtmlContent(s"<span aria-hidden=true >${messages("site.change")}</span>"),
                href = routes.EnterPhoneController.onPageLoad(CheckMode).url,
                visuallyHiddenText = Some(Messages("site.change") + " " +
                  Messages("messages__enterPhone__cya__visuallyHidden", adviserName)),
                attributes = Map("id" -> "cya-0-2-change")
              )
            )
          )
        )
      )

      result(3) mustBe summaryListRowHtml(key = messages("addressList_cya_label", adviserName),
        value = answerAddressTransform(adviserAddress), Some(Link(text = Messages("site.change"),
          target = routes.EnterPostcodeController.onPageLoad(CheckMode).url,
          visuallyHiddenText = Some(Text(Messages("site.change") + " " +
            Messages("messages__visuallyHidden__address", adviserName))),
          attributes = Map("id" -> "cya-0-3-change")))
      )

    }
  }
}
