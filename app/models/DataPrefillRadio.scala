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

import play.api.data.Form
import uk.gov.hmrc.viewmodels.{MessageInterpolators, Radios}
import uk.gov.hmrc.viewmodels.Text.Literal

object DataPrefillRadio {

  def radios(form: Form[_], values: Seq[(String, Int)]): Seq[Radios.Item] = {
    val items = values.map(value => Radios.Radio(Literal(value._1), value._2.toString)) :+
      Radios.Radio(msg"messages__directors__prefill__label__none", "11")
    Radios(form("value"), items)
  }
}
