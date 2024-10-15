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

package controllers.establishers.company.director

import connectors.cache.UserAnswersCacheConnector
import controllers.Retrievals
import controllers.actions._
import forms.establishers.ConfirmDeleteEstablisherFormProvider
import identifiers.establishers.company.OtherDirectorsId
import identifiers.establishers.company.director.{ConfirmDeleteDirectorId, DirectorNameId}
import models.Index
import navigators.CompoundNavigator
import play.api.data.Form
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import uk.gov.hmrc.viewmodels.MessageInterpolators
import utils.UserAnswers
import views.html.DeleteView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class ConfirmDeleteDirectorController @Inject()(override val messagesApi: MessagesApi,
                                                navigator: CompoundNavigator,
                                                authenticate: AuthAction,
                                                getData: DataRetrievalAction,
                                                requireData: DataRequiredAction,
                                                formProvider: ConfirmDeleteEstablisherFormProvider,
                                                val controllerComponents: MessagesControllerComponents,
                                                userAnswersCacheConnector: UserAnswersCacheConnector,
                                                deleteView: DeleteView
                                               )(implicit val executionContext: ExecutionContext
                                               )
  extends FrontendBaseController with I18nSupport with Retrievals {

  def onPageLoad(establisherIndex: Index, directorIndex: Index): Action[AnyContent] =
    (authenticate andThen getData andThen requireData()).async {
      implicit request =>
        DirectorNameId(establisherIndex, directorIndex).retrieve.map { director =>
          if (director.isDeleted) {
            Future.successful(Redirect(routes.AlreadyDeletedController.onPageLoad(establisherIndex, directorIndex)))
          } else {
            Future.successful(Ok(
              deleteView(
                form(director.fullName),
                msg"messages__confirmDeleteDirectors__title".resolve,
                director.fullName,
                Some(Messages(s"messages__confirmDeleteDirectors__companyHint")),
                utils.Radios.yesNo(formProvider(director.fullName)(implicitly)("value")),
                existingSchemeName.getOrElse(""),
                routes.ConfirmDeleteDirectorController.onSubmit(establisherIndex, directorIndex)
              )
            ))
          }
        } getOrElse {
          throw new RuntimeException("index page unavailable")
        }
    }

  def onSubmit(establisherIndex: Index, directorIndex: Index): Action[AnyContent] =
    (authenticate andThen getData andThen requireData()).async {
      implicit request =>

        DirectorNameId(establisherIndex, directorIndex).retrieve.map { director =>

          form(director.fullName).bindFromRequest().fold(
            (formWithErrors: Form[_]) => {
              Future.successful(BadRequest(
                deleteView(
                  formWithErrors,
                  msg"messages__confirmDeleteDirectors__title".resolve,
                  director.fullName,
                  Some(Messages(s"messages__confirmDeleteDirectors__companyHint")),
                  utils.Radios.yesNo(formProvider(director.fullName)(implicitly)("value")),
                  existingSchemeName.getOrElse(""),
                  routes.ConfirmDeleteDirectorController.onSubmit(establisherIndex, directorIndex)
                )
              ))
            },
            value => {
              val deletionResult: Try[UserAnswers] = if (value) {
                request.userAnswers.set(DirectorNameId(establisherIndex, directorIndex),
                  director.copy(isDeleted = true))
              } else {
                Try(request.userAnswers)
              }
              Future.fromTry(deletionResult).flatMap { answers =>
                val updatedUA = answers.remove(OtherDirectorsId(establisherIndex))
                userAnswersCacheConnector.save(request.lock, updatedUA.data).map { _ =>
                  Redirect(navigator.nextPage(ConfirmDeleteDirectorId(establisherIndex), updatedUA))
                }
              }
            }

          )
        }
    }

  private def form(name: String)(implicit messages: Messages): Form[Boolean] = formProvider(name)


}
