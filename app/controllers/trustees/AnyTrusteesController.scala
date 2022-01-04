/*
 * Copyright 2022 HM Revenue & Customs
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

import connectors.cache.UserAnswersCacheConnector
import controllers.Retrievals
import controllers.actions._
import forms.YesNoFormProvider
import identifiers.beforeYouStart.SchemeNameId
import identifiers.trustees.AnyTrusteesId
import navigators.CompoundNavigator
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import renderer.Renderer
import uk.gov.hmrc.nunjucks.NunjucksSupport
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import uk.gov.hmrc.viewmodels.Radios
import utils.{Enumerable, UserAnswers}
import viewmodels.Message

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class AnyTrusteesController @Inject()(navigator: CompoundNavigator,
                                      override val messagesApi: MessagesApi,
                                      authenticate: AuthAction,
                                      getData: DataRetrievalAction,
                                      requireData: DataRequiredAction,
                                      formProvider: YesNoFormProvider,
                                      userAnswersCacheConnector: UserAnswersCacheConnector,
                                      val controllerComponents: MessagesControllerComponents,
                                      renderer: Renderer
                                     )(implicit val executionContext: ExecutionContext) extends
  FrontendBaseController with I18nSupport with Retrievals with Enumerable.Implicits with NunjucksSupport {

  private val form = formProvider("messages__schemeTaskList__anyTrustee_error")

  def onPageLoad: Action[AnyContent] =
    (authenticate andThen getData andThen requireData()).async {
      implicit request => {
        val formWithData = request.userAnswers.get(AnyTrusteesId).fold(form)(form.fill)
        SchemeNameId.retrieve.right.map {
          schemeName =>
            val json: JsObject = Json.obj(
              "form" -> formWithData,
              "entityType" -> Message("messages__the_scheme"),
              "radios" -> Radios.yesNo(formWithData("value")),
              "schemeName" -> schemeName
            )
            renderer.render("trustees/anyTrustees.njk", json).map(Ok(_))
        }
      }
    }

  def onSubmit: Action[AnyContent] =
    (authenticate andThen getData andThen requireData()).async {
      implicit request =>
        form.bindFromRequest().fold(
          (formWithErrors: Form[_]) => {
            SchemeNameId.retrieve.right.map {
              schemeName =>
                val json: JsObject = Json.obj(
                  "form" -> formWithErrors,
                  "entityType" -> Message("messages__the_scheme"),
                  "radios" -> Radios.yesNo(formWithErrors("value")),
                  "schemeName" -> schemeName
                )
                renderer.render("trustees/anyTrustees.njk", json).map(BadRequest(_))
            }
          },
          value => {

            val ua: Try[UserAnswers] = request.userAnswers.set(AnyTrusteesId, value)

            for {
              updatedAnswers <- Future.fromTry(ua)
              _ <- userAnswersCacheConnector.save(request.lock, updatedAnswers.data)
            } yield
              Redirect(navigator.nextPage(AnyTrusteesId, updatedAnswers))
          }
        )
    }
}
