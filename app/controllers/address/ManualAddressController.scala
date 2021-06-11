/*
 * Copyright 2021 HM Revenue & Customs
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
import identifiers.TypedIdentifier
import models.requests.DataRequest
import models.AddressConfiguration.AddressConfiguration
import models.{Address, AddressConfiguration}
import navigators.CompoundNavigator
import play.api.data.Form
import play.api.i18n.{Messages, I18nSupport}
import play.api.libs.json.{JsObject, JsArray, Json}
import play.api.mvc.{Call, AnyContent, Result}
import renderer.Renderer
import uk.gov.hmrc.nunjucks.NunjucksSupport
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import scala.concurrent.{ExecutionContext, Future}

trait ManualAddressController
    extends FrontendBaseController
    with Retrievals
    with I18nSupport
    with NunjucksSupport {

  protected def renderer: Renderer

  protected def userAnswersCacheConnector: UserAnswersCacheConnector

  protected def navigator: CompoundNavigator

  protected def form(implicit messages: Messages): Form[Address]

  protected def viewTemplate = "address/manualAddress.njk"

  protected def config: AppConfig

  protected def addressPage: TypedIdentifier[Address]

  protected def submitRoute: Call

  protected val pageTitleMessageKey: String = "address.title"

  protected val pageTitleEntityTypeMessageKey: Option[String] = None

  protected val h1MessageKey: String = pageTitleMessageKey

  protected def addressConfigurationForPostcodeAndCountry(isUK:Boolean): AddressConfiguration =
    if(isUK) AddressConfiguration.PostcodeFirst else AddressConfiguration.CountryFirst

  protected def get(schemeName: Option[String],
                    addressLocation: AddressConfiguration)(
    implicit request: DataRequest[AnyContent],
    ec: ExecutionContext
  ): Future[Result] = {
    val filledForm = request.userAnswers.get(addressPage).fold(form)(form.fill)
    renderer.render(viewTemplate, json(schemeName, filledForm, addressLocation)).map(Ok(_))
  }

  protected def post(schemeName: Option[String],
                     addressLocation: AddressConfiguration)(
    implicit request: DataRequest[AnyContent],
    ec: ExecutionContext
  ): Future[Result] = {
    form
      .bindFromRequest()
      .fold(
        formWithErrors => {
          renderer.render(viewTemplate, json(schemeName, formWithErrors, addressLocation)).map(BadRequest(_))
        },
        value =>
          for {
            updatedAnswers <- Future.fromTry(request.userAnswers.set(addressPage, value))
            _ <- userAnswersCacheConnector.save(request.lock,updatedAnswers.data)
          } yield {
            Redirect(navigator.nextPage(addressPage, updatedAnswers))
        }
      )
  }

  protected def json(
    schemeName: Option[String],
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
    val h1 = schemeName match {
      case Some(e) =>  messages (h1MessageKey, e)
      case _ => messages (h1MessageKey)
    }

    Json.obj(
      "submitUrl" -> submitRoute.url,
      "form" -> form,
      "pageTitle" -> pageTitle,
      "h1" -> h1,
      "returnUrl" -> controllers.routes.TaskListController.onPageLoad().url,
      "schemeName" -> schemeName
    ) ++ extraJson
  }

  private def countryJsonElement(tuple: (String, String),
                                 isSelected: Boolean): JsArray =
    Json.arr(if (isSelected) {
      Json.obj("value" -> tuple._1, "text" -> tuple._2, "selected" -> true)
    } else {
      Json.obj("value" -> tuple._1, "text" -> tuple._2)
    })

  def jsonCountries(countrySelected: Option[String], config: AppConfig)(
    implicit messages: Messages
  ): JsArray = {
    config.validCountryCodes
      .map(countryCode => (countryCode, messages(s"country.$countryCode")))
      .sortWith(_._2 < _._2)
      .foldLeft(JsArray(Seq(Json.obj("value" -> "", "text" -> "")))) {
        (acc, nextCountryTuple) =>
          acc ++ countryJsonElement(
            nextCountryTuple,
            countrySelected.contains(nextCountryTuple._1)
          )
      }
  }
}
