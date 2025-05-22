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

package controllers.establishers

import controllers.Retrievals
import controllers.actions._
import controllers.establishers.routes.NoEstablishersController
import forms.establishers.AddEstablisherFormProvider
import identifiers.establishers.AddEstablisherId
import models.Establisher
import navigators.CompoundNavigator
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.establishers.AddEstablisherView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class AddEstablisherController @Inject()(
                                             override val messagesApi: MessagesApi,
                                             navigator: CompoundNavigator,
                                             authenticate: AuthAction,
                                             getData: DataRetrievalAction,
                                             requireData: DataRequiredAction,
                                             formProvider: AddEstablisherFormProvider,
                                             val controllerComponents: MessagesControllerComponents,
                                             view: AddEstablisherView
                                           )(implicit val ec: ExecutionContext)
  extends FrontendBaseController
    with Retrievals
    with I18nSupport {

  def onPageLoad: Action[AnyContent] =
    (authenticate andThen getData andThen requireData()) {
      implicit request =>
        val allEstablishers: Seq[Establisher[?]] = request.userAnswers.allEstablishersAfterDelete
        if (allEstablishers.isEmpty) {
          Redirect(NoEstablishersController.onPageLoad)
        } else {
          val form = formProvider(allEstablishers)
          Ok(view(
            form,
            existingSchemeName.getOrElse("Scheme"),
            allEstablishers.filterNot(_.isCompleted),
            allEstablishers.filter(_.isCompleted),
            utils.Radios.yesNo(form("value")),
            routes.AddEstablisherController.onSubmit
          ))
        }
    }

  def onSubmit: Action[AnyContent] =
    (authenticate andThen getData andThen requireData()).async {
      implicit request =>
        val allEstablishers = request.userAnswers.allEstablishersAfterDelete
        formProvider(allEstablishers).bindFromRequest().fold(
          formWithErrors =>
          Future.successful(BadRequest(view(
            formWithErrors,
            existingSchemeName.getOrElse("Scheme"),
            allEstablishers.filterNot(_.isCompleted),
            allEstablishers.filter(_.isCompleted),
            utils.Radios.yesNo(formWithErrors("value")),
            routes.AddEstablisherController.onSubmit
          ))),
          value =>
            Future.successful(Redirect(
              navigator.nextPage(
                id = AddEstablisherId(value),
                userAnswers = request.userAnswers
              )
            ))
        )
    }
}
