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

import config.AppConfig
import connectors.cache.UserAnswersCacheConnector
import controllers.Retrievals
import models.establishers.AddressPages
import models.requests.DataRequest
import models.{Mode, NormalMode, TolerantAddress}
import navigators.CompoundNavigator
import play.api.data.Form
import play.api.data.FormBinding.Implicits.formBinding
import play.api.i18n.MessagesApi
import play.api.libs.json.{JsObject, Json, OWrites, Writes}
import play.api.mvc.Results.{BadRequest, Ok, Redirect}
import play.api.mvc.{AnyContent, Call, Result}
import renderer.Renderer
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

case class CommonAddressListTemplateData(
                                      form: Form[Int],
                                      addresses: Seq[JsObject], //TODO: Change to Seq[TolerantAddress] during nunjucks migration. -Pavel Vjalicin
                                      entityType: String,
                                      entityName: String,
                                      enterManuallyUrl: String,
                                      schemeName: String,
                                      h1MessageKey: String = "addressList.title"
                                    )

object CommonAddressListTemplateData {
  implicit val formWrites: Writes[Form[Int]] = (form: Form[Int]) => Json.obj(
    "data" -> form.data,
    "errors" -> form.errors.map(_.message)
  )
  implicit val templateDataWrites: OWrites[CommonAddressListTemplateData] = Json.writes[CommonAddressListTemplateData]
}

@Singleton
class CommonAddressListService @Inject()(
                                      val renderer: Renderer,
                                      val userAnswersCacheConnector: UserAnswersCacheConnector,
                                      val navigator: CompoundNavigator,
                                      val messagesApi: MessagesApi,
                                      val config: AppConfig
                                    ) extends Retrievals {

  import CommonAddressListTemplateData._

  def viewTemplate: String = "address/addressList.njk"

  def get(template: CommonAddressListTemplateData
         )(implicit request: DataRequest[AnyContent], ec: ExecutionContext): Future[Result] = {
    renderer.render(viewTemplate, template).map(Ok(_))
  }

  def post(formToTemplate: Form[Int] => CommonAddressListTemplateData,
           pages: AddressPages,
           mode: Option[Mode] = None,
           manualUrlCall:Call,
           form: Form[Int]
          )(implicit request: DataRequest[AnyContent], ec: ExecutionContext, hc: HeaderCarrier): Future[Result] = {
    form.bindFromRequest().fold(
      formWithErrors =>
        renderer.render(viewTemplate, formToTemplate(formWithErrors)).map(BadRequest(_)),
      value =>
        pages.postcodeId.retrieve.map { addresses =>
          val address = addresses(value).copy(country = Some("GB"))
          if (address.toAddress.nonEmpty){
            for {
              updatedAnswers <- Future.fromTry(
                request.userAnswers.remove(pages.addressListPage).set(pages.addressPage,
                  address.toAddress.get)
              )
              _ <- userAnswersCacheConnector.save(request.lock, updatedAnswers.data)
            } yield {
              val finalMode = mode.getOrElse(NormalMode)
              Redirect(navigator.nextPage(pages.addressListPage, updatedAnswers, finalMode))
            }
          }else{
            for {
              updatedAnswers <- Future.fromTry(
                request.userAnswers.remove(pages.addressPage).set(pages.addressListPage,
                  address
                ))
              _ <- userAnswersCacheConnector.save(request.lock, updatedAnswers.data)
            } yield {
              Redirect(manualUrlCall)
            }

          }
        }
    )
  }

  def transformAddressesForTemplate(addresses:Seq[TolerantAddress]):Seq[JsObject] = {
    for ((row, i) <- addresses.zipWithIndex) yield {
      Json.obj(
        "value" -> i,
        "text" -> row.print
      )
    }
  }

}
