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

package utils.datacompletion

import identifiers.trustees.TrusteeKindId
import identifiers.trustees.individual.TrusteeNameId
import utils.UserAnswers

trait DataCompletionTrustees extends DataCompletion {

  self: UserAnswers =>

  def isTrusteeIndividualComplete(index: Int): Boolean =
    isComplete(
      Seq(
        isAnswerComplete(TrusteeNameId(index)),
        isAnswerComplete(TrusteeKindId(index))
      )
    ).getOrElse(false)

  //def isTrusteeIndividualDetailsCompleted(
  //                                             index: Int,
  //                                             userAnswers: UserAnswers
  //                                           ): Boolean = {
  //  isComplete(
  //    Seq(
  //      Some(false)
  //    )
  //  )
  //}
  //
  //def isTrusteeIndividualAddressCompleted(
  //  index: Int,
  //  userAnswers: UserAnswers
  //): Option[Boolean] = {
  //  isComplete(
  //    Seq(
  //      Some(false)
  //    )
  //  )
  //}

  def isTrusteeIndividualContactDetailsCompleted: Option[Boolean] =
    isComplete(
      Seq(
        Some(false)
      )
    )
}
