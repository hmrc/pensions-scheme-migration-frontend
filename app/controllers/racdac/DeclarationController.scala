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

package controllers.racdac

import config.AppConfig
import connectors.cache.BulkMigrationQueueConnector
import connectors._
import controllers.actions.AuthAction
import models.racDac.Request
import models.requests.AuthenticatedRequest
import play.api.i18n.Lang.logger
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import renderer.Renderer
import uk.gov.hmrc.crypto.{ApplicationCrypto, PlainText}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class DeclarationController @Inject()(
                                       appConfig: AppConfig,
                                       override val messagesApi: MessagesApi,
                                       authenticate: AuthAction,
                                       minimalDetailsConnector: MinimalDetailsConnector,
                                       val controllerComponents: MessagesControllerComponents,
                                       renderer: Renderer,
                                       bulkMigrationQueueConnector: BulkMigrationQueueConnector,
                                       listOfSchemesConnector: ListOfSchemesConnector,
                                       emailConnector: EmailConnector,
                                       crypto: ApplicationCrypto
                                     )(implicit val executionContext: ExecutionContext)
  extends FrontendBaseController
    with I18nSupport {

  def onPageLoad: Action[AnyContent] =
    authenticate.async {
      implicit request =>
        minimalDetailsConnector.getPSAName.flatMap {
          psaName =>
            val json = Json.obj(
              "psaName" -> psaName,
              "submitUrl" -> routes.DeclarationController.onSubmit().url,
              "returnUrl" -> appConfig.psaOverviewUrl
            )
            renderer.render("racdac/declaration.njk", json).map(Ok(_))
        }
    }

  def onSubmit: Action[AnyContent] =
    authenticate.async {
      implicit request =>
        val psaId = request.psaId.id
        listOfSchemesConnector.getListOfSchemes(psaId).flatMap {
          case Right(listOfSchemes) =>
            val racDacSchemes = listOfSchemes.items.getOrElse(Nil).filter(_.racDac).map { items =>
              Request(items.schemeName, items.policyNo.getOrElse(
                throw new Exception("Policy Number is mandatory for RAC/DAC")))
            }
            bulkMigrationQueueConnector.pushAll(psaId, Json.toJson(racDacSchemes)).flatMap { _ =>
              sendEmail(psaId).map { _ =>
                Redirect(controllers.racdac.routes.ConfirmationController.onPageLoad().url)
              }
            }
          case _ =>
            Future(Redirect(controllers.routes.IndexController.onPageLoad()))
        }
    }

  private def sendEmail(psaId: String)
                       (implicit request: AuthenticatedRequest[AnyContent]): Future[EmailStatus] = {
    logger.debug(s"Sending bulk migration email for $psaId")
    minimalDetailsConnector.getPSADetails(psaId) flatMap { minimalPsa =>
      emailConnector.sendEmail(
        emailAddress = minimalPsa.email,
        templateName = appConfig.bulkMigrationConfirmationEmailTemplateId,
        params = Map("psaName" -> minimalPsa.name),
        callbackUrl(psaId)
      )
    } recoverWith {
      case _: Throwable => Future.successful(EmailNotSent)
    }
  }

  //Todo: To be edited while implementing audit event
  private def callbackUrl(psaId: String): String = {
    val encryptedPsa = crypto.QueryParameterCrypto.encrypt(PlainText(psaId)).value
    s"${appConfig.migrationUrl}/email-response/$encryptedPsa"
  }
}
