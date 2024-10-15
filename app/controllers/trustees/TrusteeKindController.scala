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

package controllers.trustees

import controllers.Retrievals
import controllers.actions._
import forms.trustees.TrusteeKindFormProvider
import identifiers.trustees.TrusteeKindId
import models.Index
import models.trustees.TrusteeKind
import navigators.CompoundNavigator
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.Enumerable

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class TrusteeKindController @Inject()(
                                           override val messagesApi: MessagesApi,
                                           navigator: CompoundNavigator,
                                           authenticate: AuthAction,
                                           getData: DataRetrievalAction,
                                           requireData: DataRequiredAction,
                                           formProvider: TrusteeKindFormProvider,
                                           val controllerComponents: MessagesControllerComponents,
                                           view: views.html.trustees.TrusteeKindView
                                         )(implicit val executionContext: ExecutionContext) extends
  FrontendBaseController with Retrievals with I18nSupport with Enumerable.Implicits {

  private val form = formProvider()

  def onPageLoad(index: Index): Action[AnyContent] =
    (authenticate andThen getData andThen requireData()) {
      implicit request =>
        val formWithData = request.userAnswers.get(TrusteeKindId(index, TrusteeKind.Company)).fold(form)(form.fill)
        Ok(view(
          formWithData,
          controllers.trustees.routes.TrusteeKindController.onSubmit(index),
          existingSchemeName.getOrElse(throw new RuntimeException("Scheme name not available")),
          TrusteeKind.radios(formWithData)
        ))
    }

  def onSubmit(index: Index): Action[AnyContent] = (authenticate andThen getData andThen requireData()) {
    implicit request =>
      form.bindFromRequest().fold(
        (formWithErrors: Form[_]) => {
          BadRequest(view(
            formWithErrors,
            controllers.trustees.routes.TrusteeKindController.onSubmit(index),
            existingSchemeName.getOrElse(throw new RuntimeException("Scheme name not available")),
            TrusteeKind.radios(formWithErrors)
          ))
        },
        value => {
          Redirect(navigator.nextPage(TrusteeKindId(index, value), request.userAnswers))
        }
      )
  }

}
