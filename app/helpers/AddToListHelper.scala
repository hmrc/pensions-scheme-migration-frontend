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

import identifiers.establishers.company.CompanyDetailsId
import identifiers.establishers.individual.EstablisherNameId
import identifiers.establishers.partnership.PartnershipDetailsId
import identifiers.trustees.company.{CompanyDetailsId => TrusteeCompanyDetailsId}
import identifiers.trustees.individual.TrusteeNameId
import identifiers.trustees.partnership.{PartnershipDetailsId => TrusteePartnershipDetailsId}
import models.Entity
import play.api.i18n.Messages
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.viewmodels.Table.Cell
import uk.gov.hmrc.viewmodels.Text.Literal
import uk.gov.hmrc.viewmodels.{Html, Table}

class AddToListHelper {

  def mapEstablishersToTable[A <: Entity[_]](establishers: Seq[A], caption: String, editLinkText: String)
                                            (implicit messages: Messages): Table =
    mapToTable(establishers, caption, editLinkText, hideDeleteLink = false)

  def mapTrusteesToTable[A <: Entity[_]](trustees: Seq[A], caption: String, editLinkText: String, hideDeleteLink: Boolean = false)
                                        (implicit messages: Messages): Table =
    mapToTable(trustees, caption, editLinkText, hideDeleteLink)

  private def mapToTable[A <: Entity[_]](entities: Seq[A], caption: String, editLinkText: String, hideDeleteLink: Boolean)
                                        (implicit messages: Messages): Table = {

    val rows = entities.map { data =>
      Seq(Cell(Literal(data.name), Seq("govuk-!-width-one-quarter")),
        Cell(Literal(getTypeFromId(data.id)), Seq("govuk-!-width-one-quarter"))) ++
        data.editLink.fold[Seq[Cell]](Nil)(editLink =>
          Seq(Cell(link(s"edit-${data.index}", editLinkText, editLink, data.name), Seq("govuk-!-width-one-quarter")))
        ) ++
        (if(!hideDeleteLink) {
          data.deleteLink.fold[Seq[Cell]](Nil)(delLink =>
            Seq(Cell(link(s"remove-${data.index}", "site.remove", delLink, data.name), Seq("govuk-!-width-one-quarter")))
          )
        } else Nil)
    }

    Table(Nil, rows, caption = Some(Literal(caption)), attributes = Map("role" -> "table"))
  }

  def link(id: String, text: String, url: String, name: String)(implicit messages: Messages): Html = {
    Html(s"<a class=govuk-link id=$id href=$url><span aria-hidden=true >${messages(text)}</span>" +
      s"<span class=govuk-visually-hidden>${messages(text)} $name</span> </a>")
  }

  private def getTypeFromId[ID](id: ID)(implicit messages: Messages): String =
    id match {
      case EstablisherNameId(_) => messages("kind.individual")
      case TrusteeNameId(_) => messages("kind.individual")
      case CompanyDetailsId(_) => messages("kind.company")
      case TrusteeCompanyDetailsId(_) => messages("kind.company")
      case PartnershipDetailsId(_) => messages("kind.partnership")
      case TrusteePartnershipDetailsId(_) => messages("kind.partnership")
    }

  def mapDirectorOrPartnerToTable[A <: Entity[_]](directorsOrPartners: Seq[A])
                                        (implicit messages: Messages): Table =
    directorOrPartnerTable(
      entities = directorsOrPartners
    )

  private def directorOrPartnerTable[A <: Entity[_]](entities: Seq[A])
                                           (implicit messages: Messages): Table = {
    val rows = entities.map { data =>
      Seq(
        Cell(
          content = Literal(data.name),
          classes = Seq("govuk-!-width-one-half")
        )
      ) ++
        data.editLink.fold[Seq[Cell]](
          Nil
        )(
          editLink =>
            Seq(
              Cell(
                content = link(
                  id = s"change-${data.index}",
                  text = "site.change",
                  url = editLink, name = data.name
                ),
                classes = Seq("govuk-!-width-one-quarter")
              )
            )
        )++
        data.deleteLink.fold[Seq[Cell]](
          Nil
        )(
          delLink =>
            Seq(
              Cell(
                content = link(
                  id = s"remove-${data.index}",
                  text = "site.remove",
                  url = delLink, name = data.name
                ),
                classes = Seq("govuk-!-width-one-quarter")
              )
            )
        )
    }

    Table(
      ( Nil ),
      rows = rows,
      attributes = Map("role" -> "table")
    )
  }

   def directorsOrPartnersItemList[A <: Entity[_]](entities: Seq[A]): JsValue  = {

    Json.toJson(
      entities.map { data =>
        Map("name" -> data.name,
          "changeUrl" ->   data.editLink.getOrElse("#"),
          "removeUrl" ->   data.deleteLink.getOrElse("#")
        )
      }
    )
  }
}
