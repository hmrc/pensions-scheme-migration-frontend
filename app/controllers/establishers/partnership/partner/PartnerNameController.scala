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

package controllers.establishers.partnership.partner

import connectors.cache.UserAnswersCacheConnector
import controllers.Retrievals
import controllers.actions._
import forms.PersonNameFormProvider
import identifiers.establishers.partnership.partner.PartnerNameId
import models.{Index, Mode, PersonName}
import navigators.CompoundNavigator
import play.api.data.Form
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.PersonNameView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class PartnerNameController @Inject()(
                                        override val messagesApi: MessagesApi,
                                        navigator: CompoundNavigator,
                                        authenticate: AuthAction,
                                        getData: DataRetrievalAction,
                                        requireData: DataRequiredAction,
                                        formProvider: PersonNameFormProvider,
                                        val controllerComponents: MessagesControllerComponents,
                                        userAnswersCacheConnector: UserAnswersCacheConnector,
                                        personNameView: PersonNameView
                                      )(implicit val executionContext: ExecutionContext)
  extends FrontendBaseController
  with Retrievals
  with I18nSupport
  {

  private def form(implicit messages: Messages): Form[PersonName] = formProvider("messages__error__partner")

  def onPageLoad(establisherIndex: Index, partnerIndex: Index, mode: Mode): Action[AnyContent] =
    (authenticate andThen getData andThen requireData()).async {
      implicit request =>
        Future.successful(Ok(
          personNameView(
            request.userAnswers.get[PersonName](PartnerNameId(establisherIndex, partnerIndex)).fold(form)(form.fill),
            existingSchemeName.getOrElse(""),
            Messages("messages__partner"),
            routes.PartnerNameController.onSubmit(establisherIndex, partnerIndex, mode)
          )
        ))
    }

  def onSubmit(establisherIndex: Index, partnerIndex: Index, mode: Mode): Action[AnyContent] =
    (authenticate andThen getData andThen requireData()).async {
      implicit request =>
        form.bindFromRequest().fold(
          (formWithErrors: Form[?]) =>
            Future.successful(BadRequest(
              personNameView(
                formWithErrors,
                existingSchemeName.getOrElse(""),
                Messages("messages__partner"),
                routes.PartnerNameController.onSubmit(establisherIndex, partnerIndex, mode)
              )
            )),
          value =>
            for {
              updatedAnswers <- Future.fromTry(request.userAnswers.set(PartnerNameId(establisherIndex, partnerIndex), value))
              _              <- userAnswersCacheConnector.save(request.lock, updatedAnswers.data)
            } yield
              Redirect(navigator.nextPage(PartnerNameId(establisherIndex, partnerIndex), updatedAnswers, mode))
        )
    }
}

