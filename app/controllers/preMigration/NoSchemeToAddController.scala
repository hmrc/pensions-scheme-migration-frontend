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

import config.AppConfig
import connectors.{AncillaryPsaException, ListOfSchemesConnector, MinimalDetailsConnector}
import controllers.actions.AuthAction
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import renderer.Renderer
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import uk.gov.hmrc.viewmodels.MessageInterpolators

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class NoSchemeToAddController @Inject()(val appConfig: AppConfig,
                                      override val messagesApi: MessagesApi,
                                      authenticate: AuthAction,
                                      val controllerComponents: MessagesControllerComponents,
                                        listOfSchemesConnector: ListOfSchemesConnector,
                                      minimalDetailsConnector: MinimalDetailsConnector,
                                      renderer: Renderer
                                     )(implicit val executionContext: ExecutionContext) extends
  FrontendBaseController with I18nSupport {

  def onPageLoadScheme: Action[AnyContent] = authenticate.async {
    implicit request =>
      listOfSchemesConnector.getListOfSchemes(request.psaId.id).flatMap{
        case Right(list) => if(list.items.getOrElse(Nil).exists(!_.racDac)){
         val x= minimalDetailsConnector.getPSAName.flatMap{
           psaName =>
           for {
             json <- schemeJson(psaName)
           } yield{
               renderer.render("preMigration/noSchemeToAdd.njk", json).map(Ok(_))
             }
         }
        } else {
          Future.successful(Redirect(routes.NotRegisterController.onPageLoadScheme()))
        }
        case _ => Future.successful(Redirect(routes.NotRegisterController.onPageLoadScheme()))
      }recoverWith {
        case _: AncillaryPsaException =>
          Future.successful(Redirect(routes.CannotMigrateController.onPageLoad()))
      }
  }

  def onPageLoadRacDac: Action[AnyContent] = authenticate.async {
    implicit request =>
      minimalDetailsConnector.getPSAName.flatMap { psaName =>
        renderer.render(
          template = "preMigration/noSchemeToAdd.njk",
          ctx = racDacJson(psaName)
        ).map(Ok(_))
      }
  }

  private def schemeJson(psaName: String)(implicit messages: Messages):JsObject = {
    Json.obj(
      "param1" -> msg"messages__pension_scheme".resolve,
      "psaName" -> psaName,
      "contactHmrcUrl" -> appConfig.contactHmrcUrl,
      "returnUrl" -> appConfig.psaOverviewUrl
    )
  }

  private def racDacJson(psaName: String)(implicit messages: Messages):JsObject = {
    Json.obj(
      "param1" -> msg"messages__racdac".resolve,
      "psaName" -> psaName,
      "contactHmrcUrl" -> appConfig.contactHmrcUrl,
      "returnUrl" -> appConfig.psaOverviewUrl
    )
  }
}

