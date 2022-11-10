/*
 * Copyright 2022 HM Revenue & Customs
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

package forms.mappings

import play.api.data.FormError
import play.api.data.format.Formatter
import utils.Enumerable

import scala.util.control.Exception.nonFatalCatch

trait Formatters extends Constraints {

  private[mappings] val optionalStringFormatter: Formatter[Option[String]] = new Formatter[Option[String]] {
    override def bind(key: String, data: Map[String, String]): Either[Seq[FormError], Option[String]] =
      Right(
        data
          .get(key)
          .map(_.replaceAll("""\s{1,}""", " ").trim)
          .filter(_.lengthCompare(0) > 0)
      )

    override def unbind(key: String, value: Option[String]): Map[String, String] =
      Map(key -> value.getOrElse(""))
  }

  private[mappings] def booleanFormatter(requiredKey: String, invalidKey: String): Formatter[Boolean] =
    new Formatter[Boolean] {

      private val baseFormatter = stringFormatter(requiredKey)

      override def bind(key: String, data: Map[String, String]): Either[Seq[FormError], Boolean] =
        baseFormatter
          .bind(key, data)
          .flatMap {
          case "true" => Right(true)
          case "false" => Right(false)
          case _ => Left(Seq(FormError(key, invalidKey)))
        }

      def unbind(key: String, value: Boolean) = Map(key -> value.toString)
    }

  private[mappings] def stringFormatter(errorKey: String): Formatter[String] = new Formatter[String] {

    override def bind(key: String, data: Map[String, String]): Either[Seq[FormError], String] =
      data
        .get(key).map(_.trim())
        .filter(_.lengthCompare(0) > 0)
        .toRight(Seq(FormError(key, errorKey)))

    override def unbind(key: String, value: String): Map[String, String] =
      Map(key -> value)
  }

  private[mappings] def intFormatter(requiredKey: String, wholeNumberKey: String, nonNumericKey: String)
  : Formatter[Int] =
    new Formatter[Int] {

      val decimalRegexp = """^(\d*\.\d*)$"""

      private val baseFormatter = stringFormatter(requiredKey)

      override def bind(key: String, data: Map[String, String]): Either[Seq[FormError], Int] =
        baseFormatter
          .bind(key, data)
          .map(_.replace(",", ""))
          .flatMap {
          case s if s.matches(decimalRegexp) =>
            Left(Seq(FormError(key, wholeNumberKey)))
          case s =>
            nonFatalCatch
              .either(s.toInt)
              .left.map(_ => Seq(FormError(key, nonNumericKey)))
        }

      override def unbind(key: String, value: Int): Map[String, String] =
        baseFormatter.unbind(key, value.toString)
    }

  private[mappings] def enumerableFormatter[A](requiredKey: String, invalidKey: String)(implicit ev: Enumerable[A])
  : Formatter[A] =
    new Formatter[A] {

      private val baseFormatter = stringFormatter(requiredKey)

      override def bind(key: String, data: Map[String, String]): Either[Seq[FormError], A] =
        baseFormatter.bind(key, data).flatMap {
          str =>
            ev.withName(str).map(Right.apply).getOrElse(Left(Seq(FormError(key, invalidKey))))
        }

      override def unbind(key: String, value: A): Map[String, String] =
        baseFormatter.unbind(key, value.toString)
    }


  def postCodeValidTransformm(value: String): String = {
    if (value.matches(regexPostcode)) {
      if (value.contains(" ")) {
        value
      } else {
        value.substring(0, value.length - 3) + " " + value.substring(value.length - 3, value.length)
      }
    }
    else {
      value
    }
  }

  private def strip(value: String): String = {
    value.replaceAll(" ", "")
  }



  //scalastyle:off cyclomatic.complexity
  def optionalPostcodeFormatter(requiredKey: Option[String],
                                                  invalidKey: String,
                                                  nonUkLengthKey: String,
                                                  countryFieldName: String): Formatter[Option[String]] = new Formatter[Option[String]] {

    private def postCodeDataTransform(value: Option[String]): Option[String] =
      value.map(_.trim.toUpperCase.replaceAll(" {2,}", " ")).filter(_.nonEmpty)

    private def countryDataTransform(value: Option[String]): Option[String] =
      value.map(strip(_).toUpperCase()).filter(_.nonEmpty)

    override def bind(key: String, data: Map[String, String]): Either[Seq[FormError], Option[String]] = {
      val postCode = postCodeDataTransform(data.get(key))
      val country = countryDataTransform(data.get(countryFieldName))
      val maxLengthNonUKPostcode = 10

      (postCode, country, requiredKey) match {
        case (Some(zip), Some("GB"), _) if zip.matches(regexPostcode) => Right(Some(postCodeValidTransformm(zip)))
        case (Some(_), Some("GB"), _) => Left(Seq(FormError(key, invalidKey)))
        case (Some(zip), Some(_), _) if zip.length <= maxLengthNonUKPostcode => Right(Some(zip))
        case (Some(_), Some(_), _) => Left(Seq(FormError(key, nonUkLengthKey)))
        case (Some(zip), None, _) => Right(Some(zip))
        case (None, Some("GB"), Some(rk)) => Left(Seq(FormError(key, rk)))
        case _ => Right(None)
      }
    }

    override def unbind(key: String, value: Option[String]): Map[String, String] =
      Map(key -> value.getOrElse(""))
  }

  private[mappings] def postcodeFormatter(
    requiredKey: String,
    invalidKey: String
  ): Formatter[String] = new Formatter[String] {

    private def tidyPostcode(value:String):String =
      if (value.contains(" ")) {
        value
      } else {
        value.substring(0, value.length - 3) + " " + value.substring(value.length - 3, value.length)
      }

    override def bind(key: String, data: Map[String, String]): Either[Seq[FormError], String] = {
      val postCode = data.get(key).map(_.replaceAll(" {2,}", " ").toUpperCase.trim)
      (postCode, requiredKey) match {
        case (None, rk) => Left(Seq(FormError(key, rk)))
        case (Some(zip), rk) if zip.isEmpty => Left(Seq(FormError(key, rk)))
        case (Some(zip), _) if zip.matches(regexPostcode) => Right(tidyPostcode(zip))
        case (Some(_), _) => Left(Seq(FormError(key, invalidKey)))
      }
    }

    override def unbind(key: String, value: String): Map[String, String] =
      Map(key -> value)
  }
}
