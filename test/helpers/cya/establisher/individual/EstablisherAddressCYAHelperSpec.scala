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

package helpers.cya.establisher.individual

import helpers.CYAHelperSpecBase
import helpers.cya.establishers.individual.EstablisherAddressCYAHelper
import identifiers.beforeYouStart.SchemeNameId
import identifiers.establishers.individual.EstablisherNameId
import identifiers.establishers.individual.address.{AddressId, AddressYearsId, PreviousAddressId}
import models.{Address, CheckMode, PersonName}
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.Aliases.Text
import utils.Data.schemeName
import utils.UserAnswers

class EstablisherAddressCYAHelperSpec extends CYAHelperSpecBase {

  val establisherAddressCYAHelper = new EstablisherAddressCYAHelper

  private val establisherName = PersonName("test", "establisher")
  private val establisherAddress = Address("addr1", "addr2", None, None, Some("ZZ11ZZ"), "GB")
  private val establisherPreviousAddress = Address("prevaddr1", "prevaddr2", None, None, Some("ZZ11ZZ"), "GB")

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

      result.head mustBe summaryListRowHtml(key = messages("messages__address__whatYouWillNeed_h1", establisherName.fullName),
        value = answerAddressTransform(establisherAddress), Some(Link(text = Messages("site.change"),
          target = controllers.establishers.individual.address.routes.EnterPostcodeController.onPageLoad(0, CheckMode).url,
          visuallyHiddenText = Some(Text(Messages("site.change") + " " + Messages("messages__visuallyHidden__address", establisherName.fullName))),
          attributes = Map("id" -> "cya-0-0-change"))))

      result(1) mustBe summaryListRow(key = Messages("addressYears.title", establisherName.fullName), valueMsgKey = "booleanAnswer.false",
        Some(Link(text = Messages("site.change"),
          target = controllers.establishers.individual.address.routes.AddressYearsController.onPageLoad(0, CheckMode).url,
          visuallyHiddenText = Some(Text(Messages("site.change") + " " +
            Messages("messages__visuallyhidden__addressYears", establisherName.fullName))),
          attributes = Map("id" -> "cya-0-1-change"))))

      result(2) mustBe summaryListRowHtml(key = messages("messages__previousAddress", establisherName.fullName),
        value = answerAddressTransform(establisherPreviousAddress), Some(Link(text = Messages("site.change"),
          target = controllers.establishers.individual.address.routes.EnterPreviousPostcodeController.onPageLoad(0, CheckMode).url,
          visuallyHiddenText = Some(Text(Messages("site.change") + " " + Messages("messages__visuallyHidden__previousAddress", establisherName.fullName))),
          attributes = Map("id" -> "cya-0-2-change"))))
    }
  }
}
