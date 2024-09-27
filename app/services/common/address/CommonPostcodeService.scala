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
import connectors.AddressLookupConnector
import connectors.cache.UserAnswersCacheConnector
import forms.FormsHelper.formWithError
import identifiers.TypedIdentifier
import models.requests.DataRequest
import models.{Mode, NormalMode, TolerantAddress}
import navigators.CompoundNavigator
import play.api.data.Form
import play.api.data.FormBinding.Implicits.formBinding
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.libs.json.{JsArray, Json, OWrites, Writes}
import play.api.mvc.Results.{BadRequest, Ok, Redirect}
import play.api.mvc.{AnyContent, Call, Result}
import uk.gov.hmrc.http.HeaderCarrier
import views.html.address.PostcodeView

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

case class CommonPostcodeTemplateData(
                                       form: Form[String],
                                       entityType: String,
                                       entityName: String,
                                       submitUrl: Call,
                                       enterManuallyUrl: String,
                                       schemeName: String,
                                       h1MessageKey: String  = "postcode.title"
                                     )

object CommonPostcodeTemplateData {
  implicit val formWrites: Writes[Form[String]] = (form: Form[String]) => Json.obj(
    "data" -> form.data,
    "errors" -> form.errors.map(_.message)
  )
  implicit val callWrites: Writes[Call] = Writes[Call](call => Json.obj("url" -> call.url))
  implicit val templateDataWrites: OWrites[CommonPostcodeTemplateData] = Json.writes[CommonPostcodeTemplateData]
}

@Singleton
class CommonPostcodeService @Inject()(
     val messagesApi: MessagesApi,
     navigator: CompoundNavigator,
     addressLookupConnector: AddressLookupConnector,
     userAnswersCacheConnector: UserAnswersCacheConnector,
     postcodeView: PostcodeView
   ) extends I18nSupport {

  def get(formToTemplate: Form[String] => CommonPostcodeTemplateData,
          form: Form[String]
         )(implicit request: DataRequest[AnyContent], ec: ExecutionContext): Future[Result] = {
    val templateData = formToTemplate(form)
    Future.successful(Ok(postcodeView(
      form,
      templateData.entityType,
      templateData.entityName,
      templateData.submitUrl,
      templateData.enterManuallyUrl,
      Some(templateData.schemeName)
    )))
  }

  def post(formToTemplate: Form[String] => CommonPostcodeTemplateData,
           postcodeId: TypedIdentifier[Seq[TolerantAddress]],
           errorMessage: String,
           mode: Option[Mode] = None,
           form: Form[String]
          )(implicit request: DataRequest[AnyContent], ec: ExecutionContext, hc: HeaderCarrier): Future[Result] = {
    form.bindFromRequest().fold(
      formWithErrors => {
        val templateData = formToTemplate(formWithErrors)
        Future.successful(BadRequest(postcodeView(
          formWithErrors,
          templateData.entityType,
          templateData.entityName,
          templateData.submitUrl,
          templateData.enterManuallyUrl,
          Some(templateData.schemeName)
        )))
      },
      value =>
          addressLookupConnector.addressLookupByPostCode(value).flatMap {
            case Nil =>
              val formWithErrors = formWithError(form, errorMessage)
              val templateData = formToTemplate(formWithErrors)
              Future.successful(BadRequest(postcodeView(
                formWithErrors,
                templateData.entityType,
                templateData.entityName,
                templateData.submitUrl,
                templateData.enterManuallyUrl,
                Some(templateData.schemeName)
              )))
            case addresses =>
              for {
                updatedAnswers <- Future.fromTry(request.userAnswers.set(postcodeId, addresses))
                _ <- userAnswersCacheConnector.save(request.lock,updatedAnswers.data)
              } yield
                {
                  val finalMode = mode.getOrElse(NormalMode)
                  Redirect(navigator.nextPage(postcodeId, updatedAnswers, finalMode))
                }
          }
    )
  }

  def countryJsonElement(tuple: (String, String), isSelected: Boolean): JsArray = Json.arr(
    if (isSelected) {
      Json.obj(
        "value" -> tuple._1,
        "text" -> tuple._2,
        "selected" -> true
      )
    } else {
      Json.obj(
        "value" -> tuple._1,
        "text" -> tuple._2
      )
    }
  )

  def jsonCountries(countrySelected: Option[String], config: AppConfig)(implicit messages: Messages): JsArray =
    config.validCountryCodes
      .map(countryCode => (countryCode, messages(s"country.$countryCode")))
      .sortWith(_._2 < _._2)
      .foldLeft(JsArray(Seq(Json.obj("value" -> "", "text" -> "")))) { (acc, nextCountryTuple) =>
        acc ++ countryJsonElement(nextCountryTuple, countrySelected.contains(nextCountryTuple._1))
      }

}


