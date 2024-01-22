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

import config.AppConfig
import connectors.cache.UserAnswersCacheConnector
import controllers.Retrievals
import helpers.CountriesHelper
import identifiers.TypedIdentifier
import models.AddressConfiguration.AddressConfiguration
import models.requests.DataRequest
import models._
import navigators.CompoundNavigator
import play.api.data.Form
import play.api.i18n.{I18nSupport, Messages}
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.{AnyContent, Result}
import renderer.Renderer
import uk.gov.hmrc.nunjucks.NunjucksSupport
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import scala.concurrent.{ExecutionContext, Future}

trait ManualAddressController
    extends FrontendBaseController
    with Retrievals
    with I18nSupport
    with NunjucksSupport
    with CountriesHelper {

  protected def renderer: Renderer

  protected def userAnswersCacheConnector: UserAnswersCacheConnector

  protected def navigator: CompoundNavigator

  protected def form(implicit messages: Messages): Form[Address]

  protected def viewTemplate = "address/manualAddress.njk"

  protected def config: AppConfig

  protected val pageTitleMessageKey: String = "address.title"

  protected val pageTitleEntityTypeMessageKey: Option[String] = None

  protected val h1MessageKey: String = pageTitleMessageKey

  protected def addressConfigurationForPostcodeAndCountry(isUK:Boolean): AddressConfiguration =
    if(isUK) AddressConfiguration.PostcodeFirst else AddressConfiguration.CountryFirst

  protected def get(schemeName: Option[String],
                    entityName: String,
                    addressPage: TypedIdentifier[Address],
                    selectedAddress: TypedIdentifier[TolerantAddress],
                    addressLocation: AddressConfiguration)(
    implicit request: DataRequest[AnyContent],
    ec: ExecutionContext
  ): Future[Result] = {

    val preparedForm = request.userAnswers.get(addressPage) match {
      case None => request.userAnswers.get(selectedAddress) match {
        case Some(value) => form.fill(value.toPrepopAddress)
        case None => form
      }
      case Some(value) => form.fill(value)
    }
    renderer.render(viewTemplate, json(schemeName, entityName, preparedForm, addressLocation)).map(Ok(_))
  }

  protected def post(schemeName: Option[String],
                     entityName: String,
                     addressPage: TypedIdentifier[Address],
                     addressLocation: AddressConfiguration,
                     mode: Option[Mode] = None)(
    implicit request: DataRequest[AnyContent],
    ec: ExecutionContext
  ): Future[Result] = {
    form
      .bindFromRequest()
      .fold(
        formWithErrors => {
          renderer.render(viewTemplate, json(schemeName, entityName, formWithErrors, addressLocation)).map(BadRequest(_))
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

  protected def json(
    schemeName: Option[String],
    entityName: String,
    form: Form[Address],
    addressLocation: AddressConfiguration
  )(implicit request: DataRequest[AnyContent]): JsObject = {
    val messages = request2Messages
    val extraJson = addressLocation match {
      case AddressConfiguration.PostcodeFirst =>
        Json.obj(
          "postcodeFirst" -> true,
          "postcodeEntry" -> true,
          "countries" -> jsonCountries(form.data.get("country"), config)(messages)
        )
      case AddressConfiguration.CountryFirst =>
        Json.obj(
          "postcodeEntry" -> true,
          "countries" -> jsonCountries(form.data.get("country"), config)(messages)
        )
      case AddressConfiguration.CountryOnly =>
        Json.obj("countries" -> jsonCountries(form.data.get("country"), config)(messages))
      case _ => Json.obj()
    }

    val pageTitle = pageTitleEntityTypeMessageKey match {
        case Some(key) => messages(pageTitleMessageKey, messages(key))
        case _ => messages(pageTitleMessageKey)
    }

    val h1 =  messages (h1MessageKey, entityName)

    Json.obj(
      "form" -> form,
      "pageTitle" -> pageTitle,
      "h1" -> h1,
      "schemeName" -> schemeName
    ) ++ extraJson
  }
}
