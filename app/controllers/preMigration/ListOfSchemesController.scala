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

package controllers.preMigration

import com.google.inject.Inject
import config.AppConfig
import connectors.{DelimitedAdminException, MinimalDetailsConnector}
import controllers.actions.{AuthAction, DataRetrievalAction}
import forms.ListSchemesFormProvider
import models.requests.OptionalDataRequest
import models.{Index, Items}
import play.api.data.Form
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import renderer.Renderer
import services.{PaginationService, SchemeSearchService}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.nunjucks.NunjucksSupport
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import scala.concurrent.{ExecutionContext, Future}

class ListOfSchemesController @Inject()(
                                       val appConfig: AppConfig,
                                       override val messagesApi: MessagesApi,
                                       authenticate: AuthAction,
                                       getData: DataRetrievalAction,
                                       minimalDetailsConnector: MinimalDetailsConnector,
                                       val controllerComponents: MessagesControllerComponents,
                                       paginationService: PaginationService,
                                       formProvider: ListSchemesFormProvider,
                                       schemeSearchService: SchemeSearchService,
                                       renderer: Renderer
                                     )(implicit val ec: ExecutionContext)
  extends FrontendBaseController
    with I18nSupport with NunjucksSupport {

  private val pagination: Int = appConfig.listSchemePagination

  private val form: Form[String] = formProvider()
  private val msgPrefix:String ="messages__schemesOverview__pagination__"
  private def renderView(
                          schemeDetails: List[Items],
                          numberOfSchemes: Int,
                          pageNumber: Int,
                          numberOfPages: Int,
                          noResultsMessageKey: Option[String],
                          form: Form[String]
                        )(implicit hc: HeaderCarrier,
                          request: OptionalDataRequest[AnyContent]): Future[Result] =
    minimalDetailsConnector.getPSADetails(request.psaId.id).flatMap {
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
            "noResultsMessageKey" -> noResultsMessageKey,
            "clearLinkUrl" -> controllers.preMigration.routes.ListOfSchemesController.onPageLoad().url,
            "schemesCount" -> schemeDetails.size,"schemesCount" -> schemeDetails.size,
            "returnUrl" -> appConfig.psaOverviewUrl.url,
            "paginationText" ->paginationText(pageNumber,pagination,numberOfSchemes,numberOfPages),

        ) ++  (if (schemeDetails.nonEmpty) Json.obj("schemes" -> schemeSearchService.mapToTable(schemeDetails)) else Json.obj())

      renderer.render("preMigration/listOfSchemes.njk", json)
        .map(body => if (form.hasErrors) BadRequest(body) else Ok(body))

    } recoverWith {
      case _: DelimitedAdminException =>
        Future.successful(Redirect(appConfig.psaDelimitedUrl))
    }

  private  def paginationText(pageNumber:Int,pagination:Int,numberOfSchemes:Int,numberOfPages:Int)(implicit messages: Messages):String={
    messages(
      s"${msgPrefix}text",
      if (pageNumber == 1) pageNumber else ((pageNumber * pagination) - pagination) + 1,
      if (pageNumber == numberOfPages) numberOfSchemes else pageNumber * pagination,
      numberOfSchemes
    )
  }
  private def searchAndRenderView(
                                   form: Form[String],
                                   pageNumber: Int,
                                   searchText: Option[String]
                                 )(implicit request: OptionalDataRequest[AnyContent]): Future[Result] = {
    schemeSearchService.search(request.psaId.id, searchText).flatMap { searchResult =>

      val noResultsMessageKey =
        (searchText.isDefined, searchResult.isEmpty) match {
          case (true, true) =>
            Some("messages__listSchemes__search_noMatches")
          case (false, true) => Some("messages__listSchemes__noSchemes")
          case _ => None
        }

      val numberOfSchemes: Int = searchResult.length

      val numberOfPages: Int =
        paginationService.divide(numberOfSchemes, pagination)

      renderView(
        schemeDetails = selectPageOfResults(searchResult, pageNumber, numberOfPages),
        numberOfSchemes = numberOfSchemes,
        pageNumber = pageNumber,
        numberOfPages = numberOfPages,
        noResultsMessageKey = noResultsMessageKey,
        form = form
      )
    }
  }

  private def selectPageOfResults(
                                   searchResult: List[Items],
                                   pageNumber: Int,
                                   numberOfPages: Int
                                 ): List[Items] = {
    pageNumber match {
      case 1 => searchResult.take(pagination)
      case p if p <= numberOfPages =>

          searchResult.slice(
            (pageNumber * pagination) - pagination,
            pageNumber * pagination
          )

      case _ => throw new Exception
    }
  }

  def onPageLoad: Action[AnyContent] = (authenticate andThen getData).async {
    implicit request =>
      searchAndRenderView(searchText = None, pageNumber = 1, form = form)
  }

  def onPageLoadWithPageNumber(pageNumber: Index): Action[AnyContent] =
    (authenticate andThen getData).async { implicit request =>
      searchAndRenderView(
        searchText = None,
        pageNumber = pageNumber,
        form = form
      )
    }

  def onSearch: Action[AnyContent] = (authenticate andThen getData).async {
    implicit request =>
      form
        .bindFromRequest()
        .fold(
          (formWithErrors: Form[String]) =>
            searchAndRenderView(
              searchText = None,
              pageNumber = 1,
              form = formWithErrors
            ),
          value => {
            searchAndRenderView(
              searchText = Some(value),
              pageNumber = 1,
              form = form.fill(value)
            )
          }
        )
  }

 // case object UnknownPageNumberException extends Exception("User has tried to select an invalid search result page number by hanging url")
}


