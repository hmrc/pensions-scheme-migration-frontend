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
import models.establishers.AddressPages
import models.requests.DataRequest
import models.{Mode, NormalMode, TolerantAddress}
import navigators.CompoundNavigator
import play.api.data.Form
import play.api.data.FormBinding.Implicits.formBinding
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.json.{Json, OWrites, Writes}
import play.api.mvc.Results.{BadRequest, Ok, Redirect}
import play.api.mvc.{AnyContent, Call, Result}
import uk.gov.hmrc.govukfrontend.views.Aliases.{Label, Text}
import uk.gov.hmrc.govukfrontend.views.viewmodels.radios.RadioItem
import uk.gov.hmrc.http.HeaderCarrier
import views.html.address.AddressListView

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

case class CommonAddressListTemplateData(
                                      form: Form[Int],
                                      addresses: Seq[TolerantAddress],
                                      entityType: String,
                                      entityName: String,
                                      enterManuallyUrl: String,
                                      schemeName: String,
                                      h1MessageKey: String
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
    userAnswersCacheConnector: UserAnswersCacheConnector,
    navigator: CompoundNavigator,
    val messagesApi: MessagesApi,
    addressListView: AddressListView
) extends Retrievals with  I18nSupport {

  import CommonAddressListTemplateData._

  def get(template: CommonAddressListTemplateData,
          form: Form[Int],
          submitUrl : Call
         )(implicit request: DataRequest[AnyContent], ec: ExecutionContext): Future[Result] = {
    Future.successful(Ok(addressListView(
      form,
      template.entityType,
      template.entityName,
      convertToRadioItems(template.addresses),
      template.enterManuallyUrl,
      template.schemeName,
      submitUrl = submitUrl,
      template.h1MessageKey
    )))
  }

  private def convertToRadioItems(addresses: Seq[TolerantAddress]): Seq[RadioItem] = {

    addresses.zipWithIndex.map { case (address, index) =>
      RadioItem(
        content = Text(address.print),
        label = Some(Label(content = Text(address.print))),
        value = Some(index.toString)
      )
    }
  }

  def post(formToTemplate: Form[Int] => CommonAddressListTemplateData,
           pages: AddressPages,
           mode: Option[Mode] = None,
           manualUrlCall:Call,
           form: Form[Int],
           submitUrl : Call
          )(implicit request: DataRequest[AnyContent], ec: ExecutionContext, hc: HeaderCarrier): Future[Result] = {
    form.bindFromRequest().fold(
      formWithErrors =>{
        val template = formToTemplate(formWithErrors)
        Future.successful(BadRequest(addressListView(
          form,
          template.entityType,
          template.entityName,
          convertToRadioItems(template.addresses),
          template.enterManuallyUrl,
          template.schemeName,
          submitUrl = submitUrl,
          template.h1MessageKey
        )))
      },
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

}
