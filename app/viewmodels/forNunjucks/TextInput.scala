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

package viewmodels.forNunjucks

import play.api.i18n.Messages
import play.api.libs.functional.syntax._
import play.api.libs.json.{OWrites, __}
import uk.gov.hmrc.viewmodels.{Html, Text}

case class Label(
                  text: Text,
                  classes: Seq[String] = Seq.empty,
                  isPageHeading: Boolean = false
                )

object Label {
  implicit def writes(implicit messages: Messages): OWrites[Label] = (
    (__ \ "text").write[Text] and
      (__ \ "classes").writeNullable[String] and
      (__ \ "isPageHeading").write[Boolean]
    ) { label =>
    (label.text, TextInput.classes(label.classes), label.isPageHeading)
  }
}

final case class TextInput(
                            id: String,
                            value: String,
                            name: String,
                            label: Label,
                            classes: Seq[String] = Seq.empty
                          )

object TextInput {
  implicit def writes(implicit messages: Messages): OWrites[TextInput] = (
    (__ \ "id").write[String] and
      (__ \ "value").write[String] and
      (__ \ "name").write[String] and
      (__ \ "label").write[Label] and
      (__ \ "classes").writeNullable[String]
    ) { textInput =>
    (textInput.id, textInput.value, textInput.name, textInput.label, classes(textInput.classes))
  }

  def classes(classes: Seq[String]): Option[String] =
    if (classes.isEmpty) None else Some(classes.mkString(" "))

  val inputHtml: (TextInput, Messages) => Html =
    (input, messages) =>
      Html(
        s"<div class='govuk-form-group'>" +
          s"<label class='${input.label.classes.mkString}' for='${input.id.replace(".", "_")}'>${input.label.text.resolve(messages)}</label>" +
          s"<input class='govuk-input govuk-!-width-one-third' id='${input.id.replace(".", "_")}' name='${input.name}' value='${input.value}' type='text'>" +
          s"</div>"
      )
}
