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

package controllers.racdac.individual

import audit.{AuditService, EmailAuditEvent}
import config.AppConfig
import connectors.*
import controllers.actions.{AuthAction, DataRequiredAction, DataRetrievalAction}
import identifiers.beforeYouStart.SchemeNameId
import models.JourneyType.RACDAC_IND_MIG
import models.RacDac
import models.requests.DataRequest
import play.api.i18n.Lang.logger
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.json.{JsString, __}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.crypto.PlainText
import uk.gov.hmrc.http.UpstreamErrorResponse
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import uk.gov.hmrc.play.bootstrap.frontend.filters.crypto.ApplicationCrypto
import utils.UserAnswers

import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class DeclarationController @Inject()(
                                       appConfig: AppConfig,
                                       override val messagesApi: MessagesApi,
                                       authenticate: AuthAction,
                                       getData: DataRetrievalAction,
                                       requireData: DataRequiredAction,
                                       auditService: AuditService,
                                       minimalDetailsConnector: MinimalDetailsConnector,
                                       pensionsSchemeConnector:PensionsSchemeConnector,
                                       val controllerComponents: MessagesControllerComponents,
                                       emailConnector: EmailConnector,
                                       crypto: ApplicationCrypto,
                                       declarationView: views.html.racdac.DeclarationView
                                     )(implicit val executionContext: ExecutionContext)
  extends FrontendBaseController
    with I18nSupport {

  def onPageLoad: Action[AnyContent] =
    (authenticate andThen getData andThen requireData(true)).async {
      implicit request =>
        minimalDetailsConnector.getPSAName.map {
          psaName =>
            Ok(
              declarationView(
                controllers.racdac.individual.routes.DeclarationController.onSubmit,
                controllers.routes.PensionSchemeRedirectController.onPageLoad.url,
                psaName
              )
            )
        }
    }

  def onSubmit: Action[AnyContent] =
    (authenticate andThen getData andThen requireData(true)).async {
      implicit request =>
        val psaId = request.psaId.id
        val pstrId = request.lock.pstr
        val userAnswers = request.userAnswers
        val racDacName = userAnswers.get(SchemeNameId)
          .getOrElse(throw new RuntimeException("Scheme Name is mandatory for RAC/DAC"))
        (for {
          updatedUa <- Future.fromTry(userAnswers.set(__ \ "pstr", JsString(request.lock.pstr)))
          _ <- pensionsSchemeConnector.registerScheme(UserAnswers(updatedUa.data), psaId, RacDac)
          _ <- sendEmail(racDacName, psaId, pstrId)
        } yield {
          Redirect(controllers.racdac.individual.routes.ConfirmationController.onPageLoad.url)
        }) recoverWith {
          case ex: UpstreamErrorResponse if ex.statusCode == UNPROCESSABLE_ENTITY =>
            Future.successful(Redirect(controllers.racdac.individual.routes.AddingRacDacController.onPageLoad))
          case _ =>
            Future.successful(Redirect(controllers.routes.YourActionWasNotProcessedController.onPageLoadRacDac))
        }
    }

  private def sendEmail(schemeName: String, psaId: String, pstrId:String)
                       (implicit request: DataRequest[AnyContent]): Future[EmailStatus] = {
    logger.debug(s"Sending Rac Dac migration email for $psaId")
    minimalDetailsConnector.getPSADetails(psaId) flatMap { minimalPsa =>
      emailConnector.sendEmail(
        emailAddress = minimalPsa.email,
        templateName = appConfig.individualMigrationConfirmationEmailTemplateId,
        params = Map("psaName" -> minimalPsa.name, "schemeName" -> schemeName),
        callbackUrl(psaId, pstrId)
      ).map { status =>
        auditService.sendEvent(EmailAuditEvent(psaId, RACDAC_IND_MIG, minimalPsa.email, pstrId))
        status
      }
    } recoverWith {
      case _: Throwable => Future.successful(EmailNotSent)
    }
  }

  private def callbackUrl(psaId: String, pstrId:String): String = {
    val encryptedPsa = URLEncoder.encode(crypto.QueryParameterCrypto.encrypt(PlainText(psaId)).value, StandardCharsets.UTF_8.toString)
    val encryptedPstr = URLEncoder.encode(crypto.QueryParameterCrypto.encrypt(PlainText(pstrId)).value, StandardCharsets.UTF_8.toString)
    s"${appConfig.migrationUrl}/pensions-scheme-migration/email-response/$RACDAC_IND_MIG/$encryptedPsa/$encryptedPstr"
  }
}
