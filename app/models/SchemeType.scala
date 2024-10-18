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

import play.api.data.Form
import play.api.i18n.Messages
import play.api.libs.json._
import play.twirl.api.Html
import uk.gov.hmrc.govukfrontend.views.Aliases.Label
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.Text
import uk.gov.hmrc.govukfrontend.views.viewmodels.hint.Hint
import uk.gov.hmrc.govukfrontend.views.viewmodels.radios.RadioItem
import utils.WithName

sealed trait SchemeType

object SchemeType {
  val values: Seq[WithName with SchemeType] =
    Seq(
      SingleTrust,
      GroupLifeDeath,
      BodyCorporate
    )

  val mappings: Map[String, SchemeType] =
    values.map(v => (v.toString, v)).toMap

  def radios(form: Form[_])(implicit messages: Messages): Seq[RadioItem] = {
    val field = form("schemeType.type")
    val items: Seq[RadioItem] = values.map(
      value =>
      RadioItem(
        label = Some(Label(
          classes = "govuk-!-font-weight-bold",
          content = Text(Messages(s"messages__scheme_type_${value.toString}"))
        )),
        value = Some(value.toString),
        hint = Some(Hint(
          content = Text(Messages(s"messages__scheme_type_${value.toString}_hint")),
          id = Some("hint-id")
        )),
        checked = field.value.contains(value.toString)
      )
    )

    val inputHtml: Messages => Html = {
      val id = "schemeType.schemeTypeDetails"
      val value = form("schemeType.schemeTypeDetails").value.getOrElse("")

      messages =>
        Html(
          s"<div class='govuk-form-group'>" +
            s"<label class='govuk-label govuk-label--s' for='${id.replace(".", "_")}'>${Messages("messages__scheme_details__type_other_more")(messages)}</label>" +
            s"<input class='govuk-input govuk-!-width-one-third' id='${id.replace(".", "_")}' name='schemeType.schemeTypeDetails' value='$value' type='text'>" +
            s"</div>"
        )
    }

    val otherItem = {
      RadioItem(
        label = Some(Label(
          classes = "govuk-!-font-weight-bold",
          content = Text(Messages("messages__scheme_type_other"))
        )),
        value = Some(Other.toString.toLowerCase),
        hint = Some(Hint(
          content = Text(Messages("messages__scheme_type_other_hint")),
          id = Some("hint-id")
        )),
        conditionalHtml = Some(inputHtml(messages)),
        checked = field.value.contains(Other.toString.toLowerCase)
      )
    }

    items :+ otherItem
  }

  case class Other(schemeTypeDetails: String) extends WithName("other") with SchemeType

  case object SingleTrust extends WithName("single") with SchemeType

  case object GroupLifeDeath extends WithName("group") with SchemeType

  case object BodyCorporate extends WithName("corp") with SchemeType

  implicit val reads: Reads[SchemeType] = {

    (JsPath \ "name").read[String].flatMap {

      case schemeTypeName if schemeTypeName == "other" =>
        (JsPath \ "schemeTypeDetails").read[String]
          .map[SchemeType](Other)
          .orElse(Reads[SchemeType](_ => JsError("Other Value expected")))

      case schemeTypeName if mappings.keySet.contains(schemeTypeName) =>
        Reads(_ => JsSuccess(mappings.apply(schemeTypeName)))

      case _ => Reads(_ => JsError("Invalid Scheme Type"))
    }
  }

  val optionalReads: Reads[SchemeType] = {

    (JsPath \ "name").read[String].flatMap {

      case schemeTypeName if schemeTypeName == "other" =>
        (JsPath \ "schemeTypeDetails").read[String]
          .map[SchemeType](Other)
          .orElse(Reads[SchemeType](_ => JsSuccess(Other(""))))

      case schemeTypeName if mappings.keySet.contains(schemeTypeName) =>
        Reads(_ => JsSuccess(mappings.apply(schemeTypeName)))

      case _ => Reads(_ => JsError("Invalid Scheme Type"))
    }
  }


  implicit lazy val writes: Writes[SchemeType] = new Writes[SchemeType] {
    def writes(o: SchemeType): JsObject = {
      o match {
        case SchemeType.Other(schemeTypeDetails) =>
          Json.obj("name" -> "other", "schemeTypeDetails" -> schemeTypeDetails)
        case s if mappings.keySet.contains(s.toString) =>
          Json.obj("name" -> s.toString)
        case s => throw new RuntimeException(s"Invalid match $s")
      }
    }
  }

}
