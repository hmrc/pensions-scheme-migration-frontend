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

package controllers

import audit.{AuditService, EmailAuditEvent}
import config.AppConfig
import connectors.*
import controllers.actions.*
import identifiers.beforeYouStart.{SchemeNameId, WorkingKnowledgeId}
import models.JourneyType.SCHEME_MIG
import models.Scheme
import models.requests.DataRequest
import play.api.i18n.Lang.logger
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.json.{JsString, __}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.crypto.PlainText
import uk.gov.hmrc.play.bootstrap.frontend.filters.crypto.ApplicationCrypto
import uk.gov.hmrc.http.UpstreamErrorResponse
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.UserAnswers
import views.html.DeclarationView

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
                                       val controllerComponents: MessagesControllerComponents,
                                       emailConnector: EmailConnector,
                                       minimalDetailsConnector: MinimalDetailsConnector,
                                       pensionsSchemeConnector: PensionsSchemeConnector,
                                       crypto: ApplicationCrypto,
                                       declarationView: DeclarationView
                                     )(implicit val executionContext: ExecutionContext)
  extends FrontendBaseController
    with I18nSupport
    with Retrievals {

  def onPageLoad: Action[AnyContent] =
    (authenticate andThen getData andThen requireData()).async {
      implicit request =>
        val hasWorkingKnowledge = if (request.userAnswers.get(WorkingKnowledgeId).contains(true)) true else false
        SchemeNameId.retrieve.map { schemeName =>
            Future.successful(Ok(declarationView(
              schemeName, true, hasWorkingKnowledge,
              routes.DeclarationController.onSubmit
              ))
            )
        }
    }

  def onSubmit: Action[AnyContent] =
    (authenticate andThen getData andThen requireData()).async {
      implicit request =>
        SchemeNameId.retrieve.map { schemeName =>
          val psaId = request.psaId.id
          val pstrId = request.lock.pstr
          val userAnswers = request.userAnswers
          (for {
            updatedUa <- Future.fromTry(userAnswers.set(__ \ "pstr", JsString(request.lock.pstr)))
            apiResult <- pensionsSchemeConnector.registerScheme(UserAnswers(updatedUa.data), psaId, Scheme)
            _ <- if (apiResult.nonEmpty) sendEmail(schemeName, psaId, pstrId) else Future(EmailNotSent)
          } yield {
            Redirect(routes.SchemeSuccessController.onPageLoad)
          }) recoverWith {
            case ex: UpstreamErrorResponse if ex.statusCode == UNPROCESSABLE_ENTITY =>
              Future.successful(Redirect(controllers.routes.AddingSchemeController.onPageLoad))
            case error =>
              logger.error(
                s"""
                   |Failed to submit declaration:
                   |  PSTR: $pstrId
                   |  Exception: ${error.getMessage}
                   |  StackTrace: ${error.getStackTrace.mkString("\n")}
                   |""".stripMargin
              )
              Future.successful(Redirect(controllers.routes.YourActionWasNotProcessedController.onPageLoadScheme))
          }
        }
    }

  private def sendEmail(schemeName: String, psaId: String, pstrId: String)
                       (implicit request: DataRequest[AnyContent]): Future[EmailStatus] = {
    logger.debug("Fetch email from API")

    minimalDetailsConnector.getPSADetails(psaId) flatMap { minimalPsa =>
      emailConnector.sendEmail(
        emailAddress = minimalPsa.email,
        templateName = appConfig.schemeConfirmationEmailTemplateId,
        params = Map("psaName" -> minimalPsa.name, "schemeName" -> schemeName),
        callbackUrl(psaId, pstrId)
      )
        .map {
          status =>
            auditService.sendEvent(EmailAuditEvent(psaId, SCHEME_MIG, minimalPsa.email, pstrId))
            status
        }
    } recoverWith {
      case _: Throwable => Future.successful(EmailNotSent)
    }
  }

  private def callbackUrl(psaId: String, pstrId: String): String = {
    val encryptedPsa = URLEncoder.encode(crypto.QueryParameterCrypto.encrypt(PlainText(psaId)).value, StandardCharsets.UTF_8.toString)
    val encryptedPstr = URLEncoder.encode(crypto.QueryParameterCrypto.encrypt(PlainText(pstrId)).value, StandardCharsets.UTF_8.toString)
    s"${appConfig.migrationUrl}/pensions-scheme-migration/email-response/${SCHEME_MIG}/$encryptedPsa/$encryptedPstr"
  }
}
