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

package controllers.racdac.individual

import audit.{AuditService, RetirementOrDeferredAnnuityContractMigrationEmailEvent}
import config.AppConfig
import connectors._
import controllers.actions.{AuthAction, DataRequiredAction, DataRetrievalAction}
import identifiers.beforeYouStart.SchemeNameId
import models.requests.DataRequest
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
                                       getData: DataRetrievalAction,
                                       requireData: DataRequiredAction,
                                       auditService: AuditService,
                                       minimalDetailsConnector: MinimalDetailsConnector,
                                       val controllerComponents: MessagesControllerComponents,
                                       renderer: Renderer,
                                       emailConnector: EmailConnector,
                                       crypto: ApplicationCrypto
                                     )(implicit val executionContext: ExecutionContext)
  extends FrontendBaseController
    with I18nSupport {

  def onPageLoad: Action[AnyContent] =
    (authenticate andThen getData andThen requireData(true)).async {
      implicit request =>
        minimalDetailsConnector.getPSAName.flatMap {
          psaName =>
            val json = Json.obj(
              "psaName" -> psaName,
              "submitUrl" -> controllers.racdac.individual.routes.DeclarationController.onSubmit().url,
              "returnUrl" -> controllers.routes.PensionSchemeRedirectController.onPageLoad().url
            )
            renderer.render("racdac/declaration.njk", json).map(Ok(_))
        }
    }

  def onSubmit: Action[AnyContent] =
    (authenticate andThen getData andThen requireData(true)).async {
      implicit request =>
        val psaId = request.psaId.id

         val userAnswers = request.userAnswers
         val racDacName = userAnswers.get(SchemeNameId)
          .getOrElse(throw new RuntimeException("Scheme Name is mandatory for RAC/DAC"))
        //TODO need to use when calling connector for ETMP
        // val policyNumberId= userAnswers.get(ContractOrPolicyNumberId)
        //  .getOrElse(throw new RuntimeException("Policy Number is mandatory for RAC/DAC"))

          sendEmail(racDacName,psaId)(implicitly).map { _ =>
            Redirect(controllers.racdac.individual.routes.ConfirmationController.onPageLoad().url)
        }
    }

  private def sendEmail(schemeName: String,psaId: String)
                       (implicit request: DataRequest[AnyContent]): Future[EmailStatus] = {
    logger.debug(s"Sending Rac Dac migration email for $psaId")
    minimalDetailsConnector.getPSADetails(psaId) flatMap { minimalPsa =>
      emailConnector.sendEmail(
        emailAddress = minimalPsa.email,
        templateName = appConfig.individualMigrationConfirmationEmailTemplateId,
        params = Map("psaName" -> minimalPsa.name,"schemeName"-> schemeName),
        callbackUrl(psaId)
      ).map { status =>
      auditService.sendEvent(RetirementOrDeferredAnnuityContractMigrationEmailEvent(psaId, minimalPsa.email))
      status
    }
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
