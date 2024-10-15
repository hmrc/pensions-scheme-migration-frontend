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

import helpers.CYAHelperSpecBase
import helpers.cya.establishers.company.EstablisherCompanyAddressCYAHelper
import identifiers.beforeYouStart.SchemeNameId
import identifiers.establishers.company.CompanyDetailsId
import identifiers.establishers.company.address.{AddressId, AddressYearsId, PreviousAddressId, TradingTimeId}
import models.{Address, CheckMode, CompanyDetails}
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.Text
import utils.Data.schemeName
import utils.UserAnswers
import viewmodels.Message

class EstablisherCompanyAddressCYAHelperSpec extends CYAHelperSpecBase {

  val establisherCompanyAddressCYAHelper = new EstablisherCompanyAddressCYAHelper

  private val establisherCompanyName = CompanyDetails("test company")
  private val establisherAddress = Address("addr1", "addr2", None, None, Some("ZZ11ZZ"), "GB")
  private val establisherPreviousAddress = Address("prevaddr1", "prevaddr2", None, None, Some("ZZ11ZZ"), "GB")
  

  // scalastyle:off magic.number
  "EstablisherCompanyAddressCYAHelper" must {
    "return all rows with correct change link, value and visually hidden text" in {
      val ua: UserAnswers = UserAnswers()
        .setOrException(SchemeNameId, schemeName)
        .setOrException(CompanyDetailsId(0), establisherCompanyName)
        .setOrException(AddressId(0), establisherAddress)
        .setOrException(AddressYearsId(0), false)
        .setOrException(TradingTimeId(0), true)
        .setOrException(PreviousAddressId(0), establisherPreviousAddress)

      val result = establisherCompanyAddressCYAHelper.rows(0)(dataRequest(ua), messages)

      result.head mustBe summaryListRowHtml(key = messages("messages__address__whatYouWillNeed_h1", establisherCompanyName.companyName),
        value = answerAddressTransform(establisherAddress), Some(Link(text = Messages("site.change"),
          target = controllers.establishers.company.address.routes.EnterPostcodeController.onPageLoad(0, CheckMode).url,
          visuallyHiddenText = Some(Text(Messages("site.change") + " " + Messages("messages__visuallyHidden__address", establisherCompanyName.companyName))),
          attributes = Map("id" -> "cya-0-0-change"))))

      result(1) mustBe summaryListRow(key = Messages("addressYears.title", establisherCompanyName.companyName), valueMsgKey = "booleanAnswer.false",
        Some(Link(text = Messages("site.change"),
          target = controllers.establishers.company.address.routes.AddressYearsController.onPageLoad(0, CheckMode).url,
          visuallyHiddenText = Some(Text(Messages("site.change") + " " +
            Messages("messages__visuallyhidden__addressYears", establisherCompanyName.companyName))),
          attributes = Map("id" -> "cya-0-1-change"))))

      result(2) mustBe summaryListRow(key = Messages("tradingTime.title", establisherCompanyName.companyName), valueMsgKey = "booleanAnswer.true",
        Some(Link(text = Messages("site.change"),
          target = controllers.establishers.company.address.routes.TradingTimeController.onPageLoad(0, CheckMode).url,
          visuallyHiddenText = Some(Text(Messages("site.change") + " " +
            Messages("messages__visuallyhidden__TradingTime", establisherCompanyName.companyName))),
          attributes = Map("id" -> "cya-0-2-change"))))

      result(3) mustBe summaryListRowHtml(key = Message("messages__previousAddress", establisherCompanyName.companyName).resolve,
        value = answerAddressTransform(establisherPreviousAddress), Some(Link(text = Messages("site.change"),
          target = controllers.establishers.company.address.routes.EnterPreviousPostcodeController.onPageLoad(0, CheckMode).url,
          visuallyHiddenText = Some(Text(Messages("site.change") + " " + Messages("messages__visuallyHidden__previousAddress",
            establisherCompanyName.companyName))),
          attributes = Map("id" -> "cya-0-3-change"))))
    }
  }
}


