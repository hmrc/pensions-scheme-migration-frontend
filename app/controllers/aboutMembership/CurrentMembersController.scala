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

package controllers.aboutMembership

import connectors.cache.UserAnswersCacheConnector
import controllers.Retrievals
import controllers.actions._
import forms.aboutMembership.MembersFormProvider
import identifiers.aboutMembership.CurrentMembersId
import identifiers.beforeYouStart.SchemeNameId
import models.Members
import navigators.CompoundNavigator
import play.api.data.Form
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import renderer.Renderer
import uk.gov.hmrc.nunjucks.NunjucksSupport
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import uk.gov.hmrc.viewmodels.MessageInterpolators
import utils.Enumerable
import viewmodels.Message

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CurrentMembersController @Inject()(override val messagesApi: MessagesApi,
                                         userAnswersCacheConnector: UserAnswersCacheConnector,
                                         navigator: CompoundNavigator,
                                         authenticate: AuthAction,
                                         getData: DataRetrievalAction,
                                         requireData: DataRequiredAction,
                                         formProvider: MembersFormProvider,
                                         val controllerComponents: MessagesControllerComponents,
                                         renderer: Renderer
                                    )(implicit val executionContext: ExecutionContext) extends FrontendBaseController
  with I18nSupport with Retrievals with Enumerable.Implicits with NunjucksSupport {

  private def form(schemeName: String)(implicit messages: Messages): Form[Members] =
    formProvider(Message("currentMembers.error.required", schemeName))

  def onPageLoad: Action[AnyContent] = (authenticate andThen getData andThen requireData).async {
    implicit request =>
      SchemeNameId.retrieve.right.map { schemeName =>
        val preparedForm = request.userAnswers.get(CurrentMembersId) match {
          case None => form(schemeName)
          case Some(value) => form(schemeName).fill(value)
        }

        val json = Json.obj(
          "form" -> preparedForm,
          "schemeName" -> schemeName,
          "titleMessage" -> msg"currentMembers.title".withArgs(schemeName).resolve,
          "radios" -> Members.radios(preparedForm)
        )

        renderer.render("aboutMembership/members.njk", json).map(Ok(_))
      }
  }

  def onSubmit: Action[AnyContent] = (authenticate andThen getData andThen requireData).async {
    implicit request =>
      SchemeNameId.retrieve.right.map { schemeName =>
        form(schemeName).bindFromRequest().fold(
          (formWithErrors: Form[_]) => {
            val json = Json.obj(
              "form" -> formWithErrors,
              "schemeName" -> schemeName,
              "titleMessage" -> msg"currentMembers.title".withArgs(schemeName).resolve,
              "radios" -> Members.radios(formWithErrors)
            )

            renderer.render("aboutMembership/members.njk", json).map(BadRequest(_))
          },
          value =>
            for {
              updatedAnswers <- Future.fromTry(request.userAnswers.set(CurrentMembersId, value))
              _ <- userAnswersCacheConnector.save(request.lock, updatedAnswers.data)
            } yield Redirect(navigator.nextPage(CurrentMembersId, updatedAnswers))
        )
      }
  }


}
