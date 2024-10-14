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

package services

import com.google.inject.Inject
import config.AppConfig
import models.Items
import models.requests.BulkDataRequest
import play.api.data.Form
import play.api.i18n.Messages
import play.api.mvc.Results._
import play.api.mvc.{AnyContent, Result}
import uk.gov.hmrc.govukfrontend.views.Aliases.{Table, Text}
import uk.gov.hmrc.govukfrontend.views.viewmodels.table.{HeadCell, TableRow}
import uk.gov.hmrc.nunjucks.NunjucksSupport
import uk.gov.hmrc.viewmodels.MessageInterpolators
import utils.TwirlMigration
import views.html.racdac.RacDacsBulkListView

import java.time.LocalDate
import java.time.format.DateTimeFormatter

class BulkRacDacService @Inject()(appConfig: AppConfig,
                                  paginationService: PaginationService,
                                  view: RacDacsBulkListView) {

  private def pagination: Int = appConfig.listSchemePagination

  def mapToTable(schemeDetails: List[Items])(implicit messages: Messages): Table = {
    val head = Seq(
      HeadCell(Text(msg"messages__listSchemes__column_racDacName".resolve)),
      HeadCell(Text(msg"messages__listSchemes__column_pstr".resolve)),
      HeadCell(Text(msg"messages__listSchemes__column_regDate".resolve))
    )

    val formatter: String => String = date => LocalDate.parse(date).format(DateTimeFormatter.ofPattern("d MMMM yyyy"))

    val rows = schemeDetails.map { data =>
      Seq(TableRow(Text(data.schemeName), classes = "govuk-!-width-one-half"),
        TableRow(Text(data.pstr), classes = "govuk-!-width-one-quarter"),
        TableRow(Text(formatter(data.schemeOpenDate)), classes = "govuk-!-width-one-quarter"))
    }
    Table(rows, Some(head), attributes = Map("role" -> "table"))
  }

  def renderRacDacBulkView(
                            form: Form[Boolean],
                            pageNumber: Int
                          )(implicit request: BulkDataRequest[AnyContent],
                            messages: Messages): Result = {

    val numberOfSchemes: Int = request.lisOfSchemes.length
    val numberOfPages: Int = paginationService.divide(numberOfSchemes, pagination)
    val schemeDetails = paginationService.selectPageOfResults(request.lisOfSchemes, pageNumber, numberOfPages)

    request.md match {
      case md if md.deceasedFlag => Redirect(appConfig.deceasedContactHmrcUrl)
      case md if md.rlsFlag => Redirect(appConfig.psaUpdateContactDetailsUrl)
      case md =>
        val viewHtml = view(
          form,
          controllers.racdac.bulk.routes.BulkListController.onSubmit,
          mapToTable(schemeDetails),
          numberOfSchemes,
          pagination,
          paginationService.paginationText(pageNumber, pagination, numberOfSchemes, numberOfPages),
          pageNumber,
          numberOfPages,
          paginationService.pageNumberLinks(
            pageNumber,
            numberOfSchemes,
            pagination,
            numberOfPages
          ),
          md.name,
          appConfig.psaOverviewUrl,
          utils.Radios.yesNo(form("value"))
        )
        if(form.hasErrors) BadRequest(viewHtml) else Ok(viewHtml)
    }
  }
}
