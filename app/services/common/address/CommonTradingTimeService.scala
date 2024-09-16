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
import play.api.libs.json.{Json, OWrites}
import play.api.mvc.{AnyContent, Result}
import renderer.Renderer
import uk.gov.hmrc.nunjucks.NunjucksSupport
import uk.gov.hmrc.viewmodels.Radios
import play.api.mvc.Results.{BadRequest, Ok, Redirect}
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class CommonTradingTimeService @Inject()(
    renderer: Renderer,
    userAnswersCacheConnector: UserAnswersCacheConnector,
    navigator: CompoundNavigator,
    val messagesApi: MessagesApi
) extends Retrievals with I18nSupport with NunjucksSupport {

  private def viewTemplate = "address/tradingTime.njk"

  private case class TemplateData(
                                   schemeName: Option[String],
                                   entityName: String,
                                   entityType : String,
                                   form : Form[Boolean],
                                   radios: Seq[Radios.Item]
                                 )

  implicit private def templateDataWrites(implicit request: DataRequest[AnyContent]): OWrites[TemplateData] = Json.writes[TemplateData]


  def get(schemeName: Option[String],
                    entityName: String,
                    entityType : String,
                    form : Form[Boolean],
                    tradingTimeId : TypedIdentifier[Boolean]
                   )(implicit request: DataRequest[AnyContent], ec: ExecutionContext): Future[Result] = {
    val filledForm = request.userAnswers.get(tradingTimeId).fold(form)(form.fill)
    renderer.render(viewTemplate, getTemplateData(schemeName, entityName, entityType, filledForm)).map(Ok(_))
  }

  def post(schemeName: Option[String],
           entityName: String,
           entityType : String,
           form : Form[Boolean],
           tradingTimeId : TypedIdentifier[Boolean],
           mode: Option[Mode] = None
          )(implicit request: DataRequest[AnyContent], ec: ExecutionContext, hc: HeaderCarrier): Future[Result] = {
    form
      .bindFromRequest()
      .fold(
        formWithErrors => {
          renderer.render(viewTemplate, getTemplateData(schemeName, entityName, entityType, formWithErrors)).map(BadRequest(_))
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
    TemplateData(schemeName, entityName, entityType, form, Radios.yesNo(form("value")))

}
