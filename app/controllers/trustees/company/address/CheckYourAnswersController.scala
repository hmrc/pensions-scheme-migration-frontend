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

package controllers.trustees.company.address

import controllers.Retrievals
import controllers.actions.{AuthAction, DataRequiredAction, DataRetrievalAction}
import helpers.cya.CYAHelper
import helpers.cya.trustees.company.TrusteeAddressCYAHelper
import identifiers.beforeYouStart.SchemeNameId
import models.Index
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
                                            cyaHelper: TrusteeAddressCYAHelper,
                                            val controllerComponents: MessagesControllerComponents,
                                            renderer: Renderer
                                          )(implicit val ec: ExecutionContext)
  extends FrontendBaseController
    with Enumerable.Implicits
    with I18nSupport
    with Retrievals {

  def onPageLoad(index: Index): Action[AnyContent] =
    (authenticate andThen getData andThen requireData()).async {
      implicit request =>
        renderer.render(
          template = "check-your-answers.njk",
          ctx = Json.obj(
            "list"       -> cyaHelper.rows(index),
            "schemeName" -> CYAHelper.getAnswer(SchemeNameId)(request.userAnswers, implicitly),
            "submitUrl"  -> controllers.trustees.company.routes.SpokeTaskListController.onPageLoad(index).url
          )
        ).map(Ok(_))
    }
}
