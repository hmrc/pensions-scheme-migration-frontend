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

package controllers.trustees.company.contacts

import controllers.Retrievals
import controllers.actions.{AuthAction, DataRequiredAction, DataRetrievalAction}
import controllers.trustees.company.contacts.routes.EnterEmailController
import helpers.cya.MandatoryAnswerMissingException
import identifiers.beforeYouStart.SchemeNameId
import identifiers.trustees.company.CompanyDetailsId
import models.{Index, NormalMode}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import renderer.Renderer
import uk.gov.hmrc.nunjucks.NunjucksSupport
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.Message

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class WhatYouWillNeedCompanyContactController @Inject()(
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
        CompanyDetailsId(index).retrieve.right.map {
          details =>
            renderer.render(
              template = "whatYouWillNeedContact.njk",
              ctx = Json.obj(
                "name"        -> details.companyName,
                "pageHeading" -> Message("messages__title_company"),
                "entityType" -> Message("messages__company"),
                "continueUrl" -> EnterEmailController.onPageLoad(index, NormalMode).url,
                "schemeName"  -> request.userAnswers.get(SchemeNameId).getOrElse(throw MandatoryAnswerMissingException)
              )
            ).map(Ok(_))
        }
    }

}
