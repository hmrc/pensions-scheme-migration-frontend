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

package controllers.preMigration

import com.google.inject.Inject
import config.AppConfig
import connectors.ListOfSchemesConnector
import controllers.actions.AuthAction
import forms.ListSchemesFormProvider
import models.MigrationType.isRacDac
import models.requests.AuthenticatedRequest
import models.{Index, MigrationType}
import play.api.data.Form
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.{LockingService, SchemeSearchService}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.HttpResponseRedirects.listOfSchemesRedirects

import scala.concurrent.{ExecutionContext, Future}

class ListOfSchemesController @Inject()(
                                         val appConfig: AppConfig,
                                         override val messagesApi: MessagesApi,
                                         authenticate: AuthAction,
                                         val controllerComponents: MessagesControllerComponents,
                                         formProvider: ListSchemesFormProvider,
                                         listOfSchemesConnector: ListOfSchemesConnector,
                                         schemeSearchService: SchemeSearchService,
                                         lockingService: LockingService
                                       )(implicit val ec: ExecutionContext)
  extends FrontendBaseController
    with I18nSupport {


  private def form(migrationType: MigrationType)
                  (implicit messages: Messages): Form[String] = formProvider(
    messages("messages__listSchemes__search_required",
      schemeSearchService.typeOfList(migrationType)))


  def onPageLoad(migrationType: MigrationType): Action[AnyContent] = authenticate.async {
    implicit request =>
      val checkRacDac: Boolean = isRacDac(migrationType)
      listOfSchemesConnector.getListOfSchemes(request.psaId.id).flatMap {
        case Right(list) =>
          if (list.items.exists(_.exists(_.racDac == checkRacDac))) {
            schemeSearchService.searchAndRenderView(form(migrationType), pageNumber = 1, searchText = None, migrationType)
          }
          else {
            emptyListRedirect(checkRacDac)
          }
        case _ => emptyListRedirect(checkRacDac)
      } recoverWith listOfSchemesRedirects
  }

  private def emptyListRedirect(checkRacDac: Boolean): Future[Result] = {
    if (checkRacDac) {
      Future.successful(Redirect(routes.NoSchemeToAddController.onPageLoadRacDac))
    } else {
      Future.successful(Redirect(routes.NoSchemeToAddController.onPageLoadScheme))
    }
  }

  def onPageLoadWithPageNumber(pageNumber: Index, migrationType: MigrationType): Action[AnyContent] =
    authenticate.async { implicit request =>
        schemeSearchService.searchAndRenderView(form(migrationType), pageNumber, searchText = None, migrationType)
      }

  def onSearch(migrationType: MigrationType): Action[AnyContent] = authenticate.async {
    implicit request =>
      search(migrationType)
  }

  def onSearchWithPageNumber(pageNumber: Index, migrationType: MigrationType): Action[AnyContent] = authenticate.async {
    implicit request =>
      search(migrationType)
  }

  private def search(migrationType: MigrationType)(implicit request: AuthenticatedRequest[AnyContent]): Future[Result] = {
    form(migrationType)
      .bindFromRequest()
      .fold(
        (formWithErrors: Form[String]) =>
          schemeSearchService.searchAndRenderView(formWithErrors, pageNumber = 1, searchText = None, migrationType),
        value =>
          schemeSearchService.searchAndRenderView(form(migrationType).fill(value), pageNumber = 1, searchText = Some(value), migrationType)
      )
  }

  def clickSchemeLink(pstr: String, isRacDac: Boolean): Action[AnyContent] = authenticate.async {
    implicit request =>
      lockingService.initialLockSetupAndRedirect(pstr, request, isRacDac)
  }

}


