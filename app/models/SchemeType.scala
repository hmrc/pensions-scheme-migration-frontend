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

package models

import play.api.libs.json._
import utils.{InputOption, WithName}

sealed trait SchemeType

object SchemeType {
  val other = "other"
  val mappings: Map[String, SchemeType] = Seq(
    SingleTrust,
    GroupLifeDeath,
    BodyCorporate
  ).map(v => (v.toString, v)).toMap

  def options: Seq[InputOption] = {
    val key = "scheme_type"
    Seq(
      InputOption(
        SingleTrust.toString,
        s"messages__${key}_${SingleTrust.toString}",
        hint = Set(s"messages__${key}_single_hint")
      ),
      InputOption(
        GroupLifeDeath.toString,
        s"messages__${key}_${GroupLifeDeath.toString}",
        hint = Set(s"messages__${key}_group_hint")
      ),
      InputOption(
        BodyCorporate.toString,
        s"messages__${key}_${BodyCorporate.toString}",
        hint = Set(s"messages__${key}_corp_hint")
      ),
      InputOption(
        other,
        s"messages__${key}_$other",
        Some("schemeType_schemeTypeDetails-form"),
        hint = Set(s"messages__${key}_other_hint")
      )
    )
  }

  def getSchemeType(schemeTypeStr: Option[String]): Option[String] =
      schemeTypeStr.flatMap { schemeStr =>
        List(SingleTrust.toString, GroupLifeDeath.toString, BodyCorporate.toString, other).find(scheme =>
          schemeStr.toLowerCase.contains(scheme.toLowerCase)).map { str =>
          s"messages__scheme_details__type_$str"
        }
      }

  case class Other(schemeTypeDetails: String) extends WithName("other") with SchemeType

  case object SingleTrust extends WithName("single") with SchemeType

  case object GroupLifeDeath extends WithName("group") with SchemeType

  case object BodyCorporate extends WithName("corp") with SchemeType

  implicit val reads: Reads[SchemeType] = {

    (JsPath \ "name").read[String].flatMap {

      case schemeTypeName if schemeTypeName == other =>
        (JsPath \ "schemeTypeDetails").read[String]
          .map[SchemeType](Other.apply)
          .orElse(Reads[SchemeType](_ => JsError("Other Value expected")))

      case schemeTypeName if mappings.keySet.contains(schemeTypeName) =>
        Reads(_ => JsSuccess(mappings.apply(schemeTypeName)))

      case _ => Reads(_ => JsError("Invalid Scheme Type"))
    }
  }

  implicit lazy val writes: Writes[SchemeType] = new Writes[SchemeType] {
    def writes(o: SchemeType): JsObject = {
      o match {
        case SchemeType.Other(schemeTypeDetails) =>
          Json.obj("name" -> other, "schemeTypeDetails" -> schemeTypeDetails)
        case s if mappings.keySet.contains(s.toString) =>
          Json.obj("name" -> s.toString)
      }
    }
  }

}