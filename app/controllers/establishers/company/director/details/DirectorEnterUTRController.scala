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

package controllers.establishers.company.director.details

import connectors.cache.UserAnswersCacheConnector
import controllers.EnterReferenceValueController
import controllers.actions._
import forms.UTRFormProvider
import identifiers.establishers.company.director.{DirectorEnterUTRId, DirectorNameId}
import models.requests.DataRequest
import models.{Index, Mode, ReferenceValue}
import navigators.CompoundNavigator
import play.api.data.Form
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import renderer.Renderer
import viewmodels.Message

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class DirectorEnterUTRController @Inject()(
                                            override val messagesApi: MessagesApi,
                                            val navigator: CompoundNavigator,
                                            authenticate: AuthAction,
                                            getData: DataRetrievalAction,
                                            requireData: DataRequiredAction,
                                            formProvider: UTRFormProvider,
                                            val controllerComponents: MessagesControllerComponents,
                                            val userAnswersCacheConnector: UserAnswersCacheConnector,
                                            val renderer: Renderer
                                          )(implicit val executionContext: ExecutionContext)
  extends EnterReferenceValueController {

  private def name(establisherIndex: Index, directorIndex: Index)
                  (implicit request: DataRequest[AnyContent]): String =
    request
      .userAnswers
      .get(DirectorNameId(establisherIndex,directorIndex))
      .fold("the director")(_.fullName)

  private def form: Form[ReferenceValue] = formProvider()

  def onPageLoad(establisherIndex: Index, directorIndex: Index, mode: Mode): Action[AnyContent] =
    (authenticate andThen getData andThen requireData).async {
      implicit request =>
        get(
          pageTitle     = Message("messages__enterUTR_title", Message("messages__director")),
          pageHeading     = Message("messages__enterUTR", name(establisherIndex,directorIndex)),
          isPageHeading = false,
          id            = DirectorEnterUTRId(establisherIndex, directorIndex),
          form          = form,
          schemeName    = existingSchemeName.toString,
          legendClass   = "govuk-visually-hidden",
          paragraphText = Seq(Message("messages__UTR__p1"), Message("messages__UTR__p2"))
        )
    }

  def onSubmit( establisherIndex: Index, directorIndex: Index, mode: Mode): Action[AnyContent] =
    (authenticate andThen getData andThen requireData).async {
      implicit request =>
        post(
          pageTitle     = Message("messages__enterUTR_title", Message("messages__director")),
          pageHeading     = Message("messages__enterUTR",name(establisherIndex,directorIndex)),
          isPageHeading = false,
          id            = DirectorEnterUTRId(establisherIndex, directorIndex),
          form          = form,
          schemeName    = existingSchemeName.toString,
          legendClass   = "govuk-visually-hidden",
          paragraphText = Seq(Message("messages__UTR__p1"), Message("messages__UTR__p2")),
          mode          = mode
        )
    }
}
