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

package controllers.benefitsAndInsurance

import config.AppConfig
import connectors.cache.UserAnswersCacheConnector
import controllers.Retrievals
import controllers.actions._
import forms.benefitsAndInsurance.IsOccupationalFormProvider
import identifiers.beforeYouStart.{SchemeNameId, SchemeTypeId}
import identifiers.benefitsAndInsurance.IsOccupationalId
import navigators.CompoundNavigator
import play.api.data.Form
import play.api.i18n.{MessagesApi, Messages, I18nSupport}
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import renderer.Renderer
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import uk.gov.hmrc.viewmodels.{Radios, NunjucksSupport}
import utils.Enumerable

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class IsOccupationalController @Inject()(override val messagesApi: MessagesApi,
                                       userAnswersCacheConnector: UserAnswersCacheConnector,
                                       authenticate: AuthAction,
                                       getData: DataRetrievalAction,
                                       requireData: DataRequiredAction,
                                       navigator: CompoundNavigator,
                                       formProvider: IsOccupationalFormProvider,
                                       val controllerComponents: MessagesControllerComponents,
                                       config: AppConfig,
                                       renderer: Renderer)(implicit ec: ExecutionContext)
  extends FrontendBaseController  with I18nSupport with Retrievals with Enumerable.Implicits with NunjucksSupport {

  private def form(schemeName: String)(implicit messages: Messages): Form[Boolean] =
    formProvider(messages("isOccupational.error.required", schemeName))

  def onPageLoad: Action[AnyContent] =
    (authenticate andThen getData andThen requireData).async { implicit request =>
      SchemeNameId.retrieve.right.map { schemeName =>
        val preparedForm = request.userAnswers.get(IsOccupationalId) match {
          case Some(value) => form(schemeName).fill(value)
          case None        => form(schemeName)
        }
        val json = Json.obj(
          "schemeName" -> schemeName,
          "form" -> preparedForm,
          "radios" -> Radios.yesNo (preparedForm("value")),
          "submitUrl" -> controllers.benefitsAndInsurance.routes.IsOccupationalController.onSubmit().url,
          "returnUrl" -> controllers.routes.TaskListController.onPageLoad().url
        )
        renderer.render("benefitsAndInsurance/isOccupational.njk", json).map(Ok(_))
      }
    }

  def onSubmit: Action[AnyContent] =
    (authenticate andThen getData andThen requireData).async { implicit request =>
      SchemeNameId.retrieve.right.map { schemeName =>
        form(schemeName)
          .bindFromRequest()
          .fold(
            formWithErrors => {
              val json = Json.obj(
                "schemeName" -> schemeName,
                "form" -> formWithErrors,
                "radios" -> Radios.yesNo(form(schemeName)(implicitly)("value")),
                "submitUrl" -> controllers.benefitsAndInsurance.routes.IsOccupationalController.onSubmit().url,
                "returnUrl" -> controllers.routes.TaskListController.onPageLoad().url
              )

              renderer.render("benefitsAndInsurance/isOccupational.njk", json).map(BadRequest(_))
            },
            value => {
              val updatedUA = request.userAnswers.setOrException(IsOccupationalId, value)
              userAnswersCacheConnector.save(request.lock, updatedUA.data).map { _ =>
                Redirect(navigator.nextPage(SchemeTypeId, updatedUA))
              }
            }
          )
        }
    }

}
