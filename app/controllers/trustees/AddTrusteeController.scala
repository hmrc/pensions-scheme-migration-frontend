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

package controllers.trustees

import config.AppConfig
import controllers.Retrievals
import controllers.actions._
import controllers.trustees.routes.{AnyTrusteesController, NoTrusteesController}
import forms.trustees.AddTrusteeFormProvider
import identifiers.beforeYouStart.SchemeTypeId
import identifiers.trustees.AddTrusteeId
import models.requests.DataRequest
import models.{SchemeType, Trustee}
import navigators.CompoundNavigator
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc._
import play.twirl.api.Html
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class AddTrusteeController @Inject()(override val messagesApi: MessagesApi,
                                     navigator: CompoundNavigator,
                                     authenticate: AuthAction,
                                     getData: DataRetrievalAction,
                                     requireData: DataRequiredAction,
                                     formProvider: AddTrusteeFormProvider,
                                     config: AppConfig,
                                     val controllerComponents: MessagesControllerComponents,
                                     view: views.html.trustees.AddTrusteeView
                                    )(implicit val ec: ExecutionContext)
  extends FrontendBaseController with Retrievals with I18nSupport {

  def onPageLoad: Action[AnyContent] =
    (authenticate andThen getData andThen requireData()).async {
      implicit request =>
        val trustees = request.userAnswers.allTrusteesAfterDelete
        val isSingleTrust = request.userAnswers.get(SchemeTypeId).contains(SchemeType.SingleTrust)

        (trustees, isSingleTrust) match {
          case (Nil, false) => navTo(AnyTrusteesController.onPageLoad)
          case (Nil, true) => navTo(NoTrusteesController.onPageLoad)
          case _ =>
            Future.successful(Ok(getView(formProvider(trustees), trustees)))
        }
    }

  def onSubmit: Action[AnyContent] = (authenticate andThen getData andThen requireData()).async {
    implicit request =>
      def navNextPage(v: Option[Boolean]): Future[Result] =
        Future.successful(Redirect(navigator.nextPage(AddTrusteeId(v), request.userAnswers)))

      val trustees = request.userAnswers.allTrusteesAfterDelete
      val formWithErrors = formProvider(trustees).bindFromRequest()

      (formWithErrors.value, trustees.length) match {
        case (Some(v), _) => navNextPage(v)
        case (_, numberOfTrustees) if numberOfTrustees >= config.maxTrustees => navNextPage(None)
        case _ =>
          Future.successful(BadRequest(getView(formWithErrors, trustees)))
      }
  }

  private def getView(form: Form[?], trustees: Seq[Trustee[?]])(implicit request: DataRequest[AnyContent]): Html = {
    val trusteesComplete = trustees.filter(_.isCompleted)
    val trusteesIncomplete = trustees.filterNot(_.isCompleted)

    view(
      form,
      controllers.trustees.routes.AddTrusteeController.onSubmit,
      existingSchemeName.getOrElse(throw new RuntimeException("Scheme name not available")),
      trusteesIncomplete,
      trusteesComplete,
      trustees.size,
      config.maxTrustees,
      utils.Radios.yesNo(form("value"))
    )
  }

  private def navTo(call: Call): Future[Result] = Future.successful(Redirect(call))
}
