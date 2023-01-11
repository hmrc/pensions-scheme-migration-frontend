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
import connectors.cache.UserAnswersCacheConnector
import connectors.{ListOfSchemesConnector, MinimalDetailsConnector}
import controllers.Retrievals
import controllers.actions.{AuthAction, DataRetrievalAction}
import helpers.cya.{CYAHelper, RacDacIndividualCYAHelper}
import identifiers.beforeYouStart.SchemeNameId
import models.RacDac
import models.requests.OptionalDataRequest
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import renderer.Renderer
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.HttpResponseRedirects.listOfSchemesRedirects
import utils.{Enumerable, UserAnswers}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CheckYourAnswersController @Inject()(
                                            override val messagesApi: MessagesApi,
                                            appConfig: AppConfig,
                                            authenticate: AuthAction,
                                            getData: DataRetrievalAction,
                                            cyaHelper: RacDacIndividualCYAHelper,
                                            listOfSchemesConnector: ListOfSchemesConnector,
                                            userAnswersCacheConnector: UserAnswersCacheConnector,
                                            minimalDetailsConnector: MinimalDetailsConnector,
                                            val controllerComponents: MessagesControllerComponents,
                                            renderer: Renderer
                                          )(implicit val ec: ExecutionContext)
  extends FrontendBaseController
    with Enumerable.Implicits
    with I18nSupport
    with Retrievals {

  def onPageLoad: Action[AnyContent] =
    (authenticate andThen getData).async {
      implicit request =>
        (request.userAnswers, request.lock) match {
          case (_, None) =>
            Future.successful(Redirect(controllers.preMigration.routes.ListOfSchemesController.onPageLoad(RacDac)))

          case (Some(ua), _) =>
            renderView(ua)

          case (None, Some(lock)) =>
            listOfSchemesConnector.getListOfSchemes(request.psaId.id).flatMap {
              case Right(listOfSchemes) =>
                val racDac = listOfSchemes.items.getOrElse(Nil).filter(item => item.racDac && item.pstr == lock.pstr)
                if (racDac.nonEmpty) {
                  val userAnswers: UserAnswers = UserAnswers(Json.toJson(racDac.head).as[JsObject])
                  userAnswersCacheConnector.save(lock, Json.toJson(racDac.head)).flatMap { _ =>
                    renderView(userAnswers)
                  }
                } else {
                  Future.successful(Redirect(controllers.preMigration.routes.ListOfSchemesController.onPageLoad(RacDac)))
                }
              case _ => Future.successful(Redirect(appConfig.psaOverviewUrl))
            } recoverWith listOfSchemesRedirects
        }
    }

  private def renderView(userAnswers: UserAnswers)(implicit request: OptionalDataRequest[AnyContent]): Future[Result] = {
    minimalDetailsConnector.getPSAName.flatMap { psaName =>
      renderer.render(
        template = "racdac/individual/check-your-answers.njk",
        ctx = Json.obj(
          "list" -> cyaHelper.detailsRows(userAnswers),
          "schemeName" -> CYAHelper.getAnswer(SchemeNameId)(userAnswers, implicitly),
          "submitUrl" -> controllers.racdac.individual.routes.DeclarationController.onPageLoad.url,
          "psaName" -> psaName,
          "returnUrl" -> controllers.routes.PensionSchemeRedirectController.onPageLoad.url
        )
      ).map(Ok(_))
    }
  }
}

