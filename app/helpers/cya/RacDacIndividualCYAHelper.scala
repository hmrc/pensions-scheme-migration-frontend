/*
 * Copyright 2022 HM Revenue & Customs
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
import identifiers.racdac.ContractOrPolicyNumberId
import play.api.i18n.Messages
import uk.gov.hmrc.viewmodels.SummaryList.Row
import utils.{Enumerable, UserAnswers}
import viewmodels.Message

class RacDacIndividualCYAHelper
  extends CYAHelper
    with Enumerable.Implicits {

  def detailsRows(userAnswers:UserAnswers)(
                         implicit messages: Messages
                        ): Seq[Row] = {
    val racDacName: String =
      userAnswers.get(SchemeNameId)
        .getOrElse(throw MandatoryAnswerMissingException)

    val rowsWithoutDynamicIndices = Seq(
      Some(answerOrAddRow(
        id = SchemeNameId,
        message = Message("messages__racdac__cya__name__label").resolve
      )(userAnswers,implicitly,implicitly)),
      Some(answerOrAddRow(
        id = ContractOrPolicyNumberId,
        message = Message("messages__racdac__cya__contract__or__policy_number__label", racDacName).resolve
      )(userAnswers,implicitly,implicitly))).flatten

    rowsWithDynamicIndices(rowsWithoutDynamicIndices)

  }
}
