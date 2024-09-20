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

package controllers.establishers.partnership.contact

import controllers.Retrievals
import controllers.actions.{AuthAction, DataRequiredAction, DataRetrievalAction}
import controllers.establishers.partnership.contact.routes.EnterEmailController
import helpers.cya.MandatoryAnswerMissingException
import identifiers.beforeYouStart.SchemeNameId
import identifiers.establishers.partnership.PartnershipDetailsId
import models.{Index, NormalMode}
import play.api.mvc.{Action, AnyContent}
import services.common.contact.CommonWhatYouWillNeedContactService
import viewmodels.Message

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class WhatYouWillNeedController @Inject()(
                                           authenticate: AuthAction,
                                           getData: DataRetrievalAction,
                                           requireData: DataRequiredAction,
                                           common: CommonWhatYouWillNeedContactService
                                         )(implicit val ec: ExecutionContext)
  extends Retrievals {

  def onPageLoad(index: Index): Action[AnyContent] = {
    (authenticate andThen getData andThen requireData()).async {
      implicit request =>
        PartnershipDetailsId(index).retrieve.map {
          details =>
            common.get(
              name = details.partnershipName,
              pageHeading = Message("messages__title_partnership"),
              entityType = Message("messages__partnership"),
              continueUrl = EnterEmailController.onPageLoad(index, NormalMode).url,
              schemeName = request.userAnswers.get(SchemeNameId).getOrElse(throw MandatoryAnswerMissingException(SchemeNameId.toString))
            )
        }
    }
  }
}
