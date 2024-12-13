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

package controllers.trustees.individual.contact

import controllers.Retrievals
import controllers.actions.{AuthAction, DataRequiredAction, DataRetrievalAction}
import helpers.cya.MandatoryAnswerMissingException
import identifiers.beforeYouStart.SchemeNameId
import identifiers.trustees.individual.TrusteeNameId
import models.{Index, NormalMode}
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.mvc.Results.Ok
import play.api.mvc.{Action, AnyContent}
import views.html.WhatYouWillNeedContactView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class WhatYouWillNeedController @Inject()(
                                           val messagesApi: MessagesApi,
                                           authenticate: AuthAction,
                                           getData: DataRetrievalAction,
                                           requireData: DataRequiredAction,
                                           view: WhatYouWillNeedContactView
                                         )(implicit val ec: ExecutionContext)
  extends Retrievals
    with I18nSupport {

  def onPageLoad(index: Index): Action[AnyContent] =
    (authenticate andThen getData andThen requireData()).async {
      implicit request =>
        TrusteeNameId(index).retrieve.map {
          personName =>
            Future.successful(Ok(view(
              pageHeading = Messages("messages__title_individual"),
              continueUrl = controllers.trustees.individual.contact.routes.EnterEmailController.onPageLoad(index, NormalMode).url,
              name = personName.fullName,
              schemeName = request.userAnswers.get(SchemeNameId).getOrElse(throw MandatoryAnswerMissingException(SchemeNameId.toString))
            )))
        }
    }
}
