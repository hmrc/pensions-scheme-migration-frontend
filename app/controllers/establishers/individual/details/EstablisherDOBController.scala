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

package controllers.establishers.individual.details

import connectors.cache.UserAnswersCacheConnector
import controllers.actions.{AuthAction, DataRequiredAction, DataRetrievalAction}
import controllers.dateOfBirth.DateOfBirthController
import forms.DOBFormProvider
import identifiers.beforeYouStart.SchemeNameId
import identifiers.establishers.individual.EstablisherNameId
import identifiers.establishers.individual.details.EstablisherDOBId
import models.{Index, Mode}
import navigators.CompoundNavigator
import play.api.data.Form
import play.api.i18n.{Messages, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import renderer.Renderer

import java.time.LocalDate
import javax.inject.Inject
import scala.concurrent.ExecutionContext

class EstablisherDOBController @Inject()(
                                          override val messagesApi: MessagesApi,
                                          val navigator: CompoundNavigator,
                                          authenticate: AuthAction,
                                          getData: DataRetrievalAction,
                                          requireData: DataRequiredAction,
                                          formProvider: DOBFormProvider,
                                          val controllerComponents: MessagesControllerComponents,
                                          val userAnswersCacheConnector: UserAnswersCacheConnector,
                                          val renderer: Renderer
                                        )(implicit val executionContext: ExecutionContext)
  extends DateOfBirthController {

  val form: Form[LocalDate] = formProvider()

  def onPageLoad(index: Index, mode: Mode): Action[AnyContent] =
    (authenticate andThen getData andThen requireData()).async {
      implicit request =>

        SchemeNameId.retrieve.right.map {
          schemeName =>
            get(
              dobId        = EstablisherDOBId(index),
              personNameId = EstablisherNameId(index),
              schemeName   = schemeName,
              entityType   = Messages("messages__individual")
            )
        }
    }

  def onSubmit(index: Index, mode: Mode): Action[AnyContent] =
    (authenticate andThen getData andThen requireData()).async {
      implicit request =>
        SchemeNameId.retrieve.right.map {
          schemeName =>
            post(
              dobId        = EstablisherDOBId(index),
              personNameId = EstablisherNameId(index),
              schemeName   = schemeName,
              entityType   = Messages("messages__individual"),
              mode         = mode
            )
        }
    }
}
