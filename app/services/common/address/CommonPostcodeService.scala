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
import play.api.i18n.Messages
import play.api.libs.json.{JsArray, JsObject, Json}
import play.api.mvc.Results.{BadRequest, Ok, Redirect}
import play.api.mvc.{AnyContent, Result}
import renderer.Renderer
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class CommonPostcodeService @Inject()(
                                       val renderer: Renderer,
                                       val navigator: CompoundNavigator,
                                       val addressLookupConnector: AddressLookupConnector,
                                       val userAnswersCacheConnector: UserAnswersCacheConnector
                                     ) {

  def viewTemplate: String = "address/postcode.njk"

  def prepareJson(jsObject: JsObject):JsObject = {
    if (jsObject.keys.contains("h1MessageKey")) {
      jsObject
    } else {
      jsObject ++ Json.obj("h1MessageKey" -> "postcode.title")
    }
  }

  def get(json: Form[String] => JsObject,
          form: Form[String]
         )(implicit request: DataRequest[AnyContent], ec: ExecutionContext): Future[Result] = {

    renderer.render(viewTemplate, prepareJson(json(form))).map(Ok(_))
  }

  def post(formToJson: Form[String] => JsObject,
           postcodeId: TypedIdentifier[Seq[TolerantAddress]],
           errorMessage: String,
           mode: Option[Mode] = None,
           form: Form[String]
          )(implicit request: DataRequest[AnyContent], ec: ExecutionContext, hc: HeaderCarrier): Future[Result] = {
    form.bindFromRequest().fold(
      formWithErrors =>
        renderer.render(viewTemplate, prepareJson(formToJson(formWithErrors))).map(BadRequest(_)),
      value =>
          addressLookupConnector.addressLookupByPostCode(value).flatMap {
            case Nil =>
              val json = prepareJson(formToJson(formWithError(form, errorMessage)))
                renderer.render(viewTemplate, json).map(BadRequest(_))

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


