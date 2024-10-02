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
import connectors.{DelimitedAdminException, ListOfSchemesConnector, MinimalDetailsConnector}
import controllers.preMigration.routes
import controllers.preMigration.routes.ListOfSchemesController
import models.MigrationType.isRacDac
import models.requests.AuthenticatedRequest
import models.{Items, MigrationType}
import play.api.data.Form
import play.api.i18n.Messages
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.Results._
import play.api.mvc.{AnyContent, Result}
import uk.gov.hmrc.govukfrontend.views.viewmodels.button.Button
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.{HtmlContent, Text}
import uk.gov.hmrc.govukfrontend.views.viewmodels.errormessage.ErrorMessage
import uk.gov.hmrc.govukfrontend.views.viewmodels.hint.Hint
import uk.gov.hmrc.govukfrontend.views.viewmodels.label.Label
import uk.gov.hmrc.govukfrontend.views.viewmodels.table.{HeadCell, Table, TableRow}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.nunjucks.NunjucksSupport
//import uk.gov.hmrc.viewmodels.Table.Cell
import uk.gov.hmrc.viewmodels.Html
import utils.HttpResponseRedirects.listOfSchemesRedirects
import utils.{HttpResponseHelper, SchemeFuzzyMatcher}
import views.html.preMigration.ListOfSchemesView

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import scala.concurrent.{ExecutionContext, Future}

class SchemeSearchService @Inject()(appConfig: AppConfig,
                                    fuzzyMatching: SchemeFuzzyMatcher,
                                    listOfSchemesConnector: ListOfSchemesConnector,
                                    minimalDetailsConnector: MinimalDetailsConnector,
                                    paginationService: PaginationService,
                                    view: ListOfSchemesView) extends NunjucksSupport with HttpResponseHelper {

  private def pagination: Int = appConfig.listSchemePagination

  private val pstrRegex = "^[0-9]{8}[A-Za-z]{2}$".r

  private val filterSchemesByPstrOrSchemeName: (String, List[Items]) => List[Items] =
    (searchText, list) =>
      searchText match {
        case _ if pstrRegex.findFirstIn(searchText).isDefined =>
          list.filter(_.pstr.equalsIgnoreCase(searchText))
        case _ =>
          list.flatMap { schemeDetail =>
            val isMatch = fuzzyMatching.doFuzzyMatching(searchText, schemeDetail.schemeName)
            if (isMatch) Some(schemeDetail) else None
          }
      }

  def search(psaId: String, searchText: Option[String], isRacDac: Boolean)
            (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[List[Items]] =
    listOfSchemesConnector.getListOfSchemes(psaId).map {
      case Right(listOfSchemes) =>

        val filterSearchResults =
          searchText.fold[List[Items] => List[Items]](identity)(
            st => filterSchemesByPstrOrSchemeName(st, _: List[Items])
          )
        filterSearchResults(listOfSchemes.items.getOrElse(Nil).filter(_.racDac == isRacDac))
      case _ =>
        List.empty[Items]
    }


  def mapToTable(schemeDetails: List[Items], isRacDac: Boolean)(implicit messages: Messages): Option[Table] = {
    val head =
      if(isRacDac)
        Seq(
          HeadCell(HtmlContent(Messages("messages__listSchemes__column_racDacName"))),
          HeadCell(HtmlContent(Messages("messages__listSchemes__column_pstr"))),
          HeadCell(HtmlContent(Messages("messages__listSchemes__column_regDate")))
        )
      else
        Seq(
          HeadCell(HtmlContent(Messages("messages__listSchemes__column_schemeName"))),
          HeadCell(HtmlContent(Messages("messages__listSchemes__column_pstr"))),
          HeadCell(HtmlContent(Messages("messages__listSchemes__column_regDate")))
        )

    val formatter: String => String = date => LocalDate.parse(date).format(DateTimeFormatter.ofPattern("d MMMM yyyy"))

    val schemeName: Items => HtmlContent = data => HtmlContent(
      s"""<a class="govuk-link migrate-pstr-${data.pstr}" href=${ListOfSchemesController.clickSchemeLink(data.pstr, isRacDac)}>${data.schemeName}</a>""".stripMargin)

    val rows = schemeDetails.map { data =>
      Seq(
        TableRow(schemeName(data), classes = "govuk-!-width-one-half"),
        TableRow(Text(data.pstr), classes = "govuk-!-width-one-quarter"),
        TableRow(Text(formatter(data.schemeOpenDate)), classes ="govuk-!-width-one-quarter")
        )
    }
    Some(Table(rows, Some(head), attributes = Map("role" -> "table")))
  }

  def typeOfList(migrationType: MigrationType)(implicit messages: Messages): String =
    if (isRacDac(migrationType)) messages("messages__racdac") else messages("messages__pension_scheme")

  //noinspection ScalaStyle
  private def renderView(
                          schemeDetails: List[Items],
                          numberOfSchemes: Int,
                          pageNumber: Int,
                          numberOfPages: Int,
                          noResultsMessageKey: Option[String],
                          form: Form[String],
                          migrationType: MigrationType
                        )(implicit hc: HeaderCarrier,
                          messages: Messages,
                          request: AuthenticatedRequest[AnyContent],
                          ec: ExecutionContext): Future[Result] =
    minimalDetailsConnector.getPSADetails(request.psaId.id).flatMap {
      case md if md.deceasedFlag => Future.successful(Redirect(appConfig.deceasedContactHmrcUrl))
      case md if md.rlsFlag => Future.successful(Redirect(appConfig.psaUpdateContactDetailsUrl))
      case md =>
        val listType = typeOfList(migrationType)
        val racDac = isRacDac(migrationType)

        // TODO - check this works as expected
        val heading = form.value match {
          case Some(_) => messages("messages__listSchemes__search_result_title", listType)
          case None => messages("messages__listSchemes__add_title", listType)
        }

        val listRacDacUrl = "/add-pension-scheme/rac-dac/list-rac-dacs/page/"
        val listUrl = "/add-pension-scheme/list-pension-schemes/page/"

        val listSchemeUrl = if (racDac) {
          listRacDacUrl
        } else {
          listUrl
        }

        val schemes: Option[Table] = mapToTable(schemeDetails, isRacDac(migrationType))


        val pageNumberLinks = paginationService.pageNumberLinks(
          pageNumber,
          numberOfSchemes,
          pagination,
          numberOfPages
        )

        val formErrorClass = if (form.hasErrors) {
          "govuk-form-group--error"
        } else {
          ""
        }

        val inputErrorClass = if (form.hasErrors) {
          "govuk-input--error"
        } else {
          ""
        }

        val errorMessages: Option[Seq[ErrorMessage]] = if(form.hasErrors) {
          val messages = form.errors.map((err) => {
            ErrorMessage(
              id = Some(s"value-error-${err.key}"),
              content = Text(err.message)
            )
          })
          Some(messages)
        } else {
          None
        }

        val buttonContent = form.value match {
          case Some(_) => messages("messages__listSchemes__search_again")
          case _ => messages("messages__listSchemes__search_submit")
        }


        val searchButton = Button(
          content = Text(buttonContent),
          attributes = Map("id" -> "search"),
          classes = "govuk-!-margin-bottom-3"
        )

        Future.successful(Ok(view(
          heading = heading,
          form = form,
          submitCall = routes.ListOfSchemesController.onSearch(migrationType),
          formErrorClass = formErrorClass,
          errorMessages = errorMessages,
          inputErrorClass = inputErrorClass,
          searchButton = searchButton,
          schemes = schemes,
          clearLinkUrl = routes.ListOfSchemesController.onPageLoad(migrationType).url,
          numberOfSchemes = numberOfSchemes,
          noResultsMessageKey = noResultsMessageKey,
          pagination = pagination,
          paginationText = paginationService.paginationText(pageNumber, pagination, numberOfSchemes, numberOfPages),
          pageNumber = pageNumber,
          pageNumberLinks = pageNumberLinks,
          returnUrl = appConfig.psaOverviewUrl,
          psaName = md.name,
          racDac = isRacDac(migrationType),
          listSchemeUrl = listSchemeUrl,
          numberOfPages = numberOfPages,
        )))

    } recoverWith {
      case _: DelimitedAdminException =>
        Future.successful(Redirect(appConfig.psaDelimitedUrl))
    }

  private def noResultsMessageKey(searchText: Option[String], searchResult: List[Items], migrationType: MigrationType)
                                 (implicit messages: Messages): Option[String] =
    (searchText.isDefined, searchResult.isEmpty) match {
      case (true, true) =>
        Some(messages("messages__listSchemes__search_noMatches", typeOfList(migrationType)))
      case (false, true) => Some(messages("messages__listSchemes__noSchemes", typeOfList(migrationType)))
      case _ => None
    }

  def searchAndRenderView(
                           form: Form[String],
                           pageNumber: Int,
                           searchText: Option[String],
                           migrationType: MigrationType
                         )(implicit request: AuthenticatedRequest[AnyContent],
                           messages: Messages,
                           hc: HeaderCarrier,
                           ec: ExecutionContext): Future[Result] =
      search(request.psaId.id, searchText, isRacDac(migrationType)).flatMap { searchResult =>
        val numberOfSchemes: Int = searchResult.length

        val numberOfPages: Int =
          paginationService.divide(numberOfSchemes, pagination)

        renderView(
          paginationService.selectPageOfResults(searchResult, pageNumber, numberOfPages),
          numberOfSchemes,
          pageNumber,
          numberOfPages,
          noResultsMessageKey(searchText, searchResult, migrationType),
          form,
          migrationType
        )
      } recoverWith listOfSchemesRedirects
}
