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
import connectors.cache.{BulkMigrationQueueConnector, CurrentPstrCacheConnector}
import controllers.actions.{AuthAction, BulkDataAction}
import models.racDac.RacDacRequest
import play.api.Logger
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import renderer.Renderer
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class DeclarationController @Inject()(
                                       appConfig: AppConfig,
                                       override val messagesApi: MessagesApi,
                                       authenticate: AuthAction,
                                       val controllerComponents: MessagesControllerComponents,
                                       bulkMigrationQueueConnector: BulkMigrationQueueConnector,
                                       getData: BulkDataAction,
                                       schemeCacheConnector: CurrentPstrCacheConnector,
                                       declarationView: views.html.racdac.DeclarationView
                                     )(implicit val executionContext: ExecutionContext)
  extends FrontendBaseController
    with I18nSupport {

  def onPageLoad: Action[AnyContent] =
    (authenticate andThen getData(true)) {
      implicit request =>
        Ok(declarationView(
          routes.DeclarationController.onSubmit,
          appConfig.psaOverviewUrl,
          request.md.name
        ))
    }

  private val logger = Logger(classOf[DeclarationController])

  def onSubmit: Action[AnyContent] =
    (authenticate andThen getData(false)).async {
      implicit request =>
        val psaId = request.request.psaId.id
        val racDacSchemes = request.lisOfSchemes.filter(_.racDac).map { items =>
          RacDacRequest(items.schemeName, items.policyNo.getOrElse(
            throw new RuntimeException("Policy Number is mandatory for RAC/DAC")), items.pstr, items.declarationDate, items.schemeOpenDate)
        }
        bulkMigrationQueueConnector.pushAll(psaId, Json.toJson(racDacSchemes)).flatMap { e =>
          val confirmationData = Json.obj(
            "confirmationData" -> Json.obj(
              "email" -> request.md.email,
              "psaId" -> request.request.psaId.id
            )
          )
          schemeCacheConnector.save(confirmationData).map { _ =>
            Redirect(routes.ProcessingRequestController.onPageLoad.url)
          }
        } recoverWith {
          case ex =>
            logger.warn(ex.getMessage, ex)
            Future.successful(Redirect(routes.RequestNotProcessedController.onPageLoad))
        }
    }
}
