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

import config.AppConfig
import connectors.cache.CurrentPstrCacheConnector
import connectors.{ListOfSchemesConnector, MinimalDetailsConnector}
import controllers.Retrievals
import controllers.actions._
import forms.YesNoFormProvider
import models.RacDac
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.nunjucks.NunjucksSupport
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import uk.gov.hmrc.viewmodels.Radios
import utils.{Enumerable, TwirlMigration}
import utils.HttpResponseRedirects.listOfSchemesRedirects
import views.html.racdac.TransferAllView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class TransferAllController @Inject()(appConfig: AppConfig,
                                      override val messagesApi: MessagesApi,
                                      authenticate: AuthAction,
                                      formProvider: YesNoFormProvider,
                                      minimalDetailsConnector: MinimalDetailsConnector,
                                      listOfSchemesConnector: ListOfSchemesConnector,
                                      currentPstrCacheConnector: CurrentPstrCacheConnector,
                                      val controllerComponents: MessagesControllerComponents,
                                      transferAllView: TransferAllView
                                     )(implicit val executionContext: ExecutionContext) extends
  FrontendBaseController with I18nSupport with Retrievals with Enumerable.Implicits with NunjucksSupport {

  private val form = formProvider("messages__transferAll__error")

  def onPageLoad: Action[AnyContent] = authenticate.async {
    implicit request =>
      listOfSchemesConnector.getListOfSchemes(request.psaId.id).flatMap {
        case Right(list) =>
          if (list.items.getOrElse(Nil).exists(_.racDac)) {
            minimalDetailsConnector.getPSAName.map { psaName =>
              Ok(transferAllView(
                form,
                routes.TransferAllController.onSubmit,
                appConfig.psaOverviewUrl,
                psaName,
                TwirlMigration.toTwirlRadios(Radios.yesNo(form("value")))
              ))
            }
          } else {
            Future.successful(Redirect(controllers.preMigration.routes.NoSchemeToAddController.onPageLoadRacDac))
          }
        case _ => Future.successful(Redirect(controllers.preMigration.routes.NoSchemeToAddController.onPageLoadRacDac))
      } recoverWith listOfSchemesRedirects
  }

  def onSubmit: Action[AnyContent] = authenticate.async {
    implicit request =>
      form.bindFromRequest().fold(
        (formWithErrors: Form[_]) => minimalDetailsConnector.getPSAName.map { psaName =>
          BadRequest(transferAllView(
            formWithErrors,
            routes.TransferAllController.onSubmit,
            appConfig.psaOverviewUrl,
            psaName,
            TwirlMigration.toTwirlRadios(Radios.yesNo(formWithErrors("value")))
          ))
        }, { value =>
          currentPstrCacheConnector.remove.map { _ =>
            value match {
              case true =>
                Redirect(controllers.racdac.bulk.routes.BulkListController.onPageLoad)
              case _ =>
                Redirect(controllers.preMigration.routes.ListOfSchemesController.onPageLoad(RacDac))
            }
          }
        }
      )
  }
}
