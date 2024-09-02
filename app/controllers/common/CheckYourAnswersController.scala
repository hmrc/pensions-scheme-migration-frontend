/*
 * Copyright 2024 HM Revenue & Customs
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

package controllers.common

import controllers.Retrievals
import controllers.actions.{AuthAction, DataRequiredAction, DataRetrievalAction}
import helpers.cya.{CYAHelper, CommonCYAHelper}
import identifiers.beforeYouStart.SchemeNameId
import models.{Index, entities}
import models.entities.{EntityType, JourneyType, PensionManagementType}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import renderer.Renderer
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.Enumerable

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class CheckYourAnswersController @Inject()(
                                            override val messagesApi: MessagesApi,
                                            authenticate: AuthAction,
                                            getData: DataRetrievalAction,
                                            requireData: DataRequiredAction,
                                            cyaHelper: CommonCYAHelper,
                                            val controllerComponents: MessagesControllerComponents,
                                            renderer: Renderer
                                          )(implicit val ec: ExecutionContext)
  extends FrontendBaseController
    with Enumerable.Implicits
    with I18nSupport
    with Retrievals {

  def onPageLoad(index: Index,
                 pensionManagementType: PensionManagementType,
                 entityType: EntityType,
                 journeyType: JourneyType): Action[AnyContent] = page(index, pensionManagementType, entityType, None, journeyType)

  def onPageLoadWithRepresentative(index: Index,
                                   pensionManagementType: PensionManagementType,
                                   entityType: EntityType,
                                   entityRepresentativeIndex: Index): Action[AnyContent] =
    page(index, pensionManagementType, entityType, Some(entityRepresentativeIndex), entities.Details)

  private def page(index: Index,
                   pensionManagementType: PensionManagementType,
                   entityType: EntityType,
                   entityRepresentativeIndex: Option[Index],
                   journeyType: JourneyType): Action[AnyContent] =
    (authenticate andThen getData andThen requireData()).async {
      implicit request =>
        renderer.render(
          template = "check-your-answers.njk",
          ctx = Json.obj(
            "list"       -> cyaHelper.rows(index, pensionManagementType, entityType, entityRepresentativeIndex, journeyType),
            "schemeName" -> CYAHelper.getAnswer(SchemeNameId)(request.userAnswers, implicitly),
            "submitUrl"  -> controllers.common.routes.SpokeTaskListController.onPageLoad(index, pensionManagementType, entityType).url
          )
        ).map(Ok(_))
    }
}
