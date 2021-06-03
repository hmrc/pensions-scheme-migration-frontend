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
import identifiers.beforeYouStart.{SchemeNameId, SchemeTypeId}
import identifiers.benefitsAndInsurance._
import models.benefitsAndInsurance.{BenefitsProvisionType, BenefitsType}
import models.requests.DataRequest
import models.{SchemeType, Address, MigrationLock}
import org.scalatest.{TryValues, MustMatchers, WordSpec}
import play.api.i18n.Messages
import play.api.mvc.{Call, AnyContent}
import uk.gov.hmrc.domain.PsaId
import uk.gov.hmrc.viewmodels.{Content, SummaryList, Html}
import uk.gov.hmrc.viewmodels.SummaryList._
import utils.Data.{pstr, schemeName, psaId, credId}
import utils.{UserAnswers, CountryOptions, Enumerable, InputOption}
import uk.gov.hmrc.viewmodels.Text.{Message => GovUKMsg}

class BenefitsAndInsuranceCYAHelperSpec
  extends WordSpec
    with MustMatchers
    with TryValues
    with Enumerable.Implicits {

  val options = Seq(InputOption("territory:AE-AZ", "Abu Dhabi"), InputOption("country:AF", "Afghanistan"))

  implicit val countryOptions: CountryOptions = new CountryOptions(options)

  val benefitsAndInsuranceCYAHelper = new BenefitsAndInsuranceCYAHelper

  private def dataRequest(ua: UserAnswers) = DataRequest[AnyContent](
    request = fakeRequest,
    userAnswers = ua,
    psaId = PsaId(psaId),
    lock = MigrationLock(pstr = pstr, credId = credId, psaId = psaId),
    viewOnly = false
  )

  private val insurerName= "test insurer"
  private val insurerPolicyNo = "test"
  private val insurerAddress = Address("addr1", "addr2", None, None, Some("ZZ11ZZ"), "GB")
  /*
  (
content: Content,
href: String,
visuallyHiddenText: Option[Text] = None,
classes: Seq[String] = Seq.empty,
attributes: Map[String, String] = Map.empty
)
   */
  private def summaryListRow(key:String, valueMsgKey:String, target:Option[Tuple3[String,String,Call]] = None):Row = {
    SummaryList.Row(
      Key(
        GovUKMsg(key),
        List("govuk-!-width-one-half")
      ),
      Value(
        GovUKMsg(valueMsgKey)
      ),
      target.toSeq.map(xx => Action(
        content = Html(xx._1),
        href = xx._3.url,
        visuallyHiddenText = Some(GovUKMsg(xx._2))
      ))
    )
  }

  "BenefitsAndInsuranceCYAHelper" must {
    "return all rows" in {
      val ua: UserAnswers = UserAnswers()
        .setOrException(SchemeNameId, schemeName)
        .setOrException(SchemeTypeId, SchemeType.Other("other"))
        .setOrException(IsInvestmentRegulatedId, true)
        .setOrException(IsOccupationalId, true)
        .setOrException(HowProvideBenefitsId, BenefitsProvisionType.MoneyPurchaseOnly)
        .setOrException(BenefitsTypeId, BenefitsType.CashBalanceBenefits)
        .setOrException(AreBenefitsSecuredId, true)
        .setOrException(BenefitsInsuranceNameId, insurerName)
        .setOrException(BenefitsInsurancePolicyId, insurerPolicyNo)
        .setOrException(InsurerAddressId, insurerAddress)

      val result = benefitsAndInsuranceCYAHelper.rows(dataRequest(ua), messages)

      result.head mustBe summaryListRow(key = Messages("isInvestmentRegulated.h1", schemeName), valueMsgKey = "booleanAnswer.true")
      result(1) mustBe summaryListRow(key = Messages("isOccupational.h1", schemeName), valueMsgKey = "booleanAnswer.true")



      result(2) mustBe summaryListRow(
        key = Messages("howProvideBenefits.h1", schemeName),
        valueMsgKey = "howProvideBenefits.moneyPurchaseOnly",
        Some(
          Messages("site.change"),
          "howProvideBenefits.visuallyHidden",
          controllers.benefitsAndInsurance.routes.HowProvideBenefitsController.onPageLoad()
        )
      )
    }

  }
}
