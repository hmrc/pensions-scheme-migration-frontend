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

package identifiers.establishers.individual.details

import identifiers.TypedIdentifier
import identifiers.establishers.EstablishersId
import play.api.libs.json.{Format, JsPath, Json}
import utils.UserAnswers

case class EstablisherHasNINOId(index: Int) extends TypedIdentifier[Boolean] {
  override def path: JsPath =
    EstablishersId(index).path \ EstablisherHasNINOId.toString

  override def cleanup(
                        value: Option[Boolean],
                        userAnswers: UserAnswers
                      ): UserAnswers =
    value match {
      case Some(true) =>
        userAnswers.remove(EstablisherNoNINOReasonId(index))
      case Some(false) =>
        userAnswers.remove(EstablisherNINOId(index))
      case _ =>
        super.cleanup(value, userAnswers)
    }
}

object EstablisherHasNINOId {
  override lazy val toString: String =
    "hasNino"

  implicit lazy val formats: Format[EstablisherHasNINOId] =
    Json.format[EstablisherHasNINOId]
}
