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
import helpers.CountriesHelper
import identifiers.TypedIdentifier
import models.AddressConfiguration.AddressConfiguration
import models._
import models.requests.DataRequest
import navigators.CompoundNavigator
import play.api.data.Form
import play.api.data.FormBinding.Implicits.formBinding
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.json.{JsArray, Json, OWrites, Writes}
import play.api.mvc.Results.{BadRequest, Ok, Redirect}
import play.api.mvc.{AnyContent, Call, Result}
import uk.gov.hmrc.govukfrontend.views.viewmodels.select.SelectItem
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendHeaderCarrierProvider
import views.html.address.ManualAddressView

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class CommonManualAddressService @Inject()(
  userAnswersCacheConnector: UserAnswersCacheConnector,
  navigator: CompoundNavigator,
  val messagesApi: MessagesApi,
  config: AppConfig,
  manualAddressView: ManualAddressView
) extends FrontendHeaderCarrierProvider with I18nSupport with CountriesHelper {

  private val pageTitleMessageKey: String = "address.title"

  private case class TemplateData(
                                   form : Form[Address],
                                   pageTitle: String,
                                   h1Message: String,
                                   schemeName: Option[String],
                                   submitUrl: Call,
                                   postcodeFirst: Boolean = false,
                                   postcodeEntry: Boolean = false,
                                   countries: JsArray = Json.arr()
                                 )

  implicit val addressWrites: Writes[Address] = Json.writes[Address]
  implicit val formAddressWrites: Writes[Form[Address]] = (form: Form[Address]) => Json.obj(
    "data" -> form.data,
    "errors" -> form.errors.map(e => Json.obj(
      "key" -> e.key,
      "message" -> e.message
    )),
    "value" -> form.value
  )
  implicit val callWrites: Writes[Call] = Writes[Call](call => Json.obj("url" -> call.url))
  implicit private def templateDataWrites(implicit request: DataRequest[AnyContent]): OWrites[TemplateData] = Json.writes[TemplateData]

  def get(schemeName: Option[String],
          entityName: String,
          addressPage: TypedIdentifier[Address],
          selectedAddress: TypedIdentifier[TolerantAddress],
          addressLocation: AddressConfiguration,
          form: Form[Address],
          pageTitleEntityTypeMessageKey: Option[String] = None,
          pageTitleMessageKey: String = pageTitleMessageKey,
          submitUrl: Call
         )(implicit request: DataRequest[AnyContent], ec: ExecutionContext): Future[Result] = {

    val preparedForm = request.userAnswers.get(addressPage) match {
      case None => request.userAnswers.get(selectedAddress) match {
        case Some(value) => form.fill(value.toPrepopAddress)
        case None => form
      }
      case Some(value) => form.fill(value)
    }

    val templateData = getTemplateData(schemeName, entityName, preparedForm, addressLocation,
      pageTitleEntityTypeMessageKey, pageTitleMessageKey, submitUrl)
    Future.successful(
      Ok(manualAddressView(
        preparedForm,
        templateData.pageTitle,
        templateData.h1Message,
        templateData.submitUrl,
        schemeName = schemeName,
        countries = templateData.countries.value.toSeq.map(jsValue => jsValue.as[SelectItem]),
        postcodeEntry = templateData.postcodeEntry,
        postcodeFirst = templateData.postcodeFirst
      )))
  }

  def post(schemeName: Option[String],
           entityName: String,
           addressPage: TypedIdentifier[Address],
           addressLocation: AddressConfiguration,
           mode: Option[Mode] = None,
           form: Form[Address],
           pageTitleEntityTypeMessageKey: Option[String] = None,
           pageTitleMessageKey: String = pageTitleMessageKey,
           submitUrl: Call
          )(implicit request: DataRequest[AnyContent], ec: ExecutionContext): Future[Result] = {
    form
      .bindFromRequest()
      .fold(
        formWithErrors => {
          val templateData = getTemplateData(schemeName, entityName, formWithErrors, addressLocation,
            pageTitleEntityTypeMessageKey, pageTitleMessageKey, submitUrl)

          Future.successful(
            BadRequest(manualAddressView(
              formWithErrors,
              templateData.pageTitle,
              templateData.h1Message,
              templateData.submitUrl,
              schemeName = schemeName,
              countries = templateData.countries.value.toSeq.map(jsValue => jsValue.as[SelectItem]),
              postcodeEntry = templateData.postcodeEntry,
              postcodeFirst = templateData.postcodeFirst
            )))
        },
        value =>
          for {
            updatedAnswers <- Future.fromTry(request.userAnswers.set(addressPage, value))
            _ <- userAnswersCacheConnector.save(request.lock,updatedAnswers.data)
          } yield {
            val finalMode = mode.getOrElse(NormalMode)
            Redirect(navigator.nextPage(addressPage, updatedAnswers, finalMode))
        }
      )
  }

  private def getTemplateData(
            schemeName: Option[String],
            entityName: String,
            form: Form[Address],
            addressLocation: AddressConfiguration,
            pageTitleEntityTypeMessageKey: Option[String] = None,
            pageTitleMessageKey: String,
            submitUrl: Call
          )(implicit request: DataRequest[AnyContent]): TemplateData = {
    val messages = request2Messages
    val h1MessageKey = pageTitleMessageKey
    val pageTitle = pageTitleEntityTypeMessageKey match {
      case Some(key) => messages(pageTitleMessageKey, messages(key))
      case _ => messages(pageTitleMessageKey)
    }
    val h1Message =  messages(h1MessageKey, entityName)

    val templateDate = addressLocation match {
      case AddressConfiguration.PostcodeFirst =>
        TemplateData(form, pageTitle, h1Message, schemeName,
          postcodeFirst = true, postcodeEntry = true,
          countries = jsonCountries(form.data.get("country"), config)(messages), submitUrl = submitUrl)

      case AddressConfiguration.CountryFirst =>
        TemplateData(form, pageTitle, h1Message, schemeName,
          postcodeEntry = true,
          countries = jsonCountries(form.data.get("country"), config)(messages), submitUrl = submitUrl)

      case AddressConfiguration.CountryOnly =>
        TemplateData(form, pageTitle, h1Message, schemeName,
          countries = jsonCountries(form.data.get("country"), config)(messages), submitUrl = submitUrl)
      case _ => TemplateData(form, pageTitle, h1Message, schemeName, submitUrl = submitUrl)
    }

    templateDate
  }
}
