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

package services.common.contact

import connectors.cache.UserAnswersCacheConnector
import identifiers.TypedIdentifier
import models.requests.DataRequest
import models.{Mode, NormalMode}
import navigators.CompoundNavigator
import play.api.data.Form
import play.api.data.FormBinding.Implicits._
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.Results.{BadRequest, Ok, Redirect}
import play.api.mvc.{AnyContent, Call, MessagesControllerComponents, Result}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendHeaderCarrierProvider
import utils.UserAnswers
import views.html.EmailView

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

@Singleton
class CommonEmailAddressService @Inject()(
                                           val controllerComponents: MessagesControllerComponents,
                                           val userAnswersCacheConnector: UserAnswersCacheConnector,
                                           val navigator: CompoundNavigator,
                                           val messagesApi: MessagesApi,
                                           emailView: EmailView
                                         ) extends FrontendHeaderCarrierProvider
  with I18nSupport {

  def get(
           entityName: String,
           entityType: String,
           emailId: TypedIdentifier[String],
           form: Form[String],
           schemeName: String,
           paragraphText: Seq[String] = Seq(),
           submitCall: Call
         )(implicit request: DataRequest[AnyContent]): Future[Result] = {
    val filledForm = request.userAnswers.get(emailId).fold(form)(form.fill)
    Future.successful(Ok(
      emailView(
        filledForm,
        schemeName,
        entityName,
        entityType,
        paragraphText,
        submitCall
      )))
  }

  def post(entityName: String,
           entityType: String,
           emailId: TypedIdentifier[String],
           form: Form[String],
           schemeName: String,
           paragraphText: Seq[String] = Seq(),
           mode: Option[Mode] = None,
           submitCall: Call,
           optSetUserAnswers: Option[String => Try[UserAnswers]] = None)
          (implicit request: DataRequest[AnyContent],
           ec: ExecutionContext): Future[Result] = {
    form
      .bindFromRequest()
      .fold(
        formWithErrors => {
          Future.successful(BadRequest(
            emailView(
              formWithErrors,
              schemeName,
              entityName,
              entityType,
              paragraphText,
              submitCall
            )))
        },
        value => {
          def defaultSetUserAnswers = (value: String) =>
            request.userAnswers.set(emailId, value)

          val setUserAnswers = optSetUserAnswers.getOrElse(defaultSetUserAnswers)
          for {
            updatedAnswers <- Future.fromTry(setUserAnswers(value))
            _ <- userAnswersCacheConnector.save(request.lock, updatedAnswers.data)
          } yield {
            val finalMode = mode.getOrElse(NormalMode)
            Redirect(navigator.nextPage(emailId, updatedAnswers, finalMode))
          }
        }
      )
  }

}
