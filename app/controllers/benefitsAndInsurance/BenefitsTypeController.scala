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

import config.AppConfig
import connectors.cache.UserAnswersCacheConnector
import controllers.Retrievals
import controllers.actions._
import forms.benefitsAndInsurance.BenefitsTypeFormProvider
import identifiers.beforeYouStart.SchemeNameId
import identifiers.benefitsAndInsurance.BenefitsTypeId
import models.benefitsAndInsurance.BenefitsType
import navigators.CompoundNavigator
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.nunjucks.NunjucksSupport
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.{Enumerable, TwirlMigration}
import views.html.benefitsAndInsurance.BenefitsTypeView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class BenefitsTypeController @Inject()(override val messagesApi: MessagesApi,
                                       userAnswersCacheConnector: UserAnswersCacheConnector,
                                       authenticate: AuthAction,
                                       getData: DataRetrievalAction,
                                       requireData: DataRequiredAction,
                                       navigator: CompoundNavigator,
                                       formProvider: BenefitsTypeFormProvider,
                                       val controllerComponents: MessagesControllerComponents,
                                       config: AppConfig,
                                       view: BenefitsTypeView
                                       )(implicit ec: ExecutionContext)
  extends FrontendBaseController  with I18nSupport with Retrievals with Enumerable.Implicits {

  private def form: Form[BenefitsType] =
    formProvider()

  def onPageLoad: Action[AnyContent] =
    (authenticate andThen getData andThen requireData()).async { implicit request =>
      SchemeNameId.retrieve.map { schemeName =>
        val preparedForm = request.userAnswers.get(BenefitsTypeId) match {
          case Some(value) =>
            form.fill(value)
          case None        => form
        }
        Future.successful(Ok(view(
          preparedForm,
          schemeName,
          BenefitsType.radios(preparedForm),
          controllers.benefitsAndInsurance.routes.BenefitsTypeController.onSubmit
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
                BenefitsType.radios(formWithErrors),
                controllers.benefitsAndInsurance.routes.BenefitsTypeController.onSubmit
              )))
            },
            value => {
              val updatedUA = request.userAnswers.setOrException(BenefitsTypeId, value)
              userAnswersCacheConnector.save(request.lock, updatedUA.data).map { _ =>
                Redirect(navigator.nextPage(BenefitsTypeId, updatedUA))
              }
            }
          )
        }
    }

}
