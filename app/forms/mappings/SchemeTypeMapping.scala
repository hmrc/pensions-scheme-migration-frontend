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

package forms.mappings

import models.SchemeType
import models.SchemeType.{BodyCorporate, GroupLifeDeath, Other, SingleTrust}
import play.api.data.Forms.tuple
import play.api.data.Mapping
import uk.gov.voa.play.form.ConditionalMappings.mandatoryIfEqual

trait SchemeTypeMapping
  extends Formatters
    with Constraints
    with Mappings {

  protected def schemeTypeMapping(
                                   requiredTypeKey: String = "messages__scheme_type__error__required",
                                   invalidTypeKey: String = "messages__error__scheme_type_information",
                                   requiredOtherKey: String = "messages__error__scheme_type_information",
                                   lengthOtherKey: String = "messages__error__scheme_type_other_length",
                                   invalidOtherKey: String = "messages__error__scheme_type_other_invalid"
                                 ): Mapping[SchemeType] = {
    val other = "other"

    def fromSchemeType(schemeType: SchemeType): (String, Option[String]) =
      schemeType match {
        case SchemeType.Other(someValue) =>
          (other, Some(someValue))
        case _ =>
          (schemeType.toString, None)
      }

    def toSchemeType(schemeTypeTuple: (String, Option[String])): SchemeType = {

      val mappings: Map[String, SchemeType] = Seq(
        SingleTrust,
        GroupLifeDeath,
        BodyCorporate
      ).map(v => (v.toString, v)).toMap

      schemeTypeTuple match {
        case (key, Some(value)) if key == other =>
          Other(value)
        case (key, _) if mappings.keySet.contains(key) =>
          mappings.apply(key)
        case (key, _) => throw new RuntimeException(s"Invalid match $key")
      }
    }
    // scalastyle:off magic.number
    tuple(
      "type" -> text(requiredTypeKey)
        .verifying(schemeTypeConstraint(invalidTypeKey)),
      "schemeTypeDetails" -> mandatoryIfEqual(
        fieldName = "schemeType.type",
        value = other,
        mapping = text(requiredOtherKey)
          .verifying(
            firstError(
              maxLength(160, lengthOtherKey),
              safeText(invalidOtherKey)
            )
          )
      )
    ).transform(toSchemeType, fromSchemeType)
    // scalastyle:on magic.number
  }
}
