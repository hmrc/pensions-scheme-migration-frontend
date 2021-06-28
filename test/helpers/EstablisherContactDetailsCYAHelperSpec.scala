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
import controllers.establishers.individual.contact.routes
import identifiers.beforeYouStart.SchemeNameId
import identifiers.establishers.individual.EstablisherNameId
import identifiers.establishers.individual.address.{AddressId, AddressYearsId, PreviousAddressId}
import identifiers.establishers.individual.contact.{EnterEmailId, EnterPhoneId}
import models.requests.DataRequest
import models.{Address, CheckMode, MigrationLock, PersonName}
import org.scalatest.{MustMatchers, TryValues, WordSpec}
import play.api.i18n.Messages
import play.api.mvc.AnyContent
import uk.gov.hmrc.domain.PsaId
import uk.gov.hmrc.viewmodels.SummaryList._
import uk.gov.hmrc.viewmodels.Text.{Literal, Message => GovUKMsg}
import uk.gov.hmrc.viewmodels.{Html, SummaryList, Text}
import utils.Data.{credId, psaId, pstr, schemeName}
import utils.{Enumerable, UserAnswers}
import identifiers.TypedIdentifier
import models.benefitsAndInsurance.{BenefitsProvisionType, BenefitsType}
import models.{Address, Link, PersonName, ReferenceValue}
import play.api.i18n.Messages
import play.api.libs.json.Reads
import uk.gov.hmrc.viewmodels.SummaryList.{Action, Key, Row, Value}
import uk.gov.hmrc.viewmodels.Text.Literal
import uk.gov.hmrc.viewmodels.{Content, Html, MessageInterpolators, SummaryList, Text}
import utils.UserAnswers
import viewmodels.{AnswerRow, Message}
import viewmodels.Message

class EstablisherContactDetailsCYAHelperSpec extends WordSpec with MustMatchers with TryValues with Enumerable.Implicits {

  val establisherContactDetailsCYAHelper = new EstablisherContactDetailsCYAHelper

  private def dataRequest(ua: UserAnswers) = DataRequest[AnyContent](request = fakeRequest, userAnswers = ua,
    psaId = PsaId(psaId), lock = MigrationLock(pstr = pstr, credId = credId, psaId = psaId), viewOnly = false)

  private val establisherName = PersonName("test", "establisher")

  // scalastyle:off magic.number
  "EstablisherContactDetailsCYAHelper" must {
    "return all rows with correct change link, value and visually hidden text" in {
      val ua: UserAnswers = UserAnswers()
        .setOrException(SchemeNameId, schemeName)
        .setOrException(EstablisherNameId(0), establisherName)
        .setOrException(EnterEmailId(0), "test@test.com")
        .setOrException(EnterPhoneId(0), "123")

      val result = establisherContactDetailsCYAHelper.contactDetailsRows(0)(dataRequest(ua), messages)

      result.head mustBe Row(
        key = Key(msg"${Message("messages__enterEmail", establisherName.fullName).resolve}", classes = Seq("govuk-!-width-one-half")),
        value = Value(Literal("test@test.com")),
        actions = Seq(Action(
          content = Html(s"<span aria-hidden=true >${messages("site.change")}</span>"),
          href = routes.EnterEmailController.onPageLoad(0, CheckMode).url,
          visuallyHiddenText = Some(Literal(Messages("site.change") + " " +
            Messages("messages__enterEmail__cya__visuallyHidden", establisherName.fullName))),
          attributes = Map("id" -> "change")
        ))
      )

      result(1) mustBe Row(
        key = Key(msg"${Message("messages__enterPhone", establisherName.fullName).resolve}", classes = Seq("govuk-!-width-one-half")),
        value = Value(Literal("123")),
        actions = Seq(Action(
          content = Html(s"<span aria-hidden=true >${messages("site.change")}</span>"),
          href = routes.EnterPhoneController.onPageLoad(0, CheckMode).url,
          visuallyHiddenText = Some(Literal(Messages("site.change") + " " +
            Messages("messages__enterPhone__cya__visuallyHidden", establisherName.fullName))),
          attributes = Map("id" -> "change")
        ))
      )

    }
  }
}
