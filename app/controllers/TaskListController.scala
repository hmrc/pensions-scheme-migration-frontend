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

package controllers

import connectors.cache.UserAnswersCacheConnector
import controllers.actions.{AuthAction, DataRequiredAction, DataRetrievalAction}
import forms.beforeYouStart.SchemeNameFormProvider
import identifiers.beforeYouStart.SchemeNameId
import navigators.CompoundNavigator
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.beforeYouStart.schemeName

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class TaskListController @Inject()(
                                    override val messagesApi: MessagesApi,
                                    userAnswersCacheConnector: UserAnswersCacheConnector,
                                    navigator: CompoundNavigator,
                                    authenticate: AuthAction,
                                    getData: DataRetrievalAction,
                                    requireData: DataRequiredAction,
                                    formProvider: SchemeNameFormProvider,
                                    val controllerComponents: MessagesControllerComponents,
                                    val view: schemeName
                                  )(implicit val executionContext: ExecutionContext)
  extends FrontendBaseController
    with I18nSupport
    with Retrievals {

  private val form = formProvider()

  def onPageLoad: Action[AnyContent] = (authenticate andThen getData andThen requireData) {
    implicit request =>
      val preparedForm = request.userAnswers.get(SchemeNameId).fold(form)(v => form.fill(v))
      Ok(view(preparedForm, existingSchemeName))
  }
}
