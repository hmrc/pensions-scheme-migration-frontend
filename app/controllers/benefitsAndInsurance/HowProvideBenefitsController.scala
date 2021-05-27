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
import forms.benefitsAndInsurance.HowProvideBenefitsFormProvider
import identifiers.beforeYouStart.{SchemeNameId, SchemeTypeId}
import identifiers.benefitsAndInsurance.HowProvideBenefitsId
import models.benefitsAndInsurance.BenefitsProvisionType
import navigators.CompoundNavigator
import play.api.data.Form
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import renderer.Renderer
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import uk.gov.hmrc.viewmodels.NunjucksSupport
import utils.Enumerable

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class HowProvideBenefitsController @Inject()(override val messagesApi: MessagesApi,
                                       userAnswersCacheConnector: UserAnswersCacheConnector,
                                       authenticate: AuthAction,
                                       getData: DataRetrievalAction,
                                       requireData: DataRequiredAction,
                                       navigator: CompoundNavigator,
                                       formProvider: HowProvideBenefitsFormProvider,
                                       val controllerComponents: MessagesControllerComponents,
                                       config: AppConfig,
                                       renderer: Renderer)(implicit ec: ExecutionContext)
  extends FrontendBaseController  with I18nSupport with Retrievals with Enumerable.Implicits with NunjucksSupport {

  private def form(implicit messages: Messages): Form[BenefitsProvisionType] =
    formProvider()

  def onPageLoad: Action[AnyContent] =
    (authenticate andThen getData andThen requireData).async { implicit request =>
      SchemeNameId.retrieve.right.map { schemeName =>
        val preparedForm = request.userAnswers.get(HowProvideBenefitsId) match {
          case Some(value) =>
            form.fill(value)
          case None        => form
        }
        val json = Json.obj(
          "schemeName" -> schemeName,
          "form" -> preparedForm,
          "radios" -> BenefitsProvisionType.radios(preparedForm),
          "submitUrl" -> controllers.benefitsAndInsurance.routes.HowProvideBenefitsController.onSubmit().url,
          "returnUrl" -> controllers.routes.TaskListController.onPageLoad().url
        )
        renderer.render("benefitsAndInsurance/howProvideBenefits.njk", json).map(Ok(_))
      }
    }

  def onSubmit: Action[AnyContent] =
    (authenticate andThen getData andThen requireData).async { implicit request =>
      SchemeNameId.retrieve.right.map { schemeName =>
        form
          .bindFromRequest()
          .fold(
            formWithErrors => {
              val json = Json.obj(
                "schemeName" -> schemeName,
                "form" -> formWithErrors,
                "radios" -> BenefitsProvisionType.radios(formWithErrors),
                "submitUrl" -> controllers.benefitsAndInsurance.routes.HowProvideBenefitsController.onSubmit().url,
                "returnUrl" -> controllers.routes.TaskListController.onPageLoad().url
              )

              renderer.render("benefitsAndInsurance/howProvideBenefits.njk", json).map(BadRequest(_))
            },
            value => {
              val updatedUA = request.userAnswers.setOrException(HowProvideBenefitsId, value)
              userAnswersCacheConnector.save(request.lock, updatedUA.data).map { _ =>
                Redirect(navigator.nextPage(SchemeTypeId, updatedUA))
              }
            }
          )
        }
    }

}
