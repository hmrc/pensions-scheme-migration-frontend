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

package helpers

import config.AppConfig
import models.Country
import play.api.i18n.Messages
import play.api.libs.json.{JsArray, Json}
import uk.gov.hmrc.govukfrontend.views.viewmodels.select.SelectItem

trait CountriesHelper {
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

  def countrySelectList(value: Map[String, String], countries: Seq[Country]): Seq[SelectItem] = {
    def containsCountry(country: Country): Boolean =
      value.get("country") match {
        case Some(countryCode) => countryCode == country.code
        case _                 => false
      }
    val countryJsonList = countries.map {
      country =>
        SelectItem(Some(country.code), country.description, containsCountry(country))
    }
    SelectItem(None, "") +: countryJsonList
  }

}
