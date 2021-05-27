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
import identifiers.benefitsAndInsurance.{InsurerAddressListId, InsurerAddressId, HowProvideBenefitsId, IsInvestmentRegulatedId, IsOccupationalId}
import models.{Members, Address}
import play.api.i18n.Messages
import play.api.mvc.AnyContent
import uk.gov.hmrc.viewmodels.{MessageInterpolators, SummaryList, Text}
import utils.{UserAnswers, Enumerable}
import viewmodels.Message
import identifiers.aboutMembership.{FutureMembersId, CurrentMembersId}
import identifiers.beforeYouStart.SchemeNameId
import models.Members
import models.benefitsAndInsurance.BenefitsProvisionType
import models.requests.DataRequest
import play.api.i18n.Messages
import play.api.mvc.AnyContent
import uk.gov.hmrc.viewmodels.{MessageInterpolators, SummaryList, Text}
import utils.{UserAnswers, Enumerable}
import viewmodels.Message

class BenefitsAndInsuranceCYAHelper extends CYAHelper with Enumerable.Implicits{
  def rows(implicit request: DataRequest[AnyContent],
    messages: Messages
  ): Seq[SummaryList.Row] = {
    implicit val ua: UserAnswers = request.userAnswers
    val schemeName = getAnswer(SchemeNameId)

    val answerBooleanTransform: Option[Boolean => Text] = Some(opt => msg"booleanAnswer.${opt.toString}")
    val answerBenefitsProvisionTypeTransform: Option[BenefitsProvisionType => Text] = Some(opt => msg"howProvideBenefits.${opt.toString}")
    val answerBenefitsAddressTransform: Option[Address => Text] = Some(opt => lit"${opt.toString}")

    val seq = Seq(
      answerOrAddRow(
        IsInvestmentRegulatedId,
        Message("isInvestmentRegulated.h1", schemeName).resolve,
        controllers.benefitsAndInsurance.routes.IsInvestmentRegulatedController.onPageLoad().url,
        Some(msg"messages__visuallyhidden__currentMembers"), answerBooleanTransform
      ),
      answerOrAddRow(
        IsOccupationalId,
        Message("isOccupational.h1", schemeName).resolve,
        controllers.benefitsAndInsurance.routes.IsOccupationalController.onPageLoad().url,
        Some(msg"messages__visuallyhidden__currentMembers"), answerBooleanTransform
      ),
      answerOrAddRow(
        HowProvideBenefitsId,
        Message("howProvideBenefits.h1", schemeName).resolve,
        controllers.benefitsAndInsurance.routes.HowProvideBenefitsController.onPageLoad().url,
        Some(msg"messages__visuallyhidden__currentMembers"), answerBenefitsProvisionTypeTransform
      ),
      answerOrAddRow(
        InsurerAddressId,
        Message("addressFor", schemeName).resolve,
        controllers.benefitsAndInsurance.routes.InsurerEnterPostcodeController.onPageLoad().url,
        Some(msg"messages__visuallyhidden__currentMembers"), answerBenefitsAddressTransform
      )
    )


    seq
  }
}
