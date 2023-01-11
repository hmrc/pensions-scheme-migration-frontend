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

package services

import com.google.inject.Inject
import config.AppConfig
import models.Items
import models.requests.BulkDataRequest
import play.api.data.Form
import play.api.i18n.Messages
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.Results._
import play.api.mvc.{AnyContent, Result}
import renderer.Renderer
import uk.gov.hmrc.nunjucks.NunjucksSupport
import uk.gov.hmrc.viewmodels.Table.Cell
import uk.gov.hmrc.viewmodels.Text.Literal
import uk.gov.hmrc.viewmodels.{MessageInterpolators, Radios, Table}

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import scala.concurrent.{ExecutionContext, Future}

class BulkRacDacService @Inject()(appConfig: AppConfig,
                                  paginationService: PaginationService,
                                  renderer: Renderer) extends NunjucksSupport {

  private def pagination: Int = appConfig.listSchemePagination

  def mapToTable(schemeDetails: List[Items]): Table = {
    val head = Seq(
      Cell(msg"messages__listSchemes__column_racDacName"),
      Cell(msg"messages__listSchemes__column_pstr"),
      Cell(msg"messages__listSchemes__column_regDate")
    )

    val formatter: String => String = date => LocalDate.parse(date).format(DateTimeFormatter.ofPattern("d MMMM yyyy"))

    val rows = schemeDetails.map { data =>
      Seq(Cell(Literal(data.schemeName), Seq("govuk-!-width-one-quarter")),
        Cell(Literal(data.pstr), Seq("govuk-!-width-one-quarter")),
        Cell(Literal(formatter(data.schemeOpenDate)), Seq("govuk-!-width-one-half")))
    }

    Table(head, rows, attributes = Map("role" -> "table"))
  }

  def renderRacDacBulkView(
                            form: Form[Boolean],
                            pageNumber: Int
                          )(implicit request: BulkDataRequest[AnyContent],
                            messages: Messages,
                            ec: ExecutionContext): Future[Result] = {

    val numberOfSchemes: Int = request.lisOfSchemes.length
    val numberOfPages: Int = paginationService.divide(numberOfSchemes, pagination)
    val schemeDetails = paginationService.selectPageOfResults(request.lisOfSchemes, pageNumber, numberOfPages)

    request.md match {
      case md if md.deceasedFlag => Future.successful(Redirect(appConfig.deceasedContactHmrcUrl))
      case md if md.rlsFlag => Future.successful(Redirect(appConfig.psaUpdateContactDetailsUrl))
      case md =>

        val json: JsObject = Json.obj(
          "form" -> form,
          "psaName" -> md.name,
          "numberOfSchemes" -> numberOfSchemes,
          "pagination" -> pagination,
          "pageNumber" -> pageNumber,
          "pageNumberLinks" -> paginationService.pageNumberLinks(
            pageNumber,
            numberOfSchemes,
            pagination,
            numberOfPages
          ),
          "numberOfPages" -> numberOfPages,
          "returnUrl" -> appConfig.psaOverviewUrl,
          "paginationText" -> paginationService.paginationText(pageNumber, pagination, numberOfSchemes, numberOfPages),
          "schemes" -> mapToTable(schemeDetails),
          "radios" -> Radios.yesNo(form("value"))
        )
        renderer.render("racdac/racDacsBulkList.njk", json)
          .map(body => if (form.hasErrors) BadRequest(body) else Ok(body))
    }
  } recoverWith {
    case e: IllegalArgumentException =>
      Future.successful(Redirect(controllers.routes.NotFoundController.onPageLoad))
  }
}
