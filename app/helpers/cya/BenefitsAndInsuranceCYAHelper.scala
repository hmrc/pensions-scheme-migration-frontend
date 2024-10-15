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

package helpers.cya

import identifiers.beforeYouStart.SchemeNameId
import identifiers.benefitsAndInsurance._
import models.benefitsAndInsurance.BenefitsProvisionType.DefinedBenefitsOnly
import models.requests.DataRequest
import play.api.i18n.Messages
import play.api.mvc.AnyContent
import uk.gov.hmrc.govukfrontend.views.Aliases.Text
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import utils.{Enumerable, UserAnswers}

class BenefitsAndInsuranceCYAHelper extends CYAHelper with Enumerable.Implicits {
  def rows(implicit request: DataRequest[AnyContent],
           messages: Messages
          ): Seq[SummaryListRow] = {
    implicit val ua: UserAnswers = request.userAnswers
    val schemeName = CYAHelper.getAnswer(SchemeNameId)

    val seqTop = topSection(schemeName)
    val seqBottom = if (ua.get(AreBenefitsSecuredId).contains(true)) {

      val (msgHeadingInsurancePolicy, msgHeadingInsurerAddress) = {
        val optionInsuranceName = ua.get(BenefitsInsuranceNameId)
        Tuple2(
          optionInsuranceName.fold(Messages("benefitsInsurancePolicy.noCompanyName.h1"))(Messages("benefitsInsurancePolicy.h1", _)),
          optionInsuranceName.fold(Messages("addressList.noInsuranceName.title"))(Messages("addressList.title", _))
        )
      }

      Seq(
        answerOrAddRow(
          BenefitsInsuranceNameId,
          Messages("benefitsInsuranceName.title"),
          Some(controllers.benefitsAndInsurance.routes.BenefitsInsuranceNameController.onPageLoad.url),
          Some(Text(Messages("benefitsInsuranceName.visuallyHidden"))), answerStringTransform
        ),
        answerOrAddRow(
          BenefitsInsurancePolicyId,
          msgHeadingInsurancePolicy,
          Some(controllers.benefitsAndInsurance.routes.BenefitsInsurancePolicyController.onPageLoad.url),
          Some(Text(Messages("benefitsInsurancePolicy.visuallyHidden"))), answerStringTransform
        ),
        answerOrAddRow(
          InsurerAddressId,
          msgHeadingInsurerAddress,
          Some(controllers.benefitsAndInsurance.routes.InsurerEnterPostcodeController.onPageLoad.url),
          Some(Text(Messages("addressList.visuallyHidden"))), answerAddressTransform
        )
      )
    } else
      Nil

    val rowsWithoutDynamicIndices = seqTop ++ seqBottom
    rowsWithDynamicIndices(rowsWithoutDynamicIndices)
  }


  private def topSection(schemeName: String)(implicit messages: Messages, ua: UserAnswers) = {
    val seqRowFirstItems = Seq(
      answerOrAddRow(
        IsInvestmentRegulatedId,
        Messages("isInvestmentRegulated.h1", schemeName),
        None,
        Some(Text(Messages("messages__visuallyhidden__currentMembers"))), answerBooleanTransform
      ),
      answerOrAddRow(
        IsOccupationalId,
        Messages("isOccupational.h1", schemeName),
        None,
        Some(Text(Messages("messages__visuallyhidden__currentMembers"))), answerBooleanTransform
      ),
      answerOrAddRow(
        HowProvideBenefitsId,
        Messages("howProvideBenefits.h1", schemeName),
        Some(controllers.benefitsAndInsurance.routes.HowProvideBenefitsController.onPageLoad.url),
        Some(Text(Messages("howProvideBenefits.visuallyHidden",schemeName))), answerBenefitsProvisionTypeTransform
      )
    )
    val seqRowBenefitsType = if ((ua.get(HowProvideBenefitsId).contains(DefinedBenefitsOnly)) || ua.get(HowProvideBenefitsId).isEmpty) Nil else Seq(
      answerOrAddRow(
        BenefitsTypeId,
        Messages("benefitsType.h1", schemeName),
        Some(controllers.benefitsAndInsurance.routes.BenefitsTypeController.onPageLoad.url),
        Some(Text(Messages("benefitsType.visuallyHidden", schemeName))), answerBenefitsTypeTransform
      )
    )

    val seqRowBenefitsSecured = Seq(
      answerOrAddRow(
        AreBenefitsSecuredId,
        Messages("areBenefitsSecured.title"),
        Some(controllers.benefitsAndInsurance.routes.AreBenefitsSecuredController.onPageLoad.url),
        Some(Text(Messages("areBenefitsSecured.visuallyHidden"))), answerBooleanTransform
      )
    )
    seqRowFirstItems ++ seqRowBenefitsType ++ seqRowBenefitsSecured
  }
}
