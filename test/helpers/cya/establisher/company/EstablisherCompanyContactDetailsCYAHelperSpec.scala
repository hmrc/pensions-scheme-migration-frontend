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

import controllers.establishers.company.contact.routes
import helpers.CYAHelperSpecBase
import helpers.cya.establishers.company.EstablisherCompanyContactDetailsCYAHelper
import identifiers.beforeYouStart.SchemeNameId
import identifiers.establishers.company.CompanyDetailsId
import identifiers.establishers.company.contact.{EnterEmailId, EnterPhoneId}
import models.{CheckMode, CompanyDetails}
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.{HtmlContent, Text}
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist._
import utils.Data.schemeName
import utils.UserAnswers

class EstablisherCompanyContactDetailsCYAHelperSpec extends CYAHelperSpecBase {

  val establisherCompanyContactDetailsCYAHelper = new EstablisherCompanyContactDetailsCYAHelper

  private val company: CompanyDetails = CompanyDetails("test")

  // scalastyle:off magic.number
  "EstablisherCompanyContactDetailsCYAHelper" must {
    "return all rows with correct change link, value and visually hidden text" in {
      val ua: UserAnswers = UserAnswers()
        .setOrException(SchemeNameId, schemeName)
        .setOrException(CompanyDetailsId(0), company)
        .setOrException(EnterEmailId(0), "test@test.com")
        .setOrException(EnterPhoneId(0), "123")

      val result = establisherCompanyContactDetailsCYAHelper.contactDetailsRows(0)(dataRequest(ua), messages)

      result.head mustBe SummaryListRow(
        key = Key(Text(Messages("messages__enterEmail_cya_label", company.companyName)), classes = "govuk-!-width-one-half"),
        value = Value(Text("test@test.com")),
        actions = Some(Actions(items = Seq(ActionItem(
          content = HtmlContent(s"<span aria-hidden=true >${messages("site.change")}</span>"),
          href = routes.EnterEmailController.onPageLoad(0, CheckMode).url,
          visuallyHiddenText = Some(Messages("site.change") + " " +
            Messages("messages__enterEmail__cya__visuallyHidden", company.companyName)),
          attributes = Map("id" -> "cya-0-0-change")
        ))))
      )

      result(1) mustBe SummaryListRow(
        key = Key(Text(Messages("messages__enterPhone_cya_label", company.companyName)), classes = "govuk-!-width-one-half"),
        value = Value(Text("123")),
        actions = Some(Actions( items = Seq(ActionItem(
          content = HtmlContent(s"<span aria-hidden=true >${messages("site.change")}</span>"),
          href = routes.EnterPhoneController.onPageLoad(0, CheckMode).url,
          visuallyHiddenText = Some(Messages("site.change") + " " +
            Messages("messages__enterPhone__cya__visuallyHidden", company.companyName)),
          attributes = Map("id" -> "cya-0-1-change")
        ))))
      )

    }
  }
}
