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

package controllers.racdac.bulk

import audit.{AuditService, EmailAuditEvent}
import config.AppConfig
import connectors._
import connectors.cache.BulkMigrationQueueConnector
import controllers.actions.{AuthAction, BulkDataAction}
import models.JourneyType.RACDAC_BULK_MIG
import models.racDac.RacDacRequest
import models.requests.BulkDataRequest
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
                                       val controllerComponents: MessagesControllerComponents,
                                       renderer: Renderer,
                                       bulkMigrationQueueConnector: BulkMigrationQueueConnector,
                                       getData: BulkDataAction,
                                       emailConnector: EmailConnector,
                                       auditService: AuditService,
                                       crypto: ApplicationCrypto
                                     )(implicit val executionContext: ExecutionContext)
  extends FrontendBaseController
    with I18nSupport {

  def onPageLoad: Action[AnyContent] =
    (authenticate andThen getData(true)).async {
      implicit request =>
        val json = Json.obj(
          "psaName" -> request.md.name,
          "submitUrl" -> routes.DeclarationController.onSubmit().url,
          "returnUrl" -> appConfig.psaOverviewUrl
        )
        renderer.render("racdac/declaration.njk", json).map(Ok(_))
    }

  def onSubmit: Action[AnyContent] =
    (authenticate andThen getData(false)).async {
      implicit request =>
        val psaId = request.request.psaId.id
        val racDacSchemes = request.lisOfSchemes.filter(_.racDac).map { items =>
          RacDacRequest(items.schemeName, items.policyNo.getOrElse(
            throw new RuntimeException("Policy Number is mandatory for RAC/DAC")))
        }

        bulkMigrationQueueConnector.pushAll(psaId, Json.toJson(racDacSchemes)).flatMap { _ =>
          sendEmail(psaId).map { _ =>
            Redirect(routes.ConfirmationController.onPageLoad().url)
          }
        } recoverWith {
          case _ =>
            Future.successful(Redirect(routes.RequestNotProcessedController.onPageLoad()))
        }
    }

  private def sendEmail(psaId: String)
                       (implicit request: BulkDataRequest[AnyContent]): Future[EmailStatus] = {
    logger.debug(s"Sending bulk migration email for $psaId")
    emailConnector.sendEmail(
      emailAddress = request.md.email,
      templateName = appConfig.bulkMigrationConfirmationEmailTemplateId,
      params = Map("psaName" -> request.md.name),
      callbackUrl(psaId)
    ).map { status =>
      auditService.sendEvent(EmailAuditEvent(psaId, RACDAC_BULK_MIG, request.md.email))
      status
    } recoverWith {
      case _: Throwable => Future.successful(EmailNotSent)
    }
  }

  private def callbackUrl(psaId: String): String = {
    val encryptedPsa = crypto.QueryParameterCrypto.encrypt(PlainText(psaId)).value
    s"${appConfig.migrationUrl}/email-response/${RACDAC_BULK_MIG}/$encryptedPsa"
  }
}
