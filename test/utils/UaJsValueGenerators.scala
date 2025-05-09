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

package utils

import org.scalacheck.Gen
import play.api.libs.json.Reads._
import play.api.libs.json._

import java.time.LocalDate

trait UaJsValueGenerators {
  val addressLineGen: Gen[String] = Gen.listOfN[Char](35, Gen.alphaChar).map(_.mkString)
  val addressLineOptional: Gen[Option[String]] = Gen.option(addressLineGen)
  val postalCodeGem: Gen[String] = Gen.listOfN[Char](10, Gen.alphaChar).map(_.mkString)

  def randomNumberFromRange(min: Int, max: Int): Int = Gen.chooseNum(min, max).sample.fold(min)(c => c)

  val countryCode: Gen[String] = Gen.oneOf(Seq("GB", "IT"))
  val nameGenerator: Gen[String] = Gen.listOfN[Char](randomNumberFromRange(1, 35), Gen.alphaChar).map(_.mkString)
  val ninoGenerator: Gen[String] = Gen.const("SL221122D")
  val utrGenerator: Gen[String] = Gen.listOfN[Char](10, Gen.numChar).map(_.mkString)
  val dateGenerator: Gen[LocalDate] = for {
    day <- Gen.choose(1, 28)
    month <- Gen.choose(1, 12)
    year <- Gen.choose(1990, 2000)
  } yield LocalDate.of(year, month, day)

  protected def optionalWithReason(key: String, element: Option[String], reason: String): JsObject = {
    element.map { value =>
      Json.obj(key -> value)
    }.getOrElse(Json.obj(reason -> reason))
  }

  protected def optional(key: String, element: Option[String]): JsObject = {
    element.map { value =>
      Json.obj(key -> value)
    }.getOrElse(Json.obj())
  }

  private val contactDetailsJsValueGen = for {
    email <- Gen.const("aaa@gmail.com")
    phone <- Gen.listOfN[Char](randomNumberFromRange(1, 24), Gen.numChar).map(_.mkString)
  } yield {
    Json.obj(
      "emailAddress" -> email,
      "phoneNumber" -> phone
    )
  }

  def addressJsValueGen: Gen[JsValue] = for {
    line1 <- addressLineGen
    line2 <- addressLineGen
    line3 <- addressLineGen
    line4 <- addressLineGen
    postcode <- postalCodeGem
    country <- countryCode
  } yield {
    Json.obj(
      "address" -> Json.obj(
        "addressLine1" -> line1,
          "addressLine2" -> line2,
          "addressLine3" -> line3,
        "addressLine4" ->line4,
          "postcode" -> {if (country == "GB") "ZZ11ZZ" else postcode},
          "country" -> country
        )
    )
  }

  def uaJsValueWithNino: Gen[JsObject] = for {
    trusteeDetails <- trusteeIndividualJsValueGen(isNinoAvailable = true, 1)
    estComDetails <- estCompanyWithNinoInDirJsValueGen(isNinoAvailable = true)
  } yield {
    Json.obj(
      "establishers" -> Seq(estComDetails),
        "trustees" -> Seq(trusteeDetails)
    )
  }

  def uaJsValueWithNoNino: Gen[JsObject] = for {
    trusteeDetails <- trusteeIndividualJsValueGen(isNinoAvailable = false, 1)
    trusteeDetailsFour <- trusteeIndividualJsValueGen(isNinoAvailable = true, 4)
    estComDetails <- estCompanyWithNinoInDirJsValueGen(isNinoAvailable = false)
  } yield {
    Json.obj(
      "establishers" -> Seq(estComDetails),
      "trustees" -> (Seq(trusteeDetails) ++ Seq(trusteeDetailsFour))
    )
  }

  def uaJsValueWithTrusteeMatching: Gen[JsObject] = for {
    trusteeDetails <- trusteeIndividualJsValueGen(isNinoAvailable = true, 1)
    trusteeDetailsFour <- trusteeIndividualJsValueGen(isNinoAvailable = false, 4)
    estComDetails <- singleDirJsValueGen(isNinoAvailable = true)
  } yield {
    Json.obj(
      "establishers" -> Seq(estComDetails),
      "trustees" -> (Seq(trusteeDetails) ++ Seq(trusteeDetailsFour))
    )
  }

  def uaJsValueWithNoTrusteeMatching: Gen[JsObject] = for {
    trusteeDetails <- trusteeIndividualJsValueGen(isNinoAvailable = true, 2)
    trusteeDetailsFour <- trusteeIndividualJsValueGen(isNinoAvailable = false, 4)
    estComDetails <- singleDirJsValueGen(isNinoAvailable = true)
  } yield {
    Json.obj(
      "establishers" -> Seq(estComDetails),
      "trustees" -> (Seq(trusteeDetails) ++ Seq(trusteeDetailsFour))
    )
  }

  def uaJsValueWithDirectorsMatching: Gen[JsObject] = for {
    trusteeDetails <- trusteeIndividualJsValueGen(isNinoAvailable = true, 1)
    estComDetails <- estCompanyWithNinoInDirJsValueGen(isNinoAvailable = true)
  } yield {
    Json.obj(
      "establishers" -> Seq(estComDetails),
      "trustees" -> Seq(trusteeDetails)
    )
  }

  def uaJsValueWithNoDirectorMatching: Gen[JsObject] = for {
    trusteeDetails <- trusteeIndividualJsValueGen(isNinoAvailable = false, 2)
    estComDetails <- estCompanyWithNinoInDirJsValueGen(isNinoAvailable = false)
  } yield {
    Json.obj(
      "establishers" -> Seq(estComDetails),
      "trustees" -> Seq(trusteeDetails)
    )
  }

  def uaJsValueWithNoTrustee: Gen[JsObject] = for {
    estComDetails <- estCompanyWithNinoInDirJsValueGen(isNinoAvailable = false)
  } yield {
    Json.obj(
      "establishers" -> Seq(estComDetails)
    )
  }
  def trusteeIndividualJsValueGen(isNinoAvailable: Boolean, index: Int): Gen[JsObject] = for {
    referenceOrNino <- Gen.const(s"CS700${index}00A")
    email <- Gen.const("aaa@gmail.com")
    phone <- Gen.listOfN[Char](randomNumberFromRange(1, 24), Gen.numChar).map(_.mkString)
    address <- addressJsValueGen
    date <- Gen.const(s"1999-0${index}-13")
  } yield {
    Json.obj(
      "trusteeKind" -> "individual",
      "trusteeDetails" -> Json.obj(
        "firstName" -> "Test",
        "lastName" -> s"User $index"
      ),
      "dateOfBirth" -> date,
      "email" -> email,
      "phone" -> phone,
      "hasUtr" -> false,
      "noUtrReason" -> "no utr",
      "addressYears" -> true
    ) ++ address.as[JsObject] ++ ninoJsValue(isNinoAvailable, referenceOrNino).as[JsObject]
  }

  def estCompanyWithNinoInDirJsValueGen(isNinoAvailable: Boolean): Gen[JsObject] = for {
    orgName <- nameGenerator
    address <- addressJsValueGen
    directorDetails1 <- directorJsValueGen(isDeleted = false, isNinoAvailable, index = 1)
    directorDetails2 <- directorJsValueGen(isDeleted = true, isNinoAvailable, index = 2)
    directorDetails3 <- directorJsValueGen(isDeleted = false, isNinoAvailable, index = 3)
  } yield {
    Json.obj(
      "establisherKind" -> "company",
      "haveCompanyNumber" -> false,
      "havePaye" -> false,
      "haveVat" -> false,
      "noCompanyNumberReason" -> "fsdfdsf",
      "phone" -> "23234",
      "email" -> "sdf@sdf",
      "companyDyDetails" -> Json.obj(
        "companyName" -> orgName
      )
    ) ++ address.as[JsObject] ++ Json.obj("director" -> Seq(directorDetails1.as[JsObject],
      directorDetails2.as[JsObject], directorDetails3.as[JsObject]))
  }

  def singleDirJsValueGen(isNinoAvailable: Boolean): Gen[JsObject] = for {
    orgName <- nameGenerator
    address <- addressJsValueGen
    directorDetails <- directorJsValueGen(isDeleted = false, isNinoAvailable, index = 1)
  } yield {
    Json.obj(
      "establisherKind" -> "company",
      "haveCompanyNumber" -> false,
      "havePaye" -> false,
      "haveVat" -> false,
      "noCompanyNumberReason" -> "fsdfdsf",
      "phone" -> "23234",
      "email" -> "sdf@sdf",
      "companyDyDetails" -> Json.obj(
        "companyName" -> orgName
      )
    ) ++ address.as[JsObject] ++ Json.obj("director" -> Seq(directorDetails.as[JsObject]))
  }

  def directorJsValueGen(isDeleted: Boolean, isNinoAvailable: Boolean, index: Int): Gen[JsValue] = for {
    referenceOrNino <- Gen.const(s"CS700${index}00A")
    contactDetails <- contactDetailsJsValueGen
    address <- addressJsValueGen
    date <- Gen.const(s"1999-0${index}-13")
  } yield {
    Json.obj(
      "directorDetails" -> Json.obj(
        "firstName" -> "Test",
        "lastName" -> s"User $index",
        "isDeleted" -> isDeleted
      ),
      "dateOfBirth" -> date,
      "directorContactDetails" -> contactDetails,
      "hasUtr" -> false,
      "noUtrReason" -> "no utr",
      "addressYears" -> true
    ) ++ address.as[JsObject] ++ ninoJsValue(isNinoAvailable, referenceOrNino).as[JsObject]
  }

  def ninoJsValue(isNinoAvailable: Boolean, referenceOrNino: String): JsValue = {
    if (isNinoAvailable) {
      Json.obj(
        "hasNino" -> true,
        "nino" -> Json.obj(
          "value" -> referenceOrNino,
          "isEditable" -> false
        )
      )
    } else {
      Json.obj("hasNino" -> false,
        "noNinoReason" -> "no nino"
      )
    }
  }
}
