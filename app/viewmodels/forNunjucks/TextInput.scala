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

package viewmodels.forNunjucks

import play.api.i18n.Messages
import play.api.libs.functional.syntax._
import play.api.libs.json.{OWrites, __}
import uk.gov.hmrc.viewmodels.Text

case class Label(text: Text, classes: Seq[String] = Seq.empty, isPageHeading: Boolean = false)

object Label {
  implicit def writes(implicit messages: Messages): OWrites[Label] = (
    (__ \ "text").write[Text] and
      (__ \ "classes").writeNullable[String] and
        (__ \ "isPageHeading").write[Boolean]
    ) { label =>
    (label.text, classes(label.classes), label.isPageHeading)
  }

  private def classes(classes: Seq[String]): Option[String] =
    if (classes.isEmpty) None else Some(classes.mkString(" "))
}

final case class TextInput(
                          id: String,
                          name: String,
                          label: Label)

object TextInput {
    implicit def writes(implicit messages: Messages): OWrites[TextInput] = (
      (__ \ "id").write[String] and
        (__ \ "name").write[String] and
        (__ \ "label").write[Label]
      ) { textInput =>
      (textInput.id, textInput.name, textInput.label)
    }
}
