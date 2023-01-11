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

package forms.address

import forms.behaviours.StringFieldBehaviours
import forms.mappings.Constraints
import play.api.data.FormError

class PostcodeFormProviderSpec extends StringFieldBehaviours with Constraints {

  private val keyRequired = "required"
  private val keyInvalid = "required"
  private val form = new PostcodeFormProvider()(keyRequired, keyInvalid)

  "postcode for provider" must {

    val fieldName = "value"

    behave like fieldThatBindsValidData(
      form,
      fieldName,
      "zz11zz"
    )

    s"not bind when key is invalid for form with error message $keyInvalid" in {
      val result = form.bind(Map(fieldName -> "zz")).apply(fieldName)
      result.errors mustEqual Seq(FormError(fieldName, keyInvalid))
    }

    behave like mandatoryField(
      form,
      fieldName,
      requiredError = FormError(fieldName, keyRequired)
    )
  }
}
