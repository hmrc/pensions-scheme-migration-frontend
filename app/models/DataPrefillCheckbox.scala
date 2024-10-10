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
import uk.gov.hmrc.govukfrontend.views.Aliases.{Label, Text}
import uk.gov.hmrc.govukfrontend.views.viewmodels.checkboxes.{CheckboxItem, ExclusiveCheckbox}
import uk.gov.hmrc.viewmodels.MessageInterpolators

object DataPrefillCheckbox {

  def checkboxes(form: Form[_], values: Seq[DataPrefillIndividualDetails])(implicit messages: Messages): Seq[CheckboxItem] = {
    val checkBoxes = values.zipWithIndex.map { case (details, index) =>
      CheckboxItem(
        content = Text(details.fullName),
        label = Some(Label(content = Text(details.fullName))),
        value = index.toString,
        name = Some(s"value[$index]"),
        checked = form("value").value.map(_ == details.fullName).getOrElse(false)
      )
    }

    val noneOfTheAbove = CheckboxItem(
      content = Text(msg"messages__prefill__label__none".resolve),
      label = Some(Label(content = Text(msg"messages__prefill__label__none".resolve))),
      value = "-1",
      name = Some(s"value[${checkBoxes.length}]"),
      checked = form("value").value.map(_ == "-1").getOrElse(false),
      behaviour = Some(ExclusiveCheckbox)
    )

    checkBoxes :+ CheckboxItem(divider = Some("or")) :+ noneOfTheAbove
  }
}