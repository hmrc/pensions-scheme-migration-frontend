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

package identifiers.trustees.partnership.address

import identifiers.TypedIdentifier
import identifiers.trustees.TrusteesId
import play.api.libs.json.{Format, JsPath, Json}

case class PreviousAddressListId(index: Int) extends TypedIdentifier[Int] {
  override def path: JsPath =
    TrusteesId(index).path \ PreviousAddressListId.toString
}

object PreviousAddressListId {
  override lazy val toString: String = "previousAddressList"

  implicit lazy val formats: Format[PreviousAddressListId] =
    Json.format[PreviousAddressListId]
}
