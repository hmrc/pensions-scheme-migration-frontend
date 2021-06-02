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
import forms.benefitsAndInsurance.BenefitsInsurancePolicyFormProvider
import identifiers.beforeYouStart.{SchemeNameId, SchemeTypeId}
import identifiers.benefitsAndInsurance.{BenefitsInsurancePolicyId, BenefitsInsuranceNameId}
import navigators.CompoundNavigator
import play.api.data.Form
import play.api.i18n.{MessagesApi, Messages, I18nSupport}
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import renderer.Renderer
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import uk.gov.hmrc.viewmodels.NunjucksSupport
import utils.Enumerable

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class BenefitsInsurancePolicyController @Inject()(override val messagesApi: MessagesApi,
                                       userAnswersCacheConnector: UserAnswersCacheConnector,
                                       authenticate: AuthAction,
                                       getData: DataRetrievalAction,
                                       requireData: DataRequiredAction,
                                       navigator: CompoundNavigator,
                                       formProvider: BenefitsInsurancePolicyFormProvider,
                                       val controllerComponents: MessagesControllerComponents,
                                       config: AppConfig,
                                       renderer: Renderer)(implicit ec: ExecutionContext)
  extends FrontendBaseController  with I18nSupport with Retrievals with Enumerable.Implicits with NunjucksSupport {

  private def form(implicit messages: Messages): Form[String] =
    formProvider()

  def onPageLoad: Action[AnyContent] =
    (authenticate andThen getData andThen requireData).async { implicit request =>
      (SchemeNameId and BenefitsInsuranceNameId).retrieve.right.map { case schemeName ~ insurancePolicyName =>
        val preparedForm = request.userAnswers.get(BenefitsInsurancePolicyId) match {
          case Some(value) => form.fill(value)
          case None        => form
        }
        val json = Json.obj(
          "schemeName" -> schemeName,
          "insurancePolicyName" -> insurancePolicyName,
          "form" -> preparedForm,
          "submitUrl" -> controllers.benefitsAndInsurance.routes.BenefitsInsurancePolicyController.onSubmit().url,
          "returnUrl" -> controllers.routes.TaskListController.onPageLoad().url
        )
        renderer.render("benefitsAndInsurance/benefitsInsurancePolicy.njk", json).map(Ok(_))
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
                "submitUrl" -> controllers.benefitsAndInsurance.routes.BenefitsInsurancePolicyController.onSubmit().url,
                "returnUrl" -> controllers.routes.TaskListController.onPageLoad().url
              )

              renderer.render("benefitsAndInsurance/benefitsInsurancePolicy.njk", json).map(BadRequest(_))
            },
            value => {
              val updatedUA = request.userAnswers.setOrException(BenefitsInsurancePolicyId, value)
              userAnswersCacheConnector.save(request.lock, updatedUA.data).map { _ =>
                Redirect(navigator.nextPage(BenefitsInsurancePolicyId, updatedUA))
              }
            }
          )
        }
    }

}