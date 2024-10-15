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

package helpers.cya.trustees.company

import helpers.CYAHelperSpecBase
import identifiers.beforeYouStart.SchemeNameId
import identifiers.trustees.company.CompanyDetailsId
import identifiers.trustees.company.address.{AddressId, AddressYearsId, PreviousAddressId, TradingTimeId}
import models.{Address, CheckMode, CompanyDetails}
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.Aliases.Text
import utils.Data.schemeName
import utils.UserAnswers

class TrusteeCompanyAddressCYAHelperSpec extends CYAHelperSpecBase {

  val trusteeCompanyAddressCYAHelper = new TrusteeAddressCYAHelper

  private val trusteeCompanyName = CompanyDetails("test company")
  private val trusteeAddress = Address("addr1", "addr2", None, None, Some("ZZ11ZZ"), "GB")
  private val trusteePreviousAddress = Address("prevaddr1", "prevaddr2", None, None, Some("ZZ11ZZ"), "GB")

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
          visuallyHiddenText = Some(Text(Messages("site.change") + " " + Messages("messages__visuallyHidden__address", trusteeCompanyName.companyName))),
          attributes = Map("id" -> "cya-0-0-change"))))

      result(1) mustBe summaryListRow(key = Messages("addressYears.title", trusteeCompanyName.companyName), valueMsgKey = "booleanAnswer.false",
        Some(Link(text = Messages("site.change"),
          target = controllers.trustees.company.address.routes.AddressYearsController.onPageLoad(0,CheckMode).url,
          visuallyHiddenText = Some(Text(Messages("site.change") + " " +
            Messages("messages__visuallyhidden__addressYears", trusteeCompanyName.companyName))),
          attributes = Map("id" -> "cya-0-1-change"))))

      result(2) mustBe summaryListRow(key = Messages("tradingTime.title", trusteeCompanyName.companyName), valueMsgKey = "booleanAnswer.true",
        Some(Link(text = Messages("site.change"),
          target = controllers.trustees.company.address.routes.TradingTimeController.onPageLoad(0,CheckMode).url,
          visuallyHiddenText = Some(Text(Messages("site.change") + " " +
            Messages("messages__visuallyhidden__TradingTime", trusteeCompanyName.companyName))),
          attributes = Map("id" -> "cya-0-2-change"))))

      result(3) mustBe summaryListRowHtml(key = messages("messages__previousAddress", trusteeCompanyName.companyName),
        value = answerAddressTransform(trusteePreviousAddress), Some(Link(text = Messages("site.change"),
          target = controllers.trustees.company.address.routes.EnterPreviousPostcodeController.onPageLoad(0,CheckMode).url,
          visuallyHiddenText = Some(Text(Messages("site.change") + " " + Messages("messages__visuallyHidden__previousAddress",
            trusteeCompanyName.companyName))),
          attributes = Map("id" -> "cya-0-3-change"))))
    }
  }
}


