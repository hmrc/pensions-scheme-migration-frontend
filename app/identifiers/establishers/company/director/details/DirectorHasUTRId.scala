/*
 * Copyright 2023 HM Revenue & Customs
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

package identifiers.establishers.company.director.details

import identifiers._
import identifiers.establishers.EstablishersId
import play.api.libs.json.{Format, JsPath, Json}
import utils.UserAnswers

case class DirectorHasUTRId(establisherIndex: Int, directorIndex: Int) extends TypedIdentifier[Boolean] {
  override def path: JsPath =
    EstablishersId(establisherIndex).path \ "director" \ directorIndex \ DirectorHasUTRId.toString

  override def cleanup(value: Option[Boolean], userAnswers: UserAnswers): UserAnswers = {
    value match {
      case Some(true) =>
        userAnswers.remove(DirectorNoUTRReasonId(establisherIndex, directorIndex))
      case Some(false) =>
        userAnswers.remove(DirectorEnterUTRId(establisherIndex, directorIndex))
      case _ =>
        super.cleanup(value, userAnswers)
    }
  }
}

object DirectorHasUTRId {
  override def toString: String = "hasUtr"

  implicit lazy val formats: Format[DirectorHasUTRId] =
    Json.format[DirectorHasUTRId]
}


