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

package controllers.trustees.company

import controllers.GenericTaskListController
import controllers.actions.{AuthAction, DataRequiredAction, DataRetrievalAction}
import helpers.SpokeCreationService
import identifiers.beforeYouStart.SchemeNameId
import identifiers.trustees.company.CompanyDetailsId
import models.Index
import models.requests.DataRequest
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import renderer.Renderer
import viewmodels.Message

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class TaskListController @Inject()(
                                           override val messagesApi: MessagesApi,
                                           authenticate: AuthAction,
                                           getData: DataRetrievalAction,
                                           requireData: DataRequiredAction,
                                           val controllerComponents: MessagesControllerComponents,
                                           spokeCreationService: SpokeCreationService,
                                           val renderer: Renderer
                                         )(implicit val executionContext: ExecutionContext)
  extends GenericTaskListController {

  private def name(index: Index)
                  (implicit request: DataRequest[AnyContent]): String =
    request
      .userAnswers
      .get(CompanyDetailsId(index))
      .fold(Message("messages__company"))(_.companyName)

  def onPageLoad(index: Index): Action[AnyContent] =
    (authenticate andThen getData andThen requireData()).async {
      implicit request =>
        SchemeNameId.retrieve.right.map {
          schemeName =>
            get(
              spokes = spokeCreationService.getTrusteeCompanySpokes(request.userAnswers, name(index), index),
              entityName = name(index),
              schemeName = schemeName,
              entityType = Message("messages__tasklist__trustee"),
              submitUrl = controllers.trustees.routes.AddTrusteeController.onPageLoad().url
            )
        }
    }
}
