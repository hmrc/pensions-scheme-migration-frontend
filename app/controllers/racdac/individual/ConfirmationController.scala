/*
 * Copyright 2023 HM Revenue & Customs
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

import config.AppConfig
import connectors.cache.{CurrentPstrCacheConnector, LockCacheConnector, UserAnswersCacheConnector}
import connectors.{ListOfSchemesConnector, MinimalDetailsConnector}
import controllers.actions._
import helpers.cya.CYAHelper
import identifiers.beforeYouStart.SchemeNameId
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import renderer.Renderer
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class ConfirmationController @Inject()(appConfig: AppConfig,
                                       override val messagesApi: MessagesApi,
                                       authenticate: AuthAction,
                                       getData: DataRetrievalAction,
                                       requireData: DataRequiredAction,
                                       minimalDetailsConnector: MinimalDetailsConnector,
                                       userAnswersCacheConnector: UserAnswersCacheConnector,
                                       currentPstrCacheConnector:CurrentPstrCacheConnector,
                                       lockCacheConnector:LockCacheConnector,
                                       listOfSchemesConnector:ListOfSchemesConnector,
                                       val controllerComponents: MessagesControllerComponents,
                                       renderer: Renderer
                                       )(implicit ec: ExecutionContext)
  extends FrontendBaseController
    with I18nSupport {

  def onPageLoad: Action[AnyContent] =
    (authenticate andThen getData andThen requireData(true)).async {
      implicit request =>
        val jsonFuture =
          for {
            email <- minimalDetailsConnector.getPSAEmail
            _ <- currentPstrCacheConnector.remove
            _ <- lockCacheConnector.removeLock(request.lock)
            _ <- userAnswersCacheConnector.remove(request.lock.pstr)
            _ <- listOfSchemesConnector.removeCache(request.psaId.id)
          } yield {
            Json.obj(
              "schemeName" -> CYAHelper.getAnswer(SchemeNameId)(request.userAnswers, implicitly),
              "pstr" -> request.lock.pstr,
              "email" -> email,
              "yourSchemesLink" -> appConfig.yourPensionSchemesUrl,
              "returnUrl" -> appConfig.psaOverviewUrl
            )
          }

        jsonFuture.flatMap {
          json =>
            renderer.render("racdac/individual/confirmation.njk", json).map(Ok(_))
        }
    }
}
