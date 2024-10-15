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

package helpers.cya.trustees.partnership

import helpers.CYAHelperSpecBase
import identifiers.beforeYouStart.SchemeNameId
import identifiers.trustees.partnership.PartnershipDetailsId
import identifiers.trustees.partnership.address.{AddressId, AddressYearsId, PreviousAddressId, TradingTimeId}
import models.{Address, CheckMode, PartnershipDetails}
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.Text
import utils.Data.schemeName
import utils.UserAnswers

class TrusteePartnershipAddressCYAHelperSpec extends CYAHelperSpecBase {

  val trusteePartnershipAddressCYAHelper = new TrusteeAddressCYAHelper
  

  private val trusteePartnershipName = PartnershipDetails("test partnership")
  private val trusteeAddress = Address("addr1", "addr2", None, None, Some("ZZ11ZZ"), "GB")
  private val trusteePreviousAddress = Address("prevaddr1", "prevaddr2", None, None, Some("ZZ11ZZ"), "GB")
  
  // scalastyle:off magic.number
  "TrusteeAddressCYAHelper" must {
    "return all rows with correct change link, value and visually hidden text" in {
      val ua: UserAnswers = UserAnswers()
        .setOrException(SchemeNameId, schemeName)
        .setOrException(PartnershipDetailsId(0), trusteePartnershipName)
        .setOrException(AddressId(0), trusteeAddress)
        .setOrException(AddressYearsId(0), false)
        .setOrException(TradingTimeId(0), true)
        .setOrException(PreviousAddressId(0), trusteePreviousAddress)

      val result = trusteePartnershipAddressCYAHelper.rows(0)(dataRequest(ua), messages)

      result.head mustBe summaryListRowHtml(key = messages("messages__address__whatYouWillNeed_h1", trusteePartnershipName.partnershipName),
        value = answerAddressTransform(trusteeAddress), Some(Link(text = Messages("site.change"),
          target = controllers.trustees.partnership.address.routes.EnterPostcodeController.onPageLoad(0,CheckMode).url,
          visuallyHiddenText = Some(Text(Messages("site.change") + " " + Messages("messages__visuallyHidden__address",
            trusteePartnershipName.partnershipName))),
          attributes = Map("id" -> "cya-0-0-change"))))

      result(1) mustBe summaryListRow(key = Messages("addressYears.title", trusteePartnershipName.partnershipName), valueMsgKey = "booleanAnswer.false",
        Some(Link(text = Messages("site.change"),
          target = controllers.trustees.partnership.address.routes.AddressYearsController.onPageLoad(0,CheckMode).url,
          visuallyHiddenText = Some(Text(Messages("site.change") + " " +
            Messages("messages__visuallyhidden__addressYears", trusteePartnershipName.partnershipName))),
          attributes = Map("id" -> "cya-0-1-change"))))

      result(2) mustBe summaryListRow(key = Messages("tradingTime.title", trusteePartnershipName.partnershipName), valueMsgKey = "booleanAnswer.true",
        Some(Link(text = Messages("site.change"),
          target = controllers.trustees.partnership.address.routes.TradingTimeController.onPageLoad(0,CheckMode).url,
          visuallyHiddenText = Some(Text(Messages("site.change") + " " +
            Messages("messages__visuallyhidden__TradingTime", trusteePartnershipName.partnershipName))),
          attributes = Map("id" -> "cya-0-2-change"))))

      result(3) mustBe summaryListRowHtml(key = messages("messages__previousAddress", trusteePartnershipName.partnershipName),
        value = answerAddressTransform(trusteePreviousAddress), Some(Link(text = Messages("site.change"),
          target = controllers.trustees.partnership.address.routes.EnterPreviousPostcodeController.onPageLoad(0,CheckMode).url,
          visuallyHiddenText = Some(Text(Messages("site.change") + " " + Messages("messages__visuallyHidden__previousAddress",
            trusteePartnershipName.partnershipName))),
          attributes = Map("id" -> "cya-0-3-change"))))
    }
  }
}
