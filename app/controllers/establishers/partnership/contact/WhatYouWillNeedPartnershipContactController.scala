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

package controllers.establishers.partnership.contact

import controllers.Retrievals
import controllers.actions.{AuthAction, DataRequiredAction, DataRetrievalAction}
import controllers.establishers.partnership.contact
import helpers.cya.MandatoryAnswerMissingException
import identifiers.beforeYouStart.SchemeNameId
import identifiers.establishers.partnership.PartnershipDetailsId
import models.{Index, NormalMode}
import play.api.i18n.I18nSupport
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import renderer.Renderer
import uk.gov.hmrc.nunjucks.NunjucksSupport
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.Message

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class WhatYouWillNeedPartnershipContactController @Inject()(
                                                             authenticate: AuthAction,
                                                             getData: DataRetrievalAction,
                                                             requireData: DataRequiredAction,
                                                             val renderer: Renderer,
                                                             val controllerComponents: MessagesControllerComponents
                                                           ) (implicit val ec: ExecutionContext)
  extends FrontendBaseController
    with I18nSupport
    with Retrievals
    with NunjucksSupport {

  def onPageLoad(index: Index): Action[AnyContent] = {
    (authenticate andThen getData andThen requireData).apply {
      implicit request =>       PartnershipDetailsId(index).retrieve.right.map {
        details =>
          renderer.render(
            template = "whatYouWillNeedContact.njk",
            ctx = Json.obj(
              "titleValue"-> (Message("messages__establisherPartnershipContactDetails__whatYouWillNeed_title")).resolve,
              "name"        -> details.partnershipDetails,
              "continueUrl" -> EnterEmailController.onPageLoad(index, NormalMode).url,
              "schemeName"  -> request.userAnswers.get(SchemeNameId).getOrElse(throw MandatoryAnswerMissingException)
            )
          ).map(Ok(_))
    }
  }


}
