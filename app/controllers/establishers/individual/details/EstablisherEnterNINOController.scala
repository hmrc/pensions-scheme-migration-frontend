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

package controllers.establishers.individual.details

import connectors.cache.UserAnswersCacheConnector
import controllers.EnterReferenceValueController
import controllers.actions.{AuthAction, DataRequiredAction, DataRetrievalAction}
import forms.NINOFormProvider
import identifiers.beforeYouStart.SchemeNameId
import identifiers.establishers.individual.EstablisherNameId
import identifiers.establishers.individual.details.EstablisherNINOId
import models.requests.DataRequest
import models.{Index, Mode, ReferenceValue}
import navigators.CompoundNavigator
import play.api.data.Form
import play.api.i18n.{Messages, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import renderer.Renderer

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class EstablisherEnterNINOController @Inject()(
                                                override val messagesApi: MessagesApi,
                                                val navigator: CompoundNavigator,
                                                authenticate: AuthAction,
                                                getData: DataRetrievalAction,
                                                requireData: DataRequiredAction,
                                                formProvider: NINOFormProvider,
                                                val controllerComponents: MessagesControllerComponents,
                                                val userAnswersCacheConnector: UserAnswersCacheConnector,
                                                val renderer: Renderer
                                              )(implicit val executionContext: ExecutionContext)
  extends EnterReferenceValueController {

  private def name(index: Index)
                  (implicit request: DataRequest[AnyContent]): String =
    request
      .userAnswers
      .get(EstablisherNameId(index))
      .fold("the establisher")(_.fullName)

  private def form(index: Index)
                  (implicit request: DataRequest[AnyContent]): Form[ReferenceValue] =
    formProvider(name(index))

  def onPageLoad(index: Index, mode: Mode): Action[AnyContent] =
    (authenticate andThen getData andThen requireData).async {
      implicit request =>
        SchemeNameId.retrieve.right.map {
          schemeName =>
            get(
              pageTitle     = Messages("messages__enterNINO", name(index)),
              isPageHeading = true,
              id            = EstablisherNINOId(index),
              form          = form(index),
              personName    = name(index),
              submitUrl     = routes.EstablisherEnterNINOController.onSubmit(index, mode).url,
              schemeName    = schemeName
            )
        }
    }

  def onSubmit(index: Index, mode: Mode): Action[AnyContent] =
    (authenticate andThen getData andThen requireData).async {
      implicit request =>
        SchemeNameId.retrieve.right.map {
          schemeName =>
            post(
              pageTitle     = Messages("messages__enterNINO", name(index)),
              isPageHeading = true,
              id            = EstablisherNINOId(index),
              form          = form(index),
              personName    = name(index),
              submitUrl     = routes.EstablisherEnterNINOController.onSubmit(index, mode).url,
              schemeName    = schemeName
            )
        }
    }
}
