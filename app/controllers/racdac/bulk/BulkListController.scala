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

package controllers.racdac.bulk

import com.google.inject.Inject
import config.AppConfig
import controllers.actions.{AuthAction, BulkDataAction}
import forms.racdac.RacDacBulkListFormProvider
import models.requests.BulkDataRequest
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.BulkRacDacService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import scala.concurrent.ExecutionContext

class BulkListController @Inject()(
                                    val appConfig: AppConfig,
                                    override val messagesApi: MessagesApi,
                                    authenticate: AuthAction,
                                    getData: BulkDataAction,
                                    val controllerComponents: MessagesControllerComponents,
                                    formProvider: RacDacBulkListFormProvider,
                                    bulkRacDacService: BulkRacDacService
                                  )(implicit val ec: ExecutionContext)
  extends FrontendBaseController
    with I18nSupport {

  val form: Form[Boolean] = formProvider()

  def onPageLoad: Action[AnyContent] = (authenticate andThen getData(false)) {
    implicit request =>
      bulkRacDacService.renderRacDacBulkView(form, pageNumber = 1)
  }

  def onPageLoadWithPageNumber(pageNumber: Int): Action[AnyContent] = (authenticate andThen getData(false)) {
    implicit request =>
      bulkRacDacService.renderRacDacBulkView(form, pageNumber)
  }

  def onSubmit: Action[AnyContent] = (authenticate andThen getData(false)) {
    implicit request =>
      submit(pageNumber = 1)
  }

  def onSubmitWithPageNumber(pageNumber: Int): Action[AnyContent] = (authenticate andThen getData(false)) {
    implicit request =>
      submit(pageNumber)
  }

  private def submit(pageNumber: Int)(implicit request: BulkDataRequest[AnyContent]): Result = {
    form.bindFromRequest().fold(formWithErrors =>
      bulkRacDacService.renderRacDacBulkView(formWithErrors, pageNumber),
      { case true =>
        Redirect(routes.DeclarationController.onPageLoad)
      case _ =>
        Redirect(appConfig.psaOverviewUrl)
      }
    )
  }
}
