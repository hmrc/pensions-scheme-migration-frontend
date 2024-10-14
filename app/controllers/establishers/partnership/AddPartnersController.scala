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

package controllers.establishers.partnership

import config.AppConfig
import controllers.Retrievals
import controllers.actions._
import forms.establishers.partnership.partner.AddPartnersFormProvider
import helpers.AddToListHelper
import identifiers.establishers.partnership.AddPartnersId
import models.{Mode, PartnerEntity}
import navigators.CompoundNavigator
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import uk.gov.hmrc.viewmodels.Radios
import utils.TwirlMigration
import views.html.establishers.partnership.AddPartnerView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class AddPartnersController @Inject()(
                                               override val messagesApi: MessagesApi,
                                               navigator: CompoundNavigator,
                                               authenticate: AuthAction,
                                               getData: DataRetrievalAction,
                                               requireData: DataRequiredAction,
                                               formProvider: AddPartnersFormProvider,
                                               helper: AddToListHelper,
                                               config: AppConfig,
                                               val controllerComponents: MessagesControllerComponents,
                                               view: AddPartnerView
                                             )(implicit val executionContext: ExecutionContext)
  extends FrontendBaseController
    with Retrievals
    with I18nSupport {

  private val form: Form[Boolean] = formProvider()

  def onPageLoad(index: Int,mode: Mode): Action[AnyContent] =
    (authenticate andThen getData andThen requireData()) {
      implicit request =>

        val partners: Seq[PartnerEntity] = request.userAnswers.allPartnersAfterDelete(index)
        Ok(view(
          form,
          existingSchemeName.getOrElse("Scheme"),
          partners.size,
          config.maxPartners,
          partners,
          utils.Radios.yesNo(form("value")),
          routes.AddPartnersController.onSubmit(index, mode)
        ))
    }

  def onSubmit(index: Int,mode: Mode): Action[AnyContent] =
    (authenticate andThen getData andThen requireData()).async {
      implicit request =>
        val partners = request.userAnswers.allPartnersAfterDelete(index)
        if (partners.isEmpty || partners.lengthCompare(config.maxPartners) >= 0) {
          Future.successful(Redirect(
            navigator.nextPage(
              id          = AddPartnersId(index),
              userAnswers = request.userAnswers
            )
          ))
        }
        else {
          form.bindFromRequest().fold(
            formWithErrors =>
              Future.successful(BadRequest(view(
                formWithErrors,
                existingSchemeName.getOrElse("Scheme"),
                partners.size,
                config.maxPartners,
                partners,
                utils.Radios.yesNo(formWithErrors("value")),
                routes.AddPartnersController.onSubmit(index, mode)
              ))),
            value => {

              val ua = request.userAnswers.set(AddPartnersId(index),value).getOrElse(request.userAnswers)
              Future.successful(Redirect(
                navigator.nextPage(
                  id          = AddPartnersId(index),
                  userAnswers = ua
                )
              ))
            }
          )
        }
    }
}
