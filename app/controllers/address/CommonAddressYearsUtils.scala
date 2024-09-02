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
import identifiers.TypedIdentifier
import models.requests.DataRequest
import models.{Index, Mode, NormalMode}
import navigators.CompoundNavigator
import play.api.data.Form
import play.api.libs.json.{Json, OFormat}
import play.api.mvc.Results.{BadRequest, Ok, Redirect}
import play.api.mvc.{AnyContent, MessagesControllerComponents, Result}
import renderer.Renderer
import uk.gov.hmrc.nunjucks.NunjucksSupport
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendHeaderCarrierProvider
import uk.gov.hmrc.viewmodels.Radios

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import play.api.data.FormBinding.Implicits._
import play.api.i18n.{I18nSupport, MessagesApi}
import utils.UserAnswers
import viewmodels.Message

import scala.util.Try

class CommonAddressYearsUtils @Inject()(
                                               val controllerComponents: MessagesControllerComponents,
                                               val renderer: Renderer,
                                               val userAnswersCacheConnector: UserAnswersCacheConnector,
                                               val navigator: CompoundNavigator,
                                               val messagesApi: MessagesApi
                                             ) extends NunjucksSupport with FrontendHeaderCarrierProvider with I18nSupport {
  private def viewTemplate = "address/addressYears.njk"

  def get(schemeName: Option[String],
                    entityName: String,
                    entityType : Message,
                    form : Form[Boolean],
                    addressYearsId : TypedIdentifier[Boolean])(
                     implicit request: DataRequest[AnyContent],
                     ec: ExecutionContext): Future[Result] = {
    val filledForm = request.userAnswers.get(addressYearsId).fold(form)(form.fill)
    renderer.render(viewTemplate, getTemplateData(schemeName, entityName, entityType.resolve, filledForm) ).map(Ok(_))
  }

  def post(schemeName: Option[String],
           entityName: String,
           entityType : Message,
           form : Form[Boolean],
           addressYearsId : TypedIdentifier[Boolean],
           mode: Option[Mode] = None,
           optSetUserAnswers:Option[Boolean => Try[UserAnswers]] = None)
          (implicit request: DataRequest[AnyContent],
           ec: ExecutionContext): Future[Result] = {
    form
      .bindFromRequest()
      .fold(
        formWithErrors => {
          renderer.render(viewTemplate, getTemplateData(schemeName, entityName, entityType.resolve, formWithErrors) ).map(BadRequest(_))
        },
        value => {
          def defaultSetUserAnswers = (value: Boolean) => request.userAnswers.set(addressYearsId, value)
          val setUserAnswers = optSetUserAnswers.getOrElse(defaultSetUserAnswers)
          for {
            updatedAnswers <- Future.fromTry(setUserAnswers(value))
            _ <- userAnswersCacheConnector.save(request.lock,updatedAnswers.data)
          } yield {
            val finalMode = mode.getOrElse(NormalMode)
            Redirect(navigator.nextPage(addressYearsId, updatedAnswers, finalMode))
          }
        }
      )
  }


  private case class TemplateData(
                                   schemeName: Option[String],
                                   entityName: String,
                                   entityType : String,
                                   form : Form[Boolean],
                                   radios: Seq[Radios.Item]
                                 )

  implicit private val templateDataFormat: OFormat[TemplateData] = Json.format[TemplateData]

  private def getTemplateData(
                      schemeName: Option[String],
                      entityName: String,
                      entityType : String,
                      form : Form[Boolean]): TemplateData =
    TemplateData(
      schemeName,
      entityName,
      entityType,
      form,
      Radios.yesNo(form("value"))
    )
}
