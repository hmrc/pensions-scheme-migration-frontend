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
import models.requests.DataRequest
import models.{Mode, ReferenceValue}
import navigators.CompoundNavigator
import play.api.data.Form
import play.api.data.FormBinding.Implicits._
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.Results.{BadRequest, Ok, Redirect}
import play.api.mvc.{AnyContent, Call, MessagesControllerComponents, Result}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendHeaderCarrierProvider
import utils.UserAnswers
import views.html.{EnterReferenceValueView, EnterReferenceValueWithHintView}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

@Singleton
class CommonEnterReferenceValueService @Inject()(val controllerComponents: MessagesControllerComponents,
                                                 val userAnswersCacheConnector: UserAnswersCacheConnector,
                                                 val navigator: CompoundNavigator,
                                                 val messagesApi: MessagesApi,
                                                 enterReferenceValueView: EnterReferenceValueView,
                                                 enterReferenceValueWithHintView: EnterReferenceValueWithHintView
                                                ) extends FrontendHeaderCarrierProvider with I18nSupport {

  def get(
           pageTitle: String,
           pageHeading: String,
           isPageHeading: Boolean,
           id: TypedIdentifier[ReferenceValue],
           form: Form[ReferenceValue],
           schemeName: String,
           hintText: Option[String] = None,
           paragraphText: Seq[String] = Seq(),
           legendClass: String = "govuk-fieldset__legend--s",
           submitCall: Call
         )(implicit request: DataRequest[AnyContent]): Future[Result] = {
    val filledForm = request.userAnswers.get[ReferenceValue](id).fold(form)(form.fill)

    Future.successful(Ok(
      if (paragraphText.nonEmpty || hintText.nonEmpty) {
        enterReferenceValueWithHintView(
          filledForm,
          schemeName,
          pageTitle,
          pageHeading,
          legendClass,
          paragraphText,
          hintText,
          submitCall
        )
      } else {
        enterReferenceValueView(
          filledForm,
          schemeName,
          pageTitle,
          pageHeading,
          hintText,
          paragraphText,
          submitCall
        )
      }
    ))
  }

  def post(
            pageTitle: String,
            pageHeading: String,
            isPageHeading: Boolean,
            id: TypedIdentifier[ReferenceValue],
            form: Form[ReferenceValue],
            schemeName: String,
            hintText: Option[String] = None,
            paragraphText: Seq[String] = Seq(),
            legendClass: String = "govuk-fieldset__legend--s",
            mode: Mode,
            submitCall: Call,
            optSetUserAnswers: Option[ReferenceValue => Try[UserAnswers]] = None
          )(implicit request: DataRequest[AnyContent], ec: ExecutionContext): Future[Result] = {

    form.bindFromRequest().fold(
      formWithErrors => {
        val view = if (paragraphText.nonEmpty || hintText.nonEmpty) {
          enterReferenceValueWithHintView(formWithErrors, schemeName, pageTitle, pageHeading, legendClass, paragraphText, hintText, submitCall)
        } else {
          enterReferenceValueView(formWithErrors, schemeName, pageTitle, pageHeading, hintText, paragraphText, submitCall)
        }
        Future.successful(BadRequest(view))
      },
      value => {
        def defaultSetUserAnswers = (value: ReferenceValue) =>
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
