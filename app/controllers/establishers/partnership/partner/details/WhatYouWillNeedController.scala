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

package controllers.establishers.partnership.partner.details

import controllers.Retrievals
import controllers.actions._
import controllers.establishers.partnership.partner.routes._
import helpers.cya.MandatoryAnswerMissingException
import identifiers.beforeYouStart.SchemeNameId
import models.{Index, NormalMode}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.Results.Ok
import play.api.mvc.{Action, AnyContent}
import views.html.establishers.partnership.partner.WhatYouWillNeedView

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class WhatYouWillNeedController @Inject()(val messagesApi: MessagesApi,
                                          authenticate: AuthAction,
                                          getData: DataRetrievalAction,
                                          requireData: DataRequiredAction,
                                          view: WhatYouWillNeedView
                                         )(implicit val ec: ExecutionContext)
  extends I18nSupport with Retrievals {

  def onPageLoad(establisherIndex: Index): Action[AnyContent] =
    (authenticate andThen getData andThen requireData()){

      implicit request =>
        val partnerIndex = request.userAnswers.allPartners(establisherIndex).size
        Ok(view(
          PartnerNameController.onPageLoad(establisherIndex,partnerIndex, NormalMode).url,
          request.userAnswers.get(SchemeNameId).getOrElse(throw MandatoryAnswerMissingException(SchemeNameId.toString))
        )
        )
    }
}

