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

package controllers.beforeYouStartSpoke

import connectors.cache.UserAnswersCacheConnector
import controllers.Retrievals
import controllers.actions._
import forms.beforeYouStart.WorkingKnowledgeFormProvider
import identifiers.adviser._
import identifiers.beforeYouStart.WorkingKnowledgeId
import navigators.CompoundNavigator
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import uk.gov.hmrc.viewmodels.Radios
import utils.{Enumerable, TwirlMigration}
import views.html.beforeYouStart.WorkingKnowledgeView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class WorkingKnowledgeController @Inject()(
    override val messagesApi: MessagesApi,
    userAnswersCacheConnector: UserAnswersCacheConnector,
    navigator: CompoundNavigator,
    authenticate: AuthAction,
    getData: DataRetrievalAction,
    requireData: DataRequiredAction,
    formProvider: WorkingKnowledgeFormProvider,
    val controllerComponents: MessagesControllerComponents,
    workingKnowledgeView: WorkingKnowledgeView
)(implicit val executionContext: ExecutionContext) extends FrontendBaseController
  with I18nSupport with Retrievals with Enumerable.Implicits {

  private val form = formProvider()

  def onPageLoad: Action[AnyContent] = (authenticate andThen getData andThen requireData()) {
    implicit request =>
      val preparedForm = request.userAnswers.get(WorkingKnowledgeId).fold(form)(form.fill)
      Ok(workingKnowledgeView(
        preparedForm,
        existingSchemeName,
        routes.WorkingKnowledgeController.onSubmit,
        TwirlMigration.toTwirlRadios(Radios.yesNo(preparedForm("value")))
      ))
  }

  def onSubmit: Action[AnyContent] = (authenticate andThen getData andThen requireData()).async {
    implicit request =>
      form.bindFromRequest().fold(
        (formWithErrors: Form[_]) =>
          Future.successful(BadRequest(
            workingKnowledgeView(
              formWithErrors,
              existingSchemeName,
              routes.WorkingKnowledgeController.onSubmit,
              TwirlMigration.toTwirlRadios(Radios.yesNo(formWithErrors("value")))
            )
          )),
          value =>
            for {
              updatedAnswers <- {
                val updatedUA = {
                  if (!value) {
                    request.userAnswers.removeAll(Set(EnterEmailId, EnterPhoneId, AdviserNameId,
                      EnterPostCodeId, AddressListId, AddressId))
                  }else{
                    request.userAnswers
                  }
                }
                  Future.fromTry(updatedUA.set(WorkingKnowledgeId, value))
              }
              _ <- userAnswersCacheConnector.save(request.lock, updatedAnswers.data)
            } yield Redirect(navigator.nextPage(WorkingKnowledgeId, updatedAnswers))
      )
  }
}
