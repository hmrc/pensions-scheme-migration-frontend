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

package forms.establishers

import forms.mappings.Mappings
import play.api.data.{Form, Forms}

class AddEstablisherFormProvider extends Mappings {

  def apply(establishers: Seq[?]): Form[Option[Boolean]] = {
    if (establishers.isEmpty) {
      Form(
        "value" -> Forms.optional(boolean("messages__addEstablisher_error__selection"))
      )
    } else {
      Form(
        "value" -> Forms.optional(boolean("messages__addEstablisher_error__selection"))
          .verifying("messages__addEstablisher_error__selection", _.isDefined)
      )
    }
  }
}
