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

import identifiers.establishers.EstablisherKindId
import identifiers.establishers.individual.EstablisherNameId
import identifiers.establishers.individual.details._
import utils.UserAnswers

trait DataCompletionEstablishers extends DataCompletion {

  self: UserAnswers =>

  def isEstablisherIndividualComplete(index: Int): Boolean =
    isComplete(
      Seq(
        isAnswerComplete(EstablisherNameId(index)),
        isAnswerComplete(EstablisherKindId(index))
      )
    ).getOrElse(false)

  def isEstablisherIndividualDetailsCompleted(
                                               index: Int,
                                               userAnswers: UserAnswers
                                             ): Boolean = {
    val answerOrReasonNINOId: Boolean =
      userAnswers.get(EstablisherHasNINOId(index)).getOrElse(false)
    val answerOrReasonUTRId: Boolean =
      userAnswers.get(EstablisherHasUTRId(index)).getOrElse(false)

    isComplete(
      Seq(
        isAnswerComplete(EstablisherDOBId(index)),
        isAnswerComplete(EstablisherHasNINOId(index)),
        if (answerOrReasonNINOId)
          isAnswerComplete(EstablisherNINOId(index))
        else
          isAnswerComplete(EstablisherNoNINOReasonId(index)),
        isAnswerComplete(EstablisherHasUTRId(index)),
        if (answerOrReasonUTRId)
          isAnswerComplete(EstablisherUTRId(index))
        else
          isAnswerComplete(EstablisherNoUTRReasonId(index))
      )
    ).getOrElse(false)
  }

  def isEstablisherIndividualAddressCompleted: Option[Boolean] =
    isComplete(
      Seq(
        Some(false)
      )
    )

  def isEstablisherIndividualContactDetailsCompleted: Option[Boolean] =
    isComplete(
      Seq(
        Some(false)
      )
    )
}
