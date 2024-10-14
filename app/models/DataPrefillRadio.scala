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

package models

import models.prefill.{IndividualDetails => DataPrefillIndividualDetails}
import play.api.data.Form
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.viewmodels.label.Label
import uk.gov.hmrc.govukfrontend.views.viewmodels.radios.RadioItem
import uk.gov.hmrc.viewmodels.Text.Literal
import uk.gov.hmrc.viewmodels.{MessageInterpolators, Radios}

object DataPrefillRadio {

  def radios(form: Form[_], values: Seq[DataPrefillIndividualDetails])(implicit messages: Messages): Seq[RadioItem] = {
    val noneValue = "-1"
    values.map { indvDetails => {
      RadioItem(
        label = Some(Label(Some(Literal(indvDetails.fullName).resolve))),
        value = Some(indvDetails.index.toString),
        checked = form("value").value.contains(indvDetails.index.toString)
      )
    }} :+ RadioItem(
      label = Some(Label(Some(Messages("messages__prefill__label__none")))),
      value = Some(noneValue),
      checked = form("value").value.contains(noneValue)
    )
  }
}
