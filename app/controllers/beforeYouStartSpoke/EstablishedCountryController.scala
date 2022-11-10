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

package controllers.beforeYouStartSpoke

import config.AppConfig
import connectors.cache.UserAnswersCacheConnector
import controllers.Retrievals
import controllers.actions._
import forms.beforeYouStart.EstablishedCountryFormProvider
import helpers.CountriesHelper
import identifiers.beforeYouStart.{EstablishedCountryId, SchemeNameId}
import navigators.CompoundNavigator
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import renderer.Renderer
import uk.gov.hmrc.nunjucks.NunjucksSupport
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class EstablishedCountryController @Inject()( config: AppConfig,
                                              override val messagesApi: MessagesApi,
                                              userAnswersCacheConnector: UserAnswersCacheConnector,
                                              navigator: CompoundNavigator,
                                              authenticate: AuthAction,
                                              getData: DataRetrievalAction,
                                              requireData: DataRequiredAction,
                                              formProvider: EstablishedCountryFormProvider,
                                              val controllerComponents: MessagesControllerComponents,
                                              renderer: Renderer
                                            )(implicit val executionContext: ExecutionContext)
  extends FrontendBaseController
    with I18nSupport
    with Retrievals
    with NunjucksSupport
    with CountriesHelper {

  private val form = formProvider()

  def onPageLoad: Action[AnyContent] = (authenticate andThen getData andThen requireData()).async {
    implicit request =>
      SchemeNameId.retrieve.map { schemeName =>
        val preparedForm = request.userAnswers.get(EstablishedCountryId) match {
          case None => form
          case Some(value) => form.fill(value)
        }
        val json = Json.obj(
          "form" -> preparedForm,
          "schemeName" -> schemeName,
          "countries" ->   jsonCountries(preparedForm.data.get("value"), config)
        )

        renderer.render("beforeYouStart/establishedCountry.njk", json).map(Ok(_))
      }
  }

  def onSubmit: Action[AnyContent] = (authenticate andThen getData andThen requireData()).async {
    implicit request =>
      form.bindFromRequest().fold(
        (formWithErrors: Form[_]) =>
          SchemeNameId.retrieve.map { schemeName =>
            val json = Json.obj(
              "form" -> formWithErrors,
              "schemeName" -> schemeName,
              "countries" ->   jsonCountries(form.data.get("country"), config)
            )

            renderer.render("beforeYouStart/establishedCountry.njk", json).map(BadRequest(_))
          },
        value =>
          for {
            updatedAnswers <- Future.fromTry(request.userAnswers.set(EstablishedCountryId, value))
            _ <- userAnswersCacheConnector.save(request.lock, updatedAnswers.data)
          } yield Redirect(navigator.nextPage(EstablishedCountryId, updatedAnswers))
      )
  }
}
