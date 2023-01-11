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

package forms.benefitsAndInsurance

import forms.behaviours.StringFieldBehaviours
import forms.mappings.Constraints
import play.api.data.FormError

class BenefitsInsuranceNameFormProviderSpec extends StringFieldBehaviours with Constraints {

  private val keyBenefitsInsuranceNameRequired = "benefitsInsuranceName.error.required"
  private val form = new BenefitsInsuranceNameFormProvider()()
  private val maxLength = 160

  "insurance name" must {

    val fieldName = "value"

    behave like fieldThatBindsValidData(
      form,
      fieldName,
      "abc"
    )

    behave like fieldWithMaxLength(
      form,
      fieldName,
      maxLength = maxLength,
      lengthError = FormError(fieldName, "benefitsInsuranceName.error.length", Seq(maxLength))
    )

    behave like mandatoryField(
      form,
      fieldName,
      requiredError = FormError(fieldName, keyBenefitsInsuranceNameRequired)
    )
  }
}
