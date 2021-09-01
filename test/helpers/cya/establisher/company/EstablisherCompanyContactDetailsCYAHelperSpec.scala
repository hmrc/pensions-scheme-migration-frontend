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

package helpers.cya.establisher.company

import base.SpecBase._
import controllers.establishers.company.contact.routes
import helpers.cya.establishers.company.EstablisherCompanyContactDetailsCYAHelper
import identifiers.beforeYouStart.SchemeNameId
import identifiers.establishers.company.CompanyDetailsId
import identifiers.establishers.company.contact.{EnterEmailId, EnterPhoneId}
import models.requests.DataRequest
import models.{CheckMode, CompanyDetails, MigrationLock}
import org.scalatest.TryValues
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.i18n.Messages
import play.api.mvc.AnyContent
import uk.gov.hmrc.domain.PsaId
import uk.gov.hmrc.viewmodels.SummaryList.{Action, Key, Row, Value}
import uk.gov.hmrc.viewmodels.Text.Literal
import uk.gov.hmrc.viewmodels.{Html, MessageInterpolators}
import utils.Data.{credId, psaId, pstr, schemeName}
import utils.{Enumerable, UserAnswers}
import viewmodels.Message

class EstablisherCompanyContactDetailsCYAHelperSpec extends AnyWordSpec with Matchers with TryValues with Enumerable.Implicits {

  val establisherCompanyContactDetailsCYAHelper = new EstablisherCompanyContactDetailsCYAHelper

  private def dataRequest(ua: UserAnswers) = DataRequest[AnyContent](request = fakeRequest, userAnswers = ua,
    psaId = PsaId(psaId), lock = MigrationLock(pstr = pstr, credId = credId, psaId = psaId), viewOnly = false)

  private val company: CompanyDetails = CompanyDetails("test",false)

  // scalastyle:off magic.number
  "EstablisherCompanyContactDetailsCYAHelper" must {
    "return all rows with correct change link, value and visually hidden text" in {
      val ua: UserAnswers = UserAnswers()
        .setOrException(SchemeNameId, schemeName)
        .setOrException(CompanyDetailsId(0), company)
        .setOrException(EnterEmailId(0), "test@test.com")
        .setOrException(EnterPhoneId(0), "123")

      val result = establisherCompanyContactDetailsCYAHelper.contactDetailsRows(0)(dataRequest(ua), messages)

      result.head mustBe Row(
        key = Key(msg"${Message("messages__enterEmail", company.companyName).resolve}", classes = Seq("govuk-!-width-one-half")),
        value = Value(Literal("test@test.com")),
        actions = Seq(Action(
          content = Html(s"<span aria-hidden=true >${messages("site.change")}</span>"),
          href = routes.EnterEmailController.onPageLoad(0, CheckMode).url,
          visuallyHiddenText = Some(Literal(Messages("site.change") + " " +
            Messages("messages__enterEmail__cya__visuallyHidden", company.companyName))),
          attributes = Map("id" -> "cya-0-0-change")
        ))
      )

      result(1) mustBe Row(
        key = Key(msg"${Message("messages__enterPhone", company.companyName).resolve}", classes = Seq("govuk-!-width-one-half")),
        value = Value(Literal("123")),
        actions = Seq(Action(
          content = Html(s"<span aria-hidden=true >${messages("site.change")}</span>"),
          href = routes.EnterPhoneController.onPageLoad(0, CheckMode).url,
          visuallyHiddenText = Some(Literal(Messages("site.change") + " " +
            Messages("messages__enterPhone__cya__visuallyHidden", company.companyName))),
          attributes = Map("id" -> "cya-0-1-change")
        ))
      )

    }
  }
}
