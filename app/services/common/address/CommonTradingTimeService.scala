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

package services.common.address

import connectors.cache.UserAnswersCacheConnector
import controllers.Retrievals
import identifiers.TypedIdentifier
import models.requests.DataRequest
import models.{Mode, NormalMode}
import navigators.CompoundNavigator
import play.api.data.Form
import play.api.data.FormBinding.Implicits.formBinding
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.Results.{BadRequest, Ok, Redirect}
import play.api.mvc.{AnyContent, Call, Result}
import uk.gov.hmrc.govukfrontend.views.viewmodels.radios.RadioItem
import uk.gov.hmrc.http.HeaderCarrier
import views.html.address.TradingTimeView

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class CommonTradingTimeService @Inject()(
    userAnswersCacheConnector: UserAnswersCacheConnector,
    navigator: CompoundNavigator,
    val messagesApi: MessagesApi,
    tradingTimeView: TradingTimeView
) extends Retrievals with I18nSupport {

  private case class TemplateData(
                                   schemeName: Option[String],
                                   entityName: String,
                                   entityType : String,
                                   form : Form[Boolean],
                                   radios: Seq[RadioItem]
                                 )

  def get(schemeName: Option[String],
          entityName: String,
          entityType : String,
          form : Form[Boolean],
          tradingTimeId : TypedIdentifier[Boolean],
          submitUrl: Call
         )(implicit request: DataRequest[AnyContent]): Future[Result] = {
    val filledForm = request.userAnswers.get(tradingTimeId).fold(form)(form.fill)
    val templateData = getTemplateData(schemeName, entityName, entityType, filledForm)
    Future.successful(Ok(
      tradingTimeView(
        filledForm,
        templateData.entityType,
        templateData.entityName,
        utils.Radios.yesNo(filledForm("value")),
        templateData.schemeName,
        submitUrl = submitUrl
    )))
  }

  def post(schemeName: Option[String],
           entityName: String,
           entityType : String,
           form : Form[Boolean],
           tradingTimeId : TypedIdentifier[Boolean],
           mode: Option[Mode] = None,
           submitUrl: Call
          )(implicit request: DataRequest[AnyContent], ec: ExecutionContext, hc: HeaderCarrier): Future[Result] = {
    form
      .bindFromRequest()
      .fold(
        formWithErrors => {
          val templateData = getTemplateData(schemeName, entityName, entityType, formWithErrors)
          Future.successful(BadRequest(
            tradingTimeView(
              formWithErrors,
              templateData.entityType,
              templateData.entityName,
              utils.Radios.yesNo(formWithErrors("value")),
              templateData.schemeName,
              submitUrl = submitUrl
            )))
        },
        value =>
          for {
            updatedAnswers <- Future.fromTry(request.userAnswers.set(tradingTimeId, value))
            _ <- userAnswersCacheConnector.save(request.lock,updatedAnswers.data)
          } yield {
            val finalMode = mode.getOrElse(NormalMode)
            Redirect(navigator.nextPage(tradingTimeId, updatedAnswers, finalMode))
          }
      )
  }

  private def getTemplateData(
                      schemeName: Option[String],
                      entityName: String,
                      entityType : String,
                      form : Form[Boolean]
                    )(implicit request: DataRequest[AnyContent]): TemplateData =
    TemplateData(schemeName, entityName, entityType, form, utils.Radios.yesNo(form("value")))

}
