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

package forms

import forms.behaviours.StringFieldBehaviours
import forms.mappings.Constraints
import play.api.data.FormError

class EmailFormProviderSpec extends StringFieldBehaviours with Constraints {
  private val maxEmailLength = 132
  private val keyMaxLength = "messages__enterEmail__error_maxLength"
  val keyEmailRequired = "messages__enterEmail__error_required"
  val keyInvalid = "messages__enterEmail__error_invalid"
  val form = new EmailFormProvider()(keyEmailRequired)

  "emailAddress" must {

    val fieldName = "value"

    behave like fieldThatBindsValidData(
      form,
      fieldName,
      "ab@test.com"
    )

    behave like mandatoryField(
      form,
      fieldName,
      requiredError = FormError(fieldName, keyEmailRequired)
    )

    behave like fieldWithRegex(
      form,
      fieldName,
      "ABC",
      FormError(fieldName, keyInvalid, Seq(regexEmailRestrictive))
    )

    behave like fieldWithMaxLength(
      form,
      fieldName,
      maxLength = 132,
      lengthError = FormError(fieldName, keyMaxLength, Seq(maxEmailLength))
    )
  }

}
