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

package controllers

import controllers.actions.{AuthAction, DataRequiredAction, DataRetrievalAction}
import helpers.cya.CYAHelper
import identifiers.beforeYouStart.SchemeNameId
import models.{RacDac, Scheme}
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.SchemeLockedView

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class SchemeLockedController @Inject()(override val messagesApi: MessagesApi,
                                       authenticate: AuthAction,
                                       getData: DataRetrievalAction,
                                       requireData: DataRequiredAction,
                                       val controllerComponents: MessagesControllerComponents,
                                       schemeLockedView: SchemeLockedView
                                     )(implicit val executionContext: ExecutionContext) extends
  FrontendBaseController with I18nSupport {

  def onPageLoadScheme: Action[AnyContent] = (authenticate andThen getData andThen requireData()) {
    implicit request =>
        Ok(schemeLockedView(
          CYAHelper.getAnswer(SchemeNameId)(request.userAnswers, implicitly),
          Messages("messages__scheme"),
          controllers.preMigration.routes.ListOfSchemesController.onPageLoad(Scheme).url
        ))
  }

  def onPageLoadRacDac: Action[AnyContent] = (authenticate andThen getData andThen requireData()) {
    implicit request =>
        Ok(schemeLockedView(
          CYAHelper.getAnswer(SchemeNameId)(request.userAnswers, implicitly),
          Messages("messages__racdac"),
          controllers.preMigration.routes.ListOfSchemesController.onPageLoad(RacDac).url
        ))
  }
}

