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

package controllers.beforeYouStartSpoke

import connectors.cache.UserAnswersCacheConnector
import controllers.Retrievals
import controllers.actions._
import forms.beforeYouStart.EstablishedCountryFormProvider
import identifiers.beforeYouStart.{EstablishedCountryId, SchemeNameId}
import navigators.CompoundNavigator
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.CountryOptions
import views.html.beforeYouStart.establishedCountry

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class EstablishedCountryController @Inject()(override val messagesApi: MessagesApi,
                                             userAnswersCacheConnector: UserAnswersCacheConnector,
                                             navigator: CompoundNavigator,
                                             authenticate: AuthAction,
                                             getData: DataRetrievalAction,
                                             requireData: DataRequiredAction,
                                             formProvider: EstablishedCountryFormProvider,
                                             countryOptions: CountryOptions,
                                             val controllerComponents: MessagesControllerComponents,
                                             val view: establishedCountry
                                            )(implicit val executionContext: ExecutionContext) extends
  FrontendBaseController with I18nSupport with Retrievals {

  private val form = formProvider()

  def onPageLoad: Action[AnyContent] = (authenticate andThen getData andThen requireData).async {
    implicit request =>
      SchemeNameId.retrieve.right.map { schemeName =>
        val preparedForm = request.userAnswers.get(EstablishedCountryId) match {
          case None => form
          case Some(value) => form.fill(value)
        }
        Future.successful(Ok(view(preparedForm, countryOptions.options, schemeName)))
      }
  }

  def onSubmit: Action[AnyContent] = (authenticate andThen getData andThen requireData).async {
    implicit request =>
      form.bindFromRequest().fold(
        (formWithErrors: Form[_]) =>
          SchemeNameId.retrieve.right.map { schemeName =>
            Future.successful(BadRequest(view(formWithErrors, countryOptions.options, schemeName)))
          },
        value =>
            for {
              updatedAnswers <- Future.fromTry(request.userAnswers.set(EstablishedCountryId, value))
              _ <- userAnswersCacheConnector.save(request.lock, updatedAnswers.data)
            } yield Redirect(navigator.nextPage(EstablishedCountryId, updatedAnswers))
      )
  }
}
