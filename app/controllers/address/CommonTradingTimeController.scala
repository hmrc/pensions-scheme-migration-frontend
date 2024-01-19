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

package controllers.address

import connectors.cache.UserAnswersCacheConnector
import controllers.Retrievals
import identifiers.TypedIdentifier
import models.requests.DataRequest
import models.{Mode, NormalMode}
import navigators.CompoundNavigator
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.{AnyContent, Result}
import renderer.Renderer
import uk.gov.hmrc.nunjucks.NunjucksSupport
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import uk.gov.hmrc.viewmodels.Radios

import scala.concurrent.{ExecutionContext, Future}

trait CommonTradingTimeController extends FrontendBaseController
  with Retrievals
  with I18nSupport
  with NunjucksSupport {

  protected def renderer: Renderer
  protected def userAnswersCacheConnector: UserAnswersCacheConnector
  protected def navigator: CompoundNavigator
  protected def viewTemplate = "address/tradingTime.njk"

  protected def get(schemeName: Option[String],
                    entityName: String,
                    entityType : String,
                    form : Form[Boolean],
                    tradingTimeId : TypedIdentifier[Boolean])(
                     implicit request: DataRequest[AnyContent],
                     ec: ExecutionContext): Future[Result] = {
    val filledForm = request.userAnswers.get(tradingTimeId).fold(form)(form.fill)
    renderer.render(viewTemplate, json(schemeName, entityName, entityType, filledForm)).map(Ok(_))
  }

  protected def post(schemeName: Option[String],
                     entityName: String,
                     entityType : String,
                     form : Form[Boolean],
                     tradingTimeId : TypedIdentifier[Boolean],
                     mode: Option[Mode] = None)(
                      implicit request: DataRequest[AnyContent],
                      ec: ExecutionContext): Future[Result] = {
    form
      .bindFromRequest()
      .fold(
        formWithErrors => {
          renderer.render(viewTemplate, json(schemeName, entityName, entityType, formWithErrors)).map(BadRequest(_))
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

  protected def json(
                      schemeName: Option[String],
                      entityName: String,
                      entityType : String,
                      form : Form[Boolean])
                    (implicit request: DataRequest[AnyContent]): JsObject =
  Json.obj(
      "schemeName" -> schemeName,
      "entityName" -> entityName,
      "entityType" -> entityType,
      "form" -> form,
      "radios" -> Radios.yesNo (form("value"))
    )
}
