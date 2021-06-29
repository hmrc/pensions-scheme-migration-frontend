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

package viewmodels

import play.api.data.Field
import play.api.i18n.Messages
import play.api.libs.json.{OWrites, Json}
import uk.gov.hmrc.viewmodels.{Text, NunjucksSupport}

object DateInput extends NunjucksSupport {

  final case class ViewModel(items: Seq[Item], error: Option[Text])
  final case class Item(label: Text, name: String, id: String, value: String, classes: String)

  object ViewModel {
    implicit def writes(implicit messages: Messages): OWrites[ViewModel] =
      OWrites { viewmodel =>
        Json.obj(
          "items" -> viewmodel.items,
          "error" -> viewmodel.error.map { error =>
            Json.obj(
              "text" -> error
            )
          }
        )
      }
  }

  object Item {
    implicit def writes(implicit messages: Messages): OWrites[Item] = Json.writes[Item]
  }

  def localDate(field: Field): ViewModel = {

    val error = (field.error orElse field("day").error orElse field("month").error orElse field("year").error)
      .map(formError => Text.Message(formError.message, formError.args: _*))

    def classes(classes: String*): String = {
      val allClasses = if (error.isDefined) "govuk-input--error" :: classes.toList else classes.toList
      allClasses.mkString(" ")
    }

    val items = Seq(
      Item(
        label = msg"site.day.capitalized",
        name = field("day").name,
        id = field("day").id,
        value = field("day").value.getOrElse(""),
        classes = classes("govuk-input--width-2")
      ),
      Item(
        label = msg"site.month.capitalized",
        name = field("month").name,
        id = field("month").id,
        value = field("month").value.getOrElse(""),
        classes = classes("govuk-input--width-2")
      ),
      Item(
        label = msg"site.year.capitalized",
        name = field("year").name,
        id = field("year").id,
        value = field("year").value.getOrElse(""),
        classes = classes("govuk-input--width-4")
      )
    )

    ViewModel(items, error)
  }
}
