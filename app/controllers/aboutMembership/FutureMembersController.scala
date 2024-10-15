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

package controllers.aboutMembership

import connectors.cache.UserAnswersCacheConnector
import controllers.Retrievals
import controllers.actions._
import forms.aboutMembership.MembersFormProvider
import identifiers.aboutMembership.FutureMembersId
import identifiers.beforeYouStart.SchemeNameId
import models.Members
import navigators.CompoundNavigator
import play.api.data.Form
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.Enumerable
import viewmodels.Message
import views.html.aboutMembership.MembersView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class FutureMembersController @Inject()(override val messagesApi: MessagesApi,
                                         userAnswersCacheConnector: UserAnswersCacheConnector,
                                         navigator: CompoundNavigator,
                                         authenticate: AuthAction,
                                         getData: DataRetrievalAction,
                                         requireData: DataRequiredAction,
                                         formProvider: MembersFormProvider,
                                         val controllerComponents: MessagesControllerComponents,
                                         view : MembersView
                                        )(implicit val executionContext: ExecutionContext) extends FrontendBaseController
  with I18nSupport with Retrievals with Enumerable.Implicits {

  private def form(schemeName: String)(implicit messages: Messages): Form[Members] =
    formProvider(Message("futureMembers.error.required", schemeName))

  def onPageLoad: Action[AnyContent] = (authenticate andThen getData andThen requireData()).async {
    implicit request =>
      SchemeNameId.retrieve.map { schemeName =>
        val preparedForm = request.userAnswers.get(FutureMembersId) match {
          case None => form(schemeName)
          case Some(value) => form(schemeName).fill(value)
        }
        Future.successful(Ok(view(
          preparedForm,
          schemeName,
          Messages("futureMembers.title", Messages("messages__the_scheme")),
          Messages("futureMembers.title", schemeName),
          Members.radios(preparedForm),
          routes.FutureMembersController.onSubmit
        )))
      }
  }

  def onSubmit: Action[AnyContent] = (authenticate andThen getData andThen requireData()).async {
    implicit request =>
      SchemeNameId.retrieve.map { schemeName =>
        form(schemeName).bindFromRequest().fold(
          (formWithErrors: Form[_]) => {
            Future.successful(BadRequest(view(
              formWithErrors,
              schemeName,
              Messages("futureMembers.title", Messages("messages__the_scheme")),
              Messages("futureMembers.title", schemeName),
              Members.radios(formWithErrors),
              routes.FutureMembersController.onSubmit
            )))
          },
          value =>
            for {
              updatedAnswers <- Future.fromTry(request.userAnswers.set(FutureMembersId, value))
              _ <- userAnswersCacheConnector.save(request.lock, updatedAnswers.data)
            } yield Redirect(navigator.nextPage(FutureMembersId, updatedAnswers))
        )
      }
  }


}
