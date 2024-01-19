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

package helpers

import base.SpecBase._
import helpers.cya.BenefitsAndInsuranceCYAHelper
import identifiers.beforeYouStart.{SchemeNameId, SchemeTypeId}
import identifiers.benefitsAndInsurance._
import models.benefitsAndInsurance.{BenefitsProvisionType, BenefitsType}
import models.requests.DataRequest
import models.{Address, MigrationLock, SchemeType}
import org.scalatest.TryValues
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.i18n.Messages
import play.api.mvc.AnyContent
import uk.gov.hmrc.domain.PsaId
import uk.gov.hmrc.viewmodels.SummaryList._
import uk.gov.hmrc.viewmodels.Text.{Literal, Message => GovUKMsg}
import uk.gov.hmrc.viewmodels.{Html, SummaryList, Text}
import utils.Data.{credId, psaId, pstr, schemeName}
import utils.{Enumerable, UserAnswers}

class BenefitsAndInsuranceCYAHelperSpec extends AnyWordSpec with Matchers with TryValues with Enumerable.Implicits {

  val benefitsAndInsuranceCYAHelper = new BenefitsAndInsuranceCYAHelper

  private def dataRequest(ua: UserAnswers) = DataRequest[AnyContent](request = fakeRequest, userAnswers = ua,
    psaId = PsaId(psaId), lock = MigrationLock(pstr = pstr, credId = credId, psaId = psaId), viewOnly = false)

  private val insurerName = "test insurer"
  private val insurerPolicyNo = "test"
  private val insurerAddress = Address("addr1", "addr2", None, None, Some("ZZ11ZZ"), "GB")

  case class Link(text: String, target: String, visuallyHiddenText: Option[Text] = None,
    attributes: Map[String, String] = Map.empty)

  private def summaryListRow(key: String, valueMsgKey: String, target: Option[Link] = None): Row = {
    SummaryList.Row(Key(GovUKMsg(key), List("govuk-!-width-one-half")), Value(GovUKMsg(valueMsgKey)), target.toSeq.map(
      t => Action(content = Html(s"<span aria-hidden=true >${t.text}</span>"), href = t.target,
        visuallyHiddenText = t.visuallyHiddenText, attributes = t.attributes)))
  }

  private def summaryListRowLiteral(key: String, value: String, target: Option[Link]): Row = {
    SummaryList.Row(Key(GovUKMsg(key), List("govuk-!-width-one-half")), Value(Literal(value)), target.toSeq.map(
      t => Action(content = Html(s"<span aria-hidden=true >${t.text}</span>"), href = t.target,
        visuallyHiddenText = t.visuallyHiddenText, attributes = t.attributes)))
  }

  private def summaryListRowHtml(key: String, value: Html, target: Option[Link]): Row = {
    SummaryList.Row(Key(GovUKMsg(key), List("govuk-!-width-one-half")), Value(value), target.toSeq.map(
      t => Action(content = Html(s"<span aria-hidden=true >${t.text}</span>"), href = t.target,
        visuallyHiddenText = t.visuallyHiddenText, attributes = t.attributes)))
  }

  private def addressAnswer(addr: Address)(implicit messages: Messages): Html = {
    def addrLineToHtml(l: String): String = s"""<span class="govuk-!-display-block">$l</span>"""

    Html(addrLineToHtml(addr.addressLine1) + addrLineToHtml(addr.addressLine2) + addr.addressLine3
      .fold("")(addrLineToHtml) + addr.addressLine4.fold("")(addrLineToHtml) + addr.postcode
      .fold("")(addrLineToHtml) + addrLineToHtml(messages("country." + addr.country)))
  }

  private def answerBenefitsAddressTransform(addr: Address)(implicit messages: Messages): Html = addressAnswer(addr)

  // scalastyle:off magic.number
  "BenefitsAndInsuranceCYAHelper" must {
    "return all rows with correct change link, value and visually hidden text" in {
      val ua: UserAnswers = UserAnswers().setOrException(SchemeNameId, schemeName)
        .setOrException(SchemeTypeId, SchemeType.Other("other")).setOrException(IsInvestmentRegulatedId, true)
        .setOrException(IsOccupationalId, true)
        .setOrException(HowProvideBenefitsId, BenefitsProvisionType.MoneyPurchaseOnly)
        .setOrException(BenefitsTypeId, BenefitsType.CashBalanceBenefits).setOrException(AreBenefitsSecuredId, true)
        .setOrException(BenefitsInsuranceNameId, insurerName).setOrException(BenefitsInsurancePolicyId, insurerPolicyNo)
        .setOrException(InsurerAddressId, insurerAddress)

      val result = benefitsAndInsuranceCYAHelper.rows(dataRequest(ua), messages)

      result.head mustBe summaryListRow(key = Messages("isInvestmentRegulated.h1", schemeName),
        valueMsgKey = "booleanAnswer.true")
      result(1) mustBe summaryListRow(key = Messages("isOccupational.h1", schemeName),
        valueMsgKey = "booleanAnswer.true")

      result(2) mustBe summaryListRow(key = Messages("howProvideBenefits.h1", schemeName),
        valueMsgKey = "howProvideBenefits.moneyPurchaseOnly", Some(Link(text = Messages("site.change"),
          target = controllers.benefitsAndInsurance.routes.HowProvideBenefitsController.onPageLoad.url,
          visuallyHiddenText = Some(Literal(Messages("site.change") + " " + Messages("howProvideBenefits.visuallyHidden", schemeName))),
          attributes = Map("id" -> "cya-0-2-change"))))

      result(3) mustBe summaryListRow(key = Messages("benefitsType.h1", schemeName),
        valueMsgKey = "benefitsType.cashBalanceBenefits", Some(Link(text = Messages("site.change"),
          target = controllers.benefitsAndInsurance.routes.BenefitsTypeController.onPageLoad.url,
          visuallyHiddenText = Some(Literal(Messages("site.change") + " " + Messages("benefitsType.visuallyHidden", schemeName))),
          attributes = Map("id" -> "cya-0-3-change"))))





      result(4) mustBe summaryListRow(key = Messages("areBenefitsSecured.title"), valueMsgKey = "booleanAnswer.true",
        Some(Link(text = Messages("site.change"),
          target = controllers.benefitsAndInsurance.routes.AreBenefitsSecuredController.onPageLoad.url,
          visuallyHiddenText = Some(Literal(Messages("site.change") + " " + Messages("areBenefitsSecured.visuallyHidden"))),
          attributes = Map("id" -> "cya-0-4-change"))))

      result(5) mustBe summaryListRowLiteral(key = Messages("benefitsInsuranceName.title"), value = insurerName, Some(
        Link(text = Messages("site.change"),
          target = controllers.benefitsAndInsurance.routes.BenefitsInsuranceNameController.onPageLoad.url,
          visuallyHiddenText = Some(Literal(Messages("site.change") + " " + Messages("benefitsInsuranceName.visuallyHidden"))),
          attributes = Map("id" -> "cya-0-5-change"))))

      result(6) mustBe summaryListRowLiteral(key = Messages("benefitsInsurancePolicy.h1", insurerName),
        value = insurerPolicyNo, Some(Link(text = Messages("site.change"),
          target = controllers.benefitsAndInsurance.routes.BenefitsInsurancePolicyController.onPageLoad.url,
          visuallyHiddenText = Some(Literal(Messages("site.change") + " " + Messages("benefitsInsurancePolicy.visuallyHidden"))),
          attributes = Map("id" -> "cya-0-6-change"))))

      result(7) mustBe summaryListRowHtml(key = messages("addressList.title", insurerName),
        value = answerBenefitsAddressTransform(insurerAddress), Some(Link(text = Messages("site.change"),
          target = controllers.benefitsAndInsurance.routes.InsurerEnterPostcodeController.onPageLoad.url,
          visuallyHiddenText = Some(Literal(Messages("site.change") + " " + Messages("addressList.visuallyHidden"))),
          attributes = Map("id" -> "cya-0-7-change"))))
    }
  }
}
