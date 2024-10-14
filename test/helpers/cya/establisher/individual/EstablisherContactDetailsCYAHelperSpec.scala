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

import base.SpecBase._
import helpers.cya.establishers.individual.EstablisherContactDetailsCYAHelper
import identifiers.beforeYouStart.SchemeNameId
import identifiers.establishers.individual.EstablisherNameId
import identifiers.establishers.individual.contact.{EnterEmailId, EnterPhoneId}
import models.requests.DataRequest
import models.{CheckMode, MigrationLock, PersonName}
import org.scalatest.TryValues
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.i18n.Messages
import play.api.mvc.AnyContent
import uk.gov.hmrc.domain.PsaId
import uk.gov.hmrc.govukfrontend.views.Aliases.{Actions, Text, Value}
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.HtmlContent
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.{ActionItem, Key, SummaryListRow}
import uk.gov.hmrc.viewmodels.MessageInterpolators
import utils.Data.{credId, psaId, pstr, schemeName}
import utils.{Enumerable, UserAnswers}
import viewmodels.Message

class EstablisherContactDetailsCYAHelperSpec extends AnyWordSpec with Matchers with TryValues with Enumerable.Implicits {

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

      result.head mustBe SummaryListRow(
        key = Key(Text(Messages("messages__enterEmail_cya_label", establisherName.fullName)), classes = "govuk-!-width-one-half"),
        value = Value(Text("test@test.com")),
        actions = Some(Actions(
          items = Seq(ActionItem(
            content = HtmlContent(s"<span aria-hidden=true >${messages("site.change")}</span>"),
            href = controllers.establishers.individual.contact.routes.EnterEmailController.onPageLoad(0, CheckMode).url,
            visuallyHiddenText = Some(
              Messages("site.change") + " " +
              Messages("messages__enterEmail__cya__visuallyHidden", establisherName.fullName)
            ),
            attributes = Map("id" -> "cya-0-0-change")
          ))
        ))
      )

      result(1) mustBe SummaryListRow(
        key = Key(Text(Messages("messages__enterPhone_cya_label", establisherName.fullName)), classes = "govuk-!-width-one-half"),
        value = Value(Text("123")),
        actions = Some(Actions(items = Seq(ActionItem(
          content = HtmlContent(s"<span aria-hidden=true >${messages("site.change")}</span>"),
          href = controllers.establishers.individual.contact.routes.EnterPhoneController.onPageLoad(0, CheckMode).url,
          visuallyHiddenText = Some(Messages("site.change") + " " +
            Messages("messages__enterPhone__cya__visuallyHidden", establisherName.fullName)
          ),
          attributes = Map("id" -> "cya-0-1-change")
        ))))
      )

    }
  }
}
