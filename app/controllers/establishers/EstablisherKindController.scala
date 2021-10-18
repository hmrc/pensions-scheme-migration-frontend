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

package controllers.establishers

import connectors.cache.UserAnswersCacheConnector
import controllers.Retrievals
import controllers.actions._
import forms.establishers.EstablisherKindFormProvider
import identifiers.establishers.{EstablisherKindId, IsEstablisherNewId}
import models.Index
import models.establishers.EstablisherKind
import navigators.CompoundNavigator
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import renderer.Renderer
import uk.gov.hmrc.nunjucks.NunjucksSupport
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.{Enumerable, UserAnswers}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class EstablisherKindController @Inject()(
                                           override val messagesApi: MessagesApi,
                                           navigator: CompoundNavigator,
                                           authenticate: AuthAction,
                                           getData: DataRetrievalAction,
                                           requireData: DataRequiredAction,
                                           formProvider: EstablisherKindFormProvider,
                                           val controllerComponents: MessagesControllerComponents,
                                           userAnswersCacheConnector: UserAnswersCacheConnector,
                                           renderer: Renderer
                                         )(implicit val executionContext: ExecutionContext) extends
  FrontendBaseController with Retrievals with I18nSupport with Enumerable.Implicits with NunjucksSupport {

  private val form = formProvider()

  def onPageLoad(index: Index): Action[AnyContent] =
    (authenticate andThen getData andThen requireData()).async {
      implicit request =>
        val formWithData = request.userAnswers.get(EstablisherKindId(index)).fold(form)(form.fill)
        val json = Json.obj(
          "form" -> formWithData,
          "schemeName" -> existingSchemeName,
          "radios" -> EstablisherKind.radios(formWithData)
        )
        renderer.render("establishers/establisherKind.njk", json).map(Ok(_))
    }

  def onSubmit(index: Index): Action[AnyContent] = (authenticate andThen getData andThen requireData()).async {
    implicit request =>
      form.bindFromRequest().fold(
        (formWithErrors: Form[_]) => {
          val json = Json.obj(
            "form" -> formWithErrors,
            "schemeName" -> existingSchemeName,
            "radios" -> EstablisherKind.radios(formWithErrors)
          )
          renderer.render("establishers/establisherKind.njk", json).map(BadRequest(_))
        },
        value => {

          val ua: Try[UserAnswers] = request.userAnswers.set(IsEstablisherNewId(index), value = true).flatMap(_.set(EstablisherKindId(index), value))

          for {
            updatedAnswers <- Future.fromTry(ua)
            _ <- userAnswersCacheConnector.save(request.lock, updatedAnswers.data)
          } yield
            Redirect(navigator.nextPage(EstablisherKindId(index), updatedAnswers))
        }
      )
  }

}
