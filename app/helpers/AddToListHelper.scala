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
import identifiers.trustees.individual.TrusteeNameId
import identifiers.trustees.company.{CompanyDetailsId => TrusteeCompanyDetailsId}
import identifiers.trustees.partnership.{PartnershipDetailsId => TrusteePartnershipDetailsId}
import models.Entity
import play.api.i18n.Messages
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.viewmodels.Table.Cell
import uk.gov.hmrc.viewmodels.Text.Literal
import uk.gov.hmrc.viewmodels.{Html, MessageInterpolators, Table}

class AddToListHelper {

  def mapEstablishersToTable[A <: Entity[_]](establishers: Seq[A])
                                            (implicit messages: Messages): Table =
    mapToTable(establishers, establishersHead(establishers))

  private def establishersHead[A <: Entity[_]](establishers: Seq[A])(implicit messages: Messages): Seq[Cell] = {

    val linkHeader: Seq[Cell] =
      Seq(Cell(Html(s"""<span class=govuk-visually-hidden>${messages("addEstablisher.hiddenText.removeLink.header")}</span>""")))

    val typeHeader: Html =
      Html(s"""<span aria-hidden=true>${messages("addEstablisher.type.header")}</span>""" +
          s"""<span class=govuk-visually-hidden>${messages("addEstablisher.type.header.hiddenText")}</span>""")
    Seq(
      Cell(msg"addEstablisher.name.header"),
      Cell(typeHeader)
    ) ++ (if (establishers.size > 1) linkHeader else Nil)
  }

  def mapTrusteesToTable[A <: Entity[_]](trustees: Seq[A])
                                        (implicit messages: Messages): Table =
    mapToTable(trustees, trusteesHead(trustees))

  private def trusteesHead[A <: Entity[_]](trustees: Seq[A])
                                          (implicit messages: Messages): Seq[Cell] = {

    val linkHeader =
      Seq(Cell(Html(s"""<span class=govuk-visually-hidden>${messages("addTrustee.hiddenText.removeLink.header")}</span>""")))

    val typeHeader: Html =
      Html(s"""<span aria-hidden=true>${messages("addTrustee.type.header")}</span>""" +
          s"""<span class=govuk-visually-hidden>${messages("addTrustee.type.header.hiddenText")}</span>""")

    Seq(
      Cell(msg"addTrustee.name.header"),
      Cell(typeHeader)
    ) ++ (if (trustees.size > 1) linkHeader else Nil)
  }

  private def mapToTable[A <: Entity[_]](entities: Seq[A], head: Seq[Cell])
                                        (implicit messages: Messages): Table = {

    val rows = entities.map { data =>
      Seq(Cell(Literal(data.name), Seq("govuk-!-width-one-half")),
        Cell(Literal(getTypeFromId(data.id)), Seq("govuk-!-width-one-quarter"))) ++
        data.deleteLink.fold[Seq[Cell]](Nil)(delLink =>
            Seq(Cell(link(s"remove-${data.index}", "site.remove", delLink, data.name), Seq("govuk-!-width-one-quarter")))
        )
    }

    Table(head, rows, attributes = Map("role" -> "table"))
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

  def mapDirectorToTable[A <: Entity[_]](dirctors: Seq[A])
                                        (implicit messages: Messages): Table =
    directorTable(
      entities = dirctors
    )

  private def directorTable[A <: Entity[_]](entities: Seq[A])
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

   def directorsItemList[A <: Entity[_]](entities: Seq[A])
                                           (implicit messages: Messages): JsValue  = {

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
