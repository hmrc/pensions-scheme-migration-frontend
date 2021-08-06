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

package identifiers.establishers.company.director.contact

import identifiers._
import identifiers.establishers.EstablishersId
import play.api.libs.json.{Format, JsPath, Json}

case class EnterPhoneId(establisherIndex: Int, directorIndex: Int) extends TypedIdentifier[String] {
  override def path: JsPath =
    EstablishersId(establisherIndex)
      .path \ "director" \ directorIndex \ "directorContactDetails" \ EnterPhoneId.toString
}


object EnterPhoneId {
  override def toString: String = "phoneNumber"

  def collectionPath(establisherIndex: Int, directorIndex: Int): JsPath =
    EstablishersId(establisherIndex)
      .path \ "director" \ directorIndex \ "directorContactDetails" \ EnterPhoneId.toString

  implicit lazy val formats: Format[EnterPhoneId] =
    Json.format[EnterPhoneId]
}
