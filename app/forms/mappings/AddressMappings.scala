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

package forms.mappings

import play.api.data.Forms.of
import play.api.data.{FieldMapping, Mapping}
import utils.CountryOptions

trait AddressMappings extends Mappings with Constraints {

  def addressLineMapping(keyRequired: String, keyLength: String, keyInvalid: String): Mapping[String] =
    text(keyRequired)
      .verifying(
        firstError(
          maxLength(
            AddressMappings.maxAddressLineLength,
            keyLength
          ),
          validAddressLine(keyInvalid)
        )
      )

  def optionalAddressLineMapping(keyLength: String, keyInvalid: String): Mapping[Option[String]] =
    optionalText()
      .verifying(
        firstError(
          firstError(
            optionalMaxLength(AddressMappings.maxAddressLineLength, keyLength),
            optionalValidAddressLine(keyInvalid)
          )
        )
      )



  def optionalPostcode(requiredKey: Option[String],
    invalidKey: String, nonUkLengthKey: String, countryFieldName: String): FieldMapping[Option[String]] =
    of(optionalPostcodeFormatter(requiredKey, invalidKey, nonUkLengthKey, countryFieldName))

  protected def postcode(requiredKey: String, invalidKey: String): FieldMapping[String] =
    of(postcodeFormatter(requiredKey, invalidKey))

  def countryMapping(countryOptions: CountryOptions, keyRequired: String, keyInvalid: String): Mapping[String] = {
    text(keyRequired)
      .verifying(country(countryOptions, keyInvalid))
  }

}

object AddressMappings {
  val maxAddressLineLength = 35
  val maxPostCodeLength = 8
}
