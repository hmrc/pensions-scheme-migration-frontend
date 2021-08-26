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

package controllers.establishers.company.director

import connectors.cache.UserAnswersCacheConnector
import controllers.Retrievals
import controllers.actions._
import forms.establishers.company.director.ConfirmDeleteDirectorFormProvider
import identifiers.establishers.company.director.{ConfirmDeleteDirectorId, DirectorNameId}
import models.Index
import navigators.CompoundNavigator
import play.api.data.Form
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import renderer.Renderer
import uk.gov.hmrc.nunjucks.NunjucksSupport
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import uk.gov.hmrc.viewmodels.{MessageInterpolators, Radios}
import utils.UserAnswers

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class ConfirmDeleteDirectorController @Inject()(override val messagesApi: MessagesApi,
                                                navigator: CompoundNavigator,
                                                 authenticate: AuthAction,
                                                 getData: DataRetrievalAction,
                                                 requireData: DataRequiredAction,
                                                 formProvider: ConfirmDeleteDirectorFormProvider,
                                                 val controllerComponents: MessagesControllerComponents,
                                                 userAnswersCacheConnector: UserAnswersCacheConnector,
                                                 renderer: Renderer
                                               )(implicit val executionContext: ExecutionContext
                                               )
  extends FrontendBaseController with I18nSupport with Retrievals with NunjucksSupport {

  def onPageLoad(establisherIndex: Index, directorIndex: Index): Action[AnyContent] =
    (authenticate andThen getData andThen requireData).async {
      implicit request =>
        DirectorNameId(establisherIndex, directorIndex).retrieve.right.map { director =>
          if (director.isDeleted) {
            Future.successful(Redirect(routes.AlreadyDeletedController.onPageLoad(establisherIndex, directorIndex)))
          } else {
            val json = Json.obj(
              "form" -> form(director.fullName),
              "titleMessage" -> msg"messages__confirmDeleteDirectors__title".resolve,
              "name" -> director.fullName,
              "hint" ->  Some(Messages(s"messages__confirmDeleteDirectors__companyHint")),
              "radios" -> Radios.yesNo(formProvider(director.fullName)(implicitly)("value")),
              "submitUrl" -> routes.ConfirmDeleteDirectorController.onSubmit(establisherIndex, directorIndex).url,
              "schemeName" -> existingSchemeName
            )
            renderer.render("delete.njk", json).map(Ok(_))
          }
        }getOrElse Future.successful(Redirect(controllers.routes.IndexController.onPageLoad()))
    }

  private def form(name: String)(implicit messages: Messages): Form[Boolean] = formProvider(name)

  def onSubmit(establisherIndex: Index, directorIndex: Index): Action[AnyContent] =
    (authenticate andThen getData andThen requireData).async {
      implicit request =>

        DirectorNameId(establisherIndex, directorIndex).retrieve.right.map { director =>

          form(director.fullName).bindFromRequest().fold(
            (formWithErrors: Form[_]) => {
              val json = Json.obj(
                "form" -> formWithErrors,
                "titleMessage" -> msg"messages__confirmDeleteDirectors__title".resolve,
                "name" ->  director.fullName,
                "hint" -> Some(Messages(s"messages__confirmDeleteDirectors__companyHint")),
                "radios" -> Radios.yesNo(formProvider(director.fullName)(implicitly)("value")),
                "submitUrl" -> routes.ConfirmDeleteDirectorController.onSubmit(establisherIndex, directorIndex).url,
                "schemeName" -> existingSchemeName
              )
              renderer.render("delete.njk", json).map(BadRequest(_))
            },
            value => {
              val deletionResult: Try[UserAnswers] = if (value) {
                request.userAnswers.set(DirectorNameId(establisherIndex, directorIndex),
                  director.copy (isDeleted = true))
              } else {
                Try(request.userAnswers)
              }
              Future.fromTry(deletionResult).flatMap { answers =>
                userAnswersCacheConnector.save(request.lock, answers.data).map { _ =>
                  Redirect(navigator.nextPage(ConfirmDeleteDirectorId(establisherIndex), answers))
                }
              }
            }

          )
        }
    }


}
