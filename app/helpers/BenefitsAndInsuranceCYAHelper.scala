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

import identifiers.beforeYouStart.SchemeNameId
import identifiers.benefitsAndInsurance.{AreBenefitsSecuredId, BenefitsInsurancePolicyId, InsurerAddressListId, BenefitsInsuranceNameId, InsurerAddressId, BenefitsTypeId, HowProvideBenefitsId, IsInvestmentRegulatedId, IsOccupationalId}
import models.{Address, Members}
import play.api.i18n.Messages
import play.api.mvc.AnyContent
import uk.gov.hmrc.viewmodels.{MessageInterpolators, SummaryList, Text, Html}
import viewmodels.Message
import identifiers.aboutMembership.{FutureMembersId, CurrentMembersId}
import identifiers.beforeYouStart.SchemeNameId
import models.Members
import models.benefitsAndInsurance.BenefitsProvisionType.DefinedBenefitsOnly
import models.benefitsAndInsurance.{BenefitsProvisionType, BenefitsType}
import models.requests.DataRequest
import play.api.i18n.Messages
import play.api.mvc.AnyContent
import uk.gov.hmrc.viewmodels.{MessageInterpolators, SummaryList, Text}
import utils.{UserAnswers, Enumerable}
import viewmodels.Message

class BenefitsAndInsuranceCYAHelper extends CYAHelper with Enumerable.Implicits{

  private def addressAnswer(addr: Address)(implicit messages: Messages): Html = {
    def addrLineToHtml(l: String): String = s"""<span class="govuk-!-display-block">$l</span>"""

    Html(
      addrLineToHtml(addr.addressLine1) +
        addrLineToHtml(addr.addressLine2) +
        addr.addressLine3.fold("")(addrLineToHtml) +
        addr.addressLine4.fold("")(addrLineToHtml) +
        addr.postcode.fold("")(addrLineToHtml) +
        addrLineToHtml(messages("country." + addr.country))
    )
  }

  private val answerBooleanTransform: Option[Boolean => Text] = Some(opt => msg"booleanAnswer.${opt.toString}")
  private val answerStringTransform: Option[String => Text] = Some(opt => lit"${opt}")
  private val answerBenefitsProvisionTypeTransform: Option[BenefitsProvisionType => Text] = Some(opt => msg"howProvideBenefits.${opt.toString}")
  private val answerBenefitsTypeTransform: Option[BenefitsType => Text] = Some(opt => msg"benefitsType.${opt.toString}")
  private def answerBenefitsAddressTransform(implicit messages: Messages): Option[Address => Html] = Some(opt => addressAnswer(opt))


  def rows(implicit request: DataRequest[AnyContent],
    messages: Messages
  ): Seq[SummaryList.Row] = {
    implicit val ua: UserAnswers = request.userAnswers
    val schemeName = CYAHelper.getAnswer(SchemeNameId)
    val insuranceNo = CYAHelper.getAnswer(BenefitsInsuranceNameId)

    val seqTop = topSection(schemeName)
    val seqBottom = if(ua.get(AreBenefitsSecuredId).contains(true))
      Seq(
        answerOrAddRow(
          BenefitsInsuranceNameId,
          Message("benefitsInsuranceName.title").resolve,
          Some(controllers.benefitsAndInsurance.routes.BenefitsInsuranceNameController.onPageLoad().url),
          Some(msg"messages__visuallyhidden__currentMembers"), answerStringTransform
        ),
        answerOrAddRow(
          BenefitsInsurancePolicyId,
          Message("benefitsInsurancePolicy.h1", insuranceNo).resolve,
          Some(controllers.benefitsAndInsurance.routes.BenefitsInsurancePolicyController.onPageLoad().url),
          Some(msg"messages__visuallyhidden__currentMembers"), answerStringTransform
        ),
        answerOrAddRow(
          InsurerAddressId,
          Message("addressFor", insuranceNo).resolve,
          Some(controllers.benefitsAndInsurance.routes.InsurerEnterPostcodeController.onPageLoad().url),
          Some(msg"messages__visuallyhidden__currentMembers"), answerBenefitsAddressTransform
        )
    )
    else
      Nil

    seqTop ++ seqBottom
  }



  private def topSection(schemeName: String)(implicit messages: Messages, ua: UserAnswers) = {
    val s1 = Seq(
      answerOrAddRow(
        IsInvestmentRegulatedId,
        Message("isInvestmentRegulated.h1", schemeName).resolve,
        None,
        Some(msg"messages__visuallyhidden__currentMembers"), answerBooleanTransform
      ),
      answerOrAddRow(
        IsOccupationalId,
        Message("isOccupational.h1", schemeName).resolve,
        None,
        Some(msg"messages__visuallyhidden__currentMembers"), answerBooleanTransform
      ),
      answerOrAddRow(
        HowProvideBenefitsId,
        Message("howProvideBenefits.h1", schemeName).resolve,
        Some(controllers.benefitsAndInsurance.routes.HowProvideBenefitsController.onPageLoad().url),
        Some(msg"messages__visuallyhidden__currentMembers"), answerBenefitsProvisionTypeTransform
      )
    )
    val s2 = if(ua.get(HowProvideBenefitsId).contains(DefinedBenefitsOnly)) Nil else Seq(
      answerOrAddRow(
        BenefitsTypeId,
        Message("benefitsType.h1", schemeName).resolve,
        Some(controllers.benefitsAndInsurance.routes.BenefitsTypeController.onPageLoad().url),
        Some(msg"messages__visuallyhidden__currentMembers"), answerBenefitsTypeTransform
      )
    )

    val s3 = Seq(
      answerOrAddRow(
        AreBenefitsSecuredId,
        Message("areBenefitsSecured.title").resolve,
        Some(controllers.benefitsAndInsurance.routes.AreBenefitsSecuredController.onPageLoad().url),
        Some(msg"messages__visuallyhidden__currentMembers"), answerBooleanTransform
      )
    )
    s1 ++ s2 ++ s3
  }
}
