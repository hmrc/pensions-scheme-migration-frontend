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

package controllers.establishers.partnership.details

import controllers.Retrievals
import controllers.actions.{AuthAction, DataRequiredAction, DataRetrievalAction}
import controllers.establishers.partnership.details.routes.HaveUTRController
import helpers.cya.MandatoryAnswerMissingException
import identifiers.beforeYouStart.SchemeNameId
import identifiers.establishers.partnership.PartnershipDetailsId
import models.{Index, NormalMode}
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import renderer.Renderer
import uk.gov.hmrc.nunjucks.NunjucksSupport
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class WhatYouWillNeedController @Inject()(
                                           override val messagesApi: MessagesApi,
                                           authenticate: AuthAction,
                                           getData: DataRetrievalAction,
                                           requireData: DataRequiredAction,
                                           val controllerComponents: MessagesControllerComponents,
                                           val renderer: Renderer
                                         )(implicit val ec: ExecutionContext)
  extends FrontendBaseController
    with I18nSupport
    with Retrievals
    with NunjucksSupport {

  def onPageLoad(index: Index): Action[AnyContent] =
    (authenticate andThen getData andThen requireData()).async {
      implicit request =>
        PartnershipDetailsId(index).retrieve.map {
          details =>
            renderer.render(
              template = "establishers/partnership/details/whatYouWillNeed.njk",
              ctx = Json.obj(
                "name"        -> details.partnershipName,
                "pageTitle" -> Messages("messages__partnershipDetails__whatYouWillNeed_title"),
                "continueUrl" -> HaveUTRController.onPageLoad(index, NormalMode).url,
                "schemeName"  -> request.userAnswers.get(SchemeNameId).getOrElse(throw MandatoryAnswerMissingException(SchemeNameId.toString))
              )
            ).map(Ok(_))
        }
    }

}
