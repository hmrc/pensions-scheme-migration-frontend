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

package helpers

import identifiers.establishers.individual.EstablisherNameId
import models.Entity
import play.api.i18n.Messages
import uk.gov.hmrc.viewmodels.Table.Cell
import uk.gov.hmrc.viewmodels.Text.Literal
import uk.gov.hmrc.viewmodels.{Html, MessageInterpolators, Table}

class AddToListHelper {

  def mapEstablishersToTable[A <: Entity[_]](establishers: Seq[A])
                                            (implicit messages: Messages): Table = mapToTable(establishers, establishersHead(establishers))
  private def establishersHead[A <: Entity[_]](establishers: Seq[A])(implicit messages: Messages): Seq[Cell] = {

    val linkHeader = if(establishers.size > 1){
      Seq(Cell(Html(s"""<span class=govuk-visually-hidden>${messages("addEstablisher.hiddenText.removeLink.header")}</span>""")))
    }
      else Nil
    Seq(
      Cell(msg"addEstablisher.name.header"),
      Cell(msg"addEstablisher.type.header")
    ) ++linkHeader

  }

  private def mapToTable[A <: Entity[_]](entities: Seq[A], head: Seq[Cell])
                            (implicit messages: Messages): Table = {

    val rows = entities.map { data =>
      Seq(
        Cell(Literal(data.name), classes = Seq("govuk-!-width-one-half")),
        Cell(Literal(getTypeFromId(data.id)), classes = Seq("govuk-!-width-one-quarter"))) ++
        data.deleteLink.fold[Seq[Cell]](Nil)(delLink  =>
          Seq(Cell(link(s"remove-${data.index}", "site.remove", delLink, data.name), classes = Seq("govuk-!-width-one-quarter"))))
    }

    Table(head = head, rows = rows, attributes = Map("role" -> "table"))
  }

  def link(id: String, text: String, url: String, name: String)(implicit messages: Messages): Html = {
    val hiddenTag = "govuk-visually-hidden"
    Html(
      s"<a class=govuk-link id=$id href=$url>" + s"<span aria-hidden=true >${messages(text)}</span>" +
        s"<span class= $hiddenTag>${messages(text)} $name</span> </a>")
  }

  private def getTypeFromId[ID](id: ID)(implicit messages: Messages): String =
    id match {
      case EstablisherNameId(_) => messages("kind.individual")
      case _ => messages("kind.company")
    }

}
