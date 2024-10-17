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

package helpers.cya.trustees.individual

import helpers.CYAHelperSpecBase
import identifiers.beforeYouStart.SchemeNameId
import identifiers.trustees.individual.TrusteeNameId
import identifiers.trustees.individual.address.{AddressId, AddressYearsId, PreviousAddressId}
import models.{Address, CheckMode, PersonName}
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.Aliases.Text
import utils.Data.schemeName
import utils.UserAnswers

class TrusteeAddressCYAHelperSpec extends CYAHelperSpecBase {

  val trusteeAddressCYAHelper = new TrusteeAddressCYAHelper

  private val trusteeName = PersonName("test", "trustee")
  private val trusteeAddress = Address("addr1", "addr2", None, None, Some("ZZ11ZZ"), "GB")
  private val trusteePreviousAddress = Address("prevaddr1", "prevaddr2", None, None, Some("ZZ11ZZ"), "GB")
  

  // scalastyle:off magic.number
  "TrusteeAddressCYAHelper" must {
    "return all rows with correct change link, value and visually hidden text" in {
      val ua: UserAnswers = UserAnswers()
        .setOrException(SchemeNameId, schemeName)
        .setOrException(TrusteeNameId(0), trusteeName)
        .setOrException(AddressId(0), trusteeAddress)
        .setOrException(AddressYearsId(0), false)
        .setOrException(PreviousAddressId(0), trusteePreviousAddress)

      val result = trusteeAddressCYAHelper.rows(0)(dataRequest(ua), messages)

      result.head mustBe summaryListRowHtml(key = Messages("messages__address__whatYouWillNeed_h1", trusteeName.fullName),
        value = answerAddressTransform(trusteeAddress), Some(Link(text = Messages("site.change"),
          target = controllers.trustees.individual.address.routes.EnterPostcodeController.onPageLoad(0, CheckMode).url,
          visuallyHiddenText = Some(Text(Messages("site.change") + " " + Messages("messages__visuallyHidden__address", trusteeName.fullName))),
          attributes = Map("id" -> "cya-0-0-change"))))

      result(1) mustBe summaryListRow(key = Messages("addressYears.title", trusteeName.fullName), valueMsgKey = "booleanAnswer.false",
        Some(Link(text = Messages("site.change"),
          target = controllers.trustees.individual.address.routes.AddressYearsController.onPageLoad(0, CheckMode).url,
          visuallyHiddenText = Some(Text(Messages("site.change") + " " +
            Messages("messages__visuallyhidden__addressYears", trusteeName.fullName))),
          attributes = Map("id" -> "cya-0-1-change"))))

      result(2) mustBe summaryListRowHtml(key = Messages("messages__previousAddress", trusteeName.fullName),
        value = answerAddressTransform(trusteePreviousAddress), Some(Link(text = Messages("site.change"),
          target = controllers.trustees.individual.address.routes.EnterPreviousPostcodeController.onPageLoad(0, CheckMode).url,
          visuallyHiddenText = Some(Text(Messages("site.change") + " " + Messages("messages__visuallyHidden__previousAddress", trusteeName.fullName))),
          attributes = Map("id" -> "cya-0-2-change"))))
    }
  }
}
