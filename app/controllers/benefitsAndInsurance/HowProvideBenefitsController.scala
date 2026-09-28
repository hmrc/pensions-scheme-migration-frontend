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

package controllers.benefitsAndInsurance

import connectors.cache.UserAnswersCacheConnector
import controllers.Retrievals
import controllers.actions._
import forms.benefitsAndInsurance.HowProvideBenefitsFormProvider
import identifiers.beforeYouStart.SchemeNameId
import identifiers.benefitsAndInsurance.HowProvideBenefitsId
import models.benefitsAndInsurance.BenefitsProvisionType
import navigators.CompoundNavigator
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.Enumerable
import views.html.benefitsAndInsurance.HowProvideBenefitsView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class HowProvideBenefitsController @Inject()(override val messagesApi: MessagesApi,
                                       userAnswersCacheConnector: UserAnswersCacheConnector,
                                       authenticate: AuthAction,
                                       getData: DataRetrievalAction,
                                       requireData: DataRequiredAction,
                                       navigator: CompoundNavigator,
                                       formProvider: HowProvideBenefitsFormProvider,
                                       val controllerComponents: MessagesControllerComponents,
                                       view:HowProvideBenefitsView)(implicit ec: ExecutionContext)
  extends FrontendBaseController  with I18nSupport with Retrievals with Enumerable.Implicits {

  private def form: Form[BenefitsProvisionType] =
    formProvider()

  def onPageLoad: Action[AnyContent] =
    (authenticate andThen getData andThen requireData()).async { implicit request =>
      SchemeNameId.retrieve.map { schemeName =>
        val preparedForm = request.userAnswers.get(HowProvideBenefitsId) match {
          case Some(value) =>
            form.fill(value)
          case None        => form
        }
        Future.successful(Ok(view(
          preparedForm,
          schemeName,
          BenefitsProvisionType.radios(preparedForm),
          controllers.benefitsAndInsurance.routes.HowProvideBenefitsController.onSubmit
        )))
    }
}

  def onSubmit: Action[AnyContent] =
    (authenticate andThen getData andThen requireData()).async { implicit request =>
      SchemeNameId.retrieve.map { schemeName =>
        form
          .bindFromRequest()
          .fold(
            formWithErrors => {
              Future.successful(BadRequest(view(
                formWithErrors,
                schemeName,
                BenefitsProvisionType.radios(formWithErrors),
                controllers.benefitsAndInsurance.routes.BenefitsTypeController.onSubmit
              )))
            },
            value => {
              val updatedUA = request.userAnswers.setOrException(HowProvideBenefitsId, value)
              userAnswersCacheConnector.save(request.lock, updatedUA.data).map { _ =>
                Redirect(navigator.nextPage(HowProvideBenefitsId, updatedUA))
              }
            }
          )
      }
    }

}