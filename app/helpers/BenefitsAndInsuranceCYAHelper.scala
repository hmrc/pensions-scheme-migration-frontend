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
import identifiers.benefitsAndInsurance._
import models.benefitsAndInsurance.BenefitsProvisionType.DefinedBenefitsOnly
import models.requests.DataRequest
import play.api.i18n.Messages
import play.api.mvc.AnyContent
import uk.gov.hmrc.viewmodels.{MessageInterpolators, SummaryList}
import utils.{UserAnswers, Enumerable}
import viewmodels.Message

class BenefitsAndInsuranceCYAHelper extends CYAHelper with Enumerable.Implicits{
  def rows(implicit request: DataRequest[AnyContent],
    messages: Messages
  ): Seq[SummaryList.Row] = {
      implicit val ua: UserAnswers = request.userAnswers
      val schemeName = CYAHelper.getAnswer(SchemeNameId)

      val seqTop = topSection(schemeName)
      val seqBottom = if(ua.get(AreBenefitsSecuredId).contains(true)) {

      val (msgHeadingInsurancePolicy, msgHeadingInsurerAddress) = {
          val optionInsuranceName = ua.get(BenefitsInsuranceNameId)
          Tuple2(
            optionInsuranceName.fold(Message("benefitsInsurancePolicy.noCompanyName.h1"))(Message("benefitsInsurancePolicy.h1", _)),
            optionInsuranceName.fold(Message("addressList.noInsuranceName.title"))(Message("addressList.title", _))
          )
        }

        Seq(
          answerOrAddRow(
            BenefitsInsuranceNameId,
            Message("benefitsInsuranceName.title").resolve,
            Some(controllers.benefitsAndInsurance.routes.BenefitsInsuranceNameController.onPageLoad().url),
            Some(msg"benefitsInsuranceName.visuallyHidden"), answerStringTransform
          ),
          answerOrAddRow(
            BenefitsInsurancePolicyId,
            msgHeadingInsurancePolicy.resolve,
            Some(controllers.benefitsAndInsurance.routes.BenefitsInsurancePolicyController.onPageLoad().url),
            Some(msg"benefitsInsurancePolicy.visuallyHidden"), answerStringTransform
          ),
          answerOrAddRow(
            InsurerAddressId,
            msgHeadingInsurerAddress.resolve,
            Some(controllers.benefitsAndInsurance.routes.InsurerEnterPostcodeController.onPageLoad().url),
            Some(msg"addressList.visuallyHidden"), answerAddressTransform
          )
      )
    } else
      Nil

   val rowsWithoutDynamicIndices =  seqTop ++ seqBottom
    rowsWithDynamicIndices(rowsWithoutDynamicIndices)
  }



  private def topSection(schemeName: String)(implicit messages: Messages, ua: UserAnswers) = {
    val section1 = Seq(
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
        Some(msg"howProvideBenefits.visuallyHidden".withArgs(schemeName)), answerBenefitsProvisionTypeTransform
      )
    )
    val section2 = if(ua.get(HowProvideBenefitsId).contains(DefinedBenefitsOnly)) Nil else Seq(
      answerOrAddRow(
        BenefitsTypeId,
        Message("benefitsType.h1", schemeName).resolve,
        Some(controllers.benefitsAndInsurance.routes.BenefitsTypeController.onPageLoad().url),
        Some(msg"benefitsType.visuallyHidden".withArgs(schemeName)), answerBenefitsTypeTransform
      )
    )

    val section3 = Seq(
      answerOrAddRow(
        AreBenefitsSecuredId,
        Message("areBenefitsSecured.title").resolve,
        Some(controllers.benefitsAndInsurance.routes.AreBenefitsSecuredController.onPageLoad().url),
        Some(msg"areBenefitsSecured.visuallyHidden"), answerBooleanTransform
      )
    )
    section1 ++ section2 ++ section3
  }
}
