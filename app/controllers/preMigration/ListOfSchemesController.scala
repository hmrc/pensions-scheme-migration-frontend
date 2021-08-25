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
import connectors.{AncillaryPsaException, DelimitedAdminException, MinimalDetailsConnector}
import controllers.actions.AuthAction
import forms.ListSchemesFormProvider
import models.MigrationType.isRacDac
import models.requests.AuthenticatedRequest
import models.{Index, Items, MigrationType}
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

  private def form(migrationType: MigrationType)
                  (implicit messages: Messages): Form[String] = formProvider(messages("messages__listSchemes__search_required", typeOfList(migrationType)))

  private val msgPrefix:String ="messages__listSchemes__pagination__"
  private def typeOfList(migrationType: MigrationType)(implicit messages: Messages):String =
    if(isRacDac(migrationType)) messages("messages__racdac") else messages("messages__pension_scheme")

  private def renderView(
                          schemeDetails: List[Items],
                          numberOfSchemes: Int,
                          pageNumber: Int,
                          numberOfPages: Int,
                          noResultsMessageKey: Option[String],
                          form: Form[String],
                          migrationType: MigrationType
                        )(implicit hc: HeaderCarrier,
                          request: AuthenticatedRequest[AnyContent]): Future[Result] =
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
            "racDac" -> isRacDac(migrationType),
            "numberOfPages" -> numberOfPages,
            "noResultsMessageKey" -> noResultsMessageKey,
            "clearLinkUrl" -> routes.ListOfSchemesController.onPageLoad(migrationType).url,
            "returnUrl" -> appConfig.psaOverviewUrl,
            "paginationText" -> paginationText(pageNumber,pagination,numberOfSchemes,numberOfPages),
            "typeOfList" -> typeOfList(migrationType)
        ) ++  (if (schemeDetails.nonEmpty) Json.obj("schemes" -> schemeSearchService.mapToTable(schemeDetails, isRacDac(migrationType))) else Json.obj())

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

  private def noResultsMessageKey(searchText: Option[String], searchResult: List[Items], migrationType: MigrationType)
                                 (implicit messages: Messages): Option[String] =
    (searchText.isDefined, searchResult.isEmpty) match {
      case (true, true) =>
        Some(messages("messages__listSchemes__search_noMatches", typeOfList(migrationType)))
      case (false, true) => Some(messages("messages__listSchemes__noSchemes", typeOfList(migrationType)))
      case _ => None
    }

  private def searchAndRenderView(
                                   form: Form[String],
                                   pageNumber: Int,
                                   searchText: Option[String],
                                   migrationType: MigrationType
                                 )(implicit request: AuthenticatedRequest[AnyContent]): Future[Result] = {
    schemeSearchService.search(request.psaId.id, searchText, isRacDac(migrationType)).flatMap { searchResult =>

      val numberOfSchemes: Int = searchResult.length

      val numberOfPages: Int =
        paginationService.divide(numberOfSchemes, pagination)

      renderView(
        selectPageOfResults(searchResult, pageNumber, numberOfPages),
        numberOfSchemes,
        pageNumber,
        numberOfPages,
        noResultsMessageKey(searchText, searchResult, migrationType),
        form,
        migrationType
      )
    } recoverWith {
      case _: AncillaryPsaException =>
        Future.successful(Redirect(routes.CannotMigrateController.onPageLoad()))
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

  def onPageLoad(migrationType: MigrationType): Action[AnyContent] = (authenticate).async {
    implicit request =>
      searchAndRenderView(form(migrationType), pageNumber = 1, searchText = None, migrationType)
  }

  def onPageLoadWithPageNumber(pageNumber: Index, migrationType: MigrationType): Action[AnyContent] =
    (authenticate).async { implicit request =>
      searchAndRenderView(form(migrationType), pageNumber, searchText = None, migrationType)
    }

  def onSearch(migrationType: MigrationType): Action[AnyContent] = (authenticate).async {
    implicit request =>
      search(migrationType)
  }

  def onSearchWithPageNumber(pageNumber: Index, migrationType: MigrationType): Action[AnyContent] = (authenticate).async {
    implicit request =>
      search(migrationType)
  }

  private def search(migrationType: MigrationType)(implicit request: AuthenticatedRequest[AnyContent]): Future[Result] = {
    form(migrationType)
      .bindFromRequest()
      .fold(
        (formWithErrors: Form[String]) =>
          searchAndRenderView(formWithErrors, pageNumber = 1, searchText = None, migrationType),
        value =>
          searchAndRenderView(form(migrationType).fill(value), pageNumber = 1, searchText = Some(value), migrationType)
      )
  }



}


