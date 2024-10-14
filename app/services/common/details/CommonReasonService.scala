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

package services.common.details

import connectors.cache.UserAnswersCacheConnector
import identifiers.TypedIdentifier
import models.Mode
import models.requests.DataRequest
import navigators.CompoundNavigator
import play.api.data.Form
import play.api.data.FormBinding.Implicits._
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.Results.{BadRequest, Ok, Redirect}
import play.api.mvc.{AnyContent, Call, MessagesControllerComponents, Result}
import uk.gov.hmrc.nunjucks.NunjucksSupport
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendHeaderCarrierProvider
import utils.UserAnswers
import views.html.ReasonView

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

@Singleton
class CommonReasonService @Inject()(val controllerComponents: MessagesControllerComponents,
                                    val userAnswersCacheConnector: UserAnswersCacheConnector,
                                    val navigator: CompoundNavigator,
                                    val messagesApi: MessagesApi,
                                    reasonView: ReasonView
                                   ) extends FrontendHeaderCarrierProvider with I18nSupport {

  def get(pageTitle: String,
          pageHeading: String,
          isPageHeading: Boolean,
          id: TypedIdentifier[String],
          form: Form[String],
          schemeName: String,
          submitUrl: Call
         )(implicit request: DataRequest[AnyContent], ec: ExecutionContext): Future[Result] = {

    val filledForm = request.userAnswers.get[String](id).fold(form)(form.fill)
    Future.successful(Ok(reasonView(
      pageTitle, pageHeading, isPageHeading, filledForm, schemeName, submitUrl
    )))
  }

  def post(pageTitle: String,
           pageHeading: String,
           isPageHeading: Boolean,
           id: TypedIdentifier[String],
           form: Form[String],
           schemeName: String,
           mode: Mode,
           optSetUserAnswers: Option[String => Try[UserAnswers]] = None,
           submitUrl: Call
          )(implicit request: DataRequest[AnyContent], ec: ExecutionContext): Future[Result] =

    form.bindFromRequest().fold(
      (formWithErrors: Form[String]) =>
          Future.successful(BadRequest(reasonView(
            pageTitle, pageHeading, isPageHeading, formWithErrors, schemeName, submitUrl
          ))),
      value => {
        def defaultSetUserAnswers = (value: String) =>
          request.userAnswers.set(id, value)

        val setUserAnswers = optSetUserAnswers.getOrElse(defaultSetUserAnswers)

        for {
          updatedAnswers <- Future.fromTry(setUserAnswers(value))
          _ <- userAnswersCacheConnector.save(request.lock, updatedAnswers.data)
        } yield
          Redirect(navigator.nextPage(id, updatedAnswers, mode))
      }
    )
}