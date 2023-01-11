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

package identifiers.establishers.company.details

import identifiers.TypedIdentifier
import identifiers.establishers.EstablishersId
import play.api.libs.json.{Format, JsPath, Json}
import utils.UserAnswers

case class HaveUTRId(index: Int) extends TypedIdentifier[Boolean] {
  override def path: JsPath = EstablishersId(index).path \ HaveUTRId.toString

  override def cleanup(value: Option[Boolean], userAnswers: UserAnswers): UserAnswers =
    value match {
      case Some(true) => userAnswers.remove(NoUTRReasonId(index))
      case Some(false) => userAnswers.remove(CompanyUTRId(index))
      case _ => super.cleanup(value, userAnswers)
    }
}

object HaveUTRId {
  override lazy val toString: String = "hasUtr"
  implicit lazy val formats: Format[HaveUTRId] = Json.format[HaveUTRId]
}


