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
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendHeaderCarrierProvider
import utils.UserAnswers
import views.html.{HasReferenceValueView, HasReferenceValueWithHintView}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

@Singleton
class CommonHasReferenceValueService @Inject()(val controllerComponents: MessagesControllerComponents,
                                               hasReferenceValueWithHintView: HasReferenceValueWithHintView,
                                               hasReferenceValueView:HasReferenceValueView,
                                               val userAnswersCacheConnector: UserAnswersCacheConnector,
                                               val navigator: CompoundNavigator,
                                               val messagesApi: MessagesApi
                                              ) extends FrontendHeaderCarrierProvider with I18nSupport {

  def get(
           pageTitle: String,
           pageHeading: String,
           isPageHeading: Boolean,
           id: TypedIdentifier[Boolean],
           form: Form[Boolean],
           schemeName: String,
           paragraphText: Seq[String] = Seq(),
           legendClass: String = "govuk-fieldset__legend--s",
           submitCall: Call
         )(implicit request: DataRequest[AnyContent]): Future[Result] = {

    val preparedForm: Form[Boolean] =
      request.userAnswers.get[Boolean](id) match {
        case Some(value) => form.fill(value)
        case _ => form
      }

    Future.successful(Ok(
      if (paragraphText.nonEmpty){
        hasReferenceValueWithHintView(preparedForm, schemeName, pageTitle, pageHeading,
          utils.Radios.yesNo(preparedForm("value")), legendClass, paragraphText, submitCall)
      } else {
        hasReferenceValueView(
          preparedForm, schemeName, pageTitle, pageHeading,
          utils.Radios.yesNo(preparedForm("value")), legendClass, submitCall)
      }
    ))
  }
  def post(pageTitle: String,
           pageHeading: String,
           isPageHeading: Boolean,
           id: TypedIdentifier[Boolean],
           form: Form[Boolean],
           schemeName: String,
           paragraphText: Seq[String] = Seq(),
           legendClass: String = "govuk-fieldset__legend--s",
           mode: Mode,
           submitCall: Call,
           optSetUserAnswers: Option[Boolean => Try[UserAnswers]] = None
          )(implicit request: DataRequest[AnyContent], ec: ExecutionContext): Future[Result] = {

    form.bindFromRequest().fold(
      (formWithErrors: Form[Boolean]) =>
        Future.successful(BadRequest(
          if (paragraphText.nonEmpty){
            hasReferenceValueWithHintView(formWithErrors, schemeName, pageTitle, pageHeading,
              utils.Radios.yesNo(formWithErrors("value")), legendClass, paragraphText, submitCall)
          } else {
            hasReferenceValueView(
              formWithErrors, schemeName, pageTitle, pageHeading,
              utils.Radios.yesNo(formWithErrors("value")), legendClass, submitCall)
          }
        )),
      value => {
        def defaultSetUserAnswers = (value: Boolean) =>
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
}