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

package services

import base.SpecBase
import matchers.JsonMatchers
import models.prefill.IndividualDetails
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks.forAll
import play.api.libs.json.{JsValue, Json}
import utils.{Enumerable, UaJsValueGenerators, UserAnswers}

import java.time.LocalDate

class DataPrefillServiceSpec extends SpecBase with JsonMatchers with Enumerable.Implicits with UaJsValueGenerators {
  private val dataPrefillService = new DataPrefillService()

  "copyAllDirectorsToTrustees" must {
    "copy all the selected directors to trustees" in {
      forAll(uaJsValueWithNino) {
        ua => {
          val result = dataPrefillService.copyAllDirectorsToTrustees(UserAnswers(ua), Seq(1), 0)
          val path = result.data \ "trustees"
          (path \ 0 \ "nino" \ "value").as[String] mustBe "CS700100A"
          (path \ 0 \ "trusteeDetails" \ "firstName").as[String] mustBe "Test"
          (path \ 0 \ "trusteeDetails" \ "lastName").as[String] mustBe "User 1"
        }
      }
    }

    "preserve existing company trustees when copying selected directors to trustees" in {
      val ua = Json.obj(
        "establishers" -> Json.arr(Json.obj(
          "director" -> Json.arr(director("Copied", "Director"))
        )),
        "trustees" -> Json.arr(companyTrustee("Test & Co Trustees Ltd"))
      )

      val result = dataPrefillService.copyAllDirectorsToTrustees(UserAnswers(ua), Seq(0), 0)
      val trustees = result.data \ "trustees"

      (trustees \ 0 \ "trusteeKind").as[String] mustBe "company"
      (trustees \ 0 \ "companyDetails" \ "companyName").as[String] mustBe "Test & Co Trustees Ltd"
      (trustees \ 1 \ "trusteeKind").as[String] mustBe "individual"
      (trustees \ 1 \ "trusteeDetails" \ "firstName").as[String] mustBe "Copied"
      (trustees \ 1 \ "trusteeDetails" \ "lastName").as[String] mustBe "Director"
    }

    "remove empty and partial trustee objects when copying selected directors to trustees" in {
      val ua = Json.obj(
        "establishers" -> Json.arr(Json.obj(
          "director" -> Json.arr(director("Copied", "Director"))
        )),
        "trustees" -> Json.arr(
          companyTrustee("Test & Co Trustees Ltd"),
          Json.obj(),
          Json.obj(
            "isTrusteeNew" -> true,
            "trusteeKind" -> "company"
          )
        )
      )

      val result = dataPrefillService.copyAllDirectorsToTrustees(UserAnswers(ua), Seq(0), 0)
      val trustees = (result.data \ "trustees").as[Seq[JsValue]]

      trustees.size.mustBe(2)
      (result.data \ "trustees" \ 0 \ "companyDetails" \ "companyName").as[String] mustBe "Test & Co Trustees Ltd"
      (result.data \ "trustees" \ 1 \ "trusteeDetails" \ "firstName").as[String] mustBe "Copied"
    }
  }

  "copyAllTrusteesToDirectors" must {
    "copy all the selected trustees to directors" in {
      forAll(uaJsValueWithNoNino) {
        ua => {
          val result = dataPrefillService.copyAllTrusteesToDirectors(UserAnswers(ua), Seq(1), 0)
          val path = result.data \ "establishers" \ 0
          (path \ "director" \ 3 \ "directorDetails" \ "firstName").as[String] mustBe "Test"
          (path \ "director" \ 3 \ "directorDetails" \ "lastName").as[String] mustBe "User 4"
        }
      }
    }

    "copy correct trustee when trustees list is a company, an individual, then a company" in {
      val ua = Json.obj(
        "establishers" -> Json.arr(Json.obj(
          "director" -> Json.arr(director("Existing", "Director"))
        )),
        "trustees" -> Json.arr(
          companyTrustee("First Trustee Ltd"),
          individualTrustee("Copied", "Trustee 1", "CS700100A"),
          companyTrustee("Second Trustee Ltd")
        )
      )

      val result = dataPrefillService.copyAllTrusteesToDirectors(UserAnswers(ua), Seq(0), 0)
      val directors = result.data \ "establishers" \ 0 \ "director"
      val trustees = result.data \ "trustees"

      directors.as[Seq[JsValue]].size.mustBe(2)
      (directors \ 1 \ "directorDetails" \ "firstName").as[String] mustBe "Copied"
      (directors \ 1 \ "directorDetails" \ "lastName").as[String] mustBe "Trustee 1"
      trustees.as[Seq[JsValue]].size.mustBe(3)
      (trustees \ 0 \ "companyDetails" \ "companyName").as[String] mustBe "First Trustee Ltd"
      (trustees \ 2 \ "companyDetails" \ "companyName").as[String] mustBe "Second Trustee Ltd"
    }

    "copy correct trustees when trustees list is an individual, a company, then an individual" in {
      val ua = Json.obj(
        "establishers" -> Json.arr(Json.obj(
          "director" -> Json.arr(director("Existing", "Director"))
        )),
        "trustees" -> Json.arr(
          individualTrustee("Copied", "Trustee 1", "CS700100A"),
          companyTrustee("Denton & Co Trustees Ltd"),
          individualTrustee("Copied", "Trustee 2", "CS700200A")
        )
      )

      val result = dataPrefillService.copyAllTrusteesToDirectors(UserAnswers(ua), Seq(0, 1), 0)
      val directors = result.data \ "establishers" \ 0 \ "director"
      val trustees = result.data \ "trustees"

      directors.as[Seq[JsValue]].size.mustBe(3)
      (directors \ 1 \ "directorDetails" \ "lastName").as[String] mustBe "Trustee 1"
      (directors \ 2 \ "directorDetails" \ "lastName").as[String] mustBe "Trustee 2"
      trustees.as[Seq[JsValue]].size.mustBe(3)
      (trustees \ 1 \ "companyDetails" \ "companyName").as[String] mustBe "Denton & Co Trustees Ltd"
    }
  }

  "getListOfDirectors" must {
    "return the directors which are non deleted, completed and their nino is not matching with any of the existing trustees" in {
      forAll(uaJsValueWithNino) {
        ua => {
          val result = dataPrefillService.getListOfDirectorsToBeCopied(UserAnswers(ua))
          result mustBe Seq(IndividualDetails("Test", "User 3", false, Some("CS700300A"), Some(LocalDate.parse("1999-03-13")), 2, true, Some(0)))
        }
      }
    }

    "return the directors which are non deleted, completed, no nino and their name and dob is not matching with any of the existing trustees" in {
      forAll(uaJsValueWithNoNino) {
        ua => {
          val result = dataPrefillService.getListOfDirectorsToBeCopied(UserAnswers(ua))
          result mustBe Seq(IndividualDetails("Test", "User 3", false, None, Some(LocalDate.parse("1999-03-13")), 2, true, Some(0)))
        }
      }
    }
  }

  "getListOfTrusteesToBeCopied" must {
    "return the trustees which are non deleted, completed, no nino and their name and dob is not matching with any of the existing directors" in {
      forAll(uaJsValueWithNoNino) {
        ua => {
          val result = dataPrefillService.getListOfTrusteesToBeCopied(0)(UserAnswers(ua))
          result mustBe Seq(IndividualDetails("Test", "User 4", false, Some("CS700400A"), Some(LocalDate.parse("1999-04-13")), 1, true, None))
        }
      }
    }

    "return no trustees when their nino is matching with any of the existing directors" in {
      forAll(uaJsValueWithNino) {
        ua => {
          val result = dataPrefillService.getListOfTrusteesToBeCopied(0)(UserAnswers(ua))
          result mustBe Nil
        }
      }
    }


    "return eligible trustees for each establisher company" in {
      forAll(uaJsValueTwoEstablisherCompaniesThreeTrustees) {
        ua =>
          val result1 = dataPrefillService.getListOfTrusteesToBeCopied(0)(UserAnswers(ua))
          val result2 = dataPrefillService.getListOfTrusteesToBeCopied(1)(UserAnswers(ua))

          result1.length mustBe 1
          result2.length mustBe 3
      }
    }
  }

  "findMatchingTrustee" must {
    "return the trustee which is non deleted and their nino, name and dob matched with the directors" in {
      forAll(uaJsValueWithTrusteeMatching) {
        ua => {
          val result = dataPrefillService.findMatchingTrustee(0, 0)(UserAnswers(ua)).get
          result mustBe IndividualDetails("Test", "User 1", false, Some("CS700100A"), Some(LocalDate.parse("1999-01-13")), 0, true, None)
        }
      }
    }

    "return no trustees when their nino, name and dob is not matching with the director" in {
      forAll(uaJsValueWithNoTrusteeMatching) {
        ua => {
          val result = dataPrefillService.findMatchingTrustee(0, 0)(UserAnswers(ua))
          result mustBe None
        }
      }
    }
  }

  "findMatchingDirectors" must {
    "return the list of Directors which is non deleted and their nino, name and dob matched with the trustee" in {
      forAll(uaJsValueWithDirectorsMatching) {
        ua => {
          val result = dataPrefillService.findMatchingDirectors(0)(UserAnswers(ua))
          result mustBe Seq(IndividualDetails("Test", "User 1", false, Some("CS700100A"), Some(LocalDate.parse("1999-01-13")), 0, true, mainIndex = Some(0)))
        }
      }
    }

    "return empty list when their nino, name and dob is not matching with the trustee" in {
      forAll(uaJsValueWithNoDirectorMatching) {
        ua => {
          val result = dataPrefillService.findMatchingDirectors(0)(UserAnswers(ua))
          result mustBe Nil
        }
      }
    }

    "return empty list when trustee is not found" in {
      forAll(uaJsValueWithNoTrustee) {
        ua => {
          val result = dataPrefillService.findMatchingDirectors(1)(UserAnswers(ua))
          result mustBe Nil
        }
      }
    }
  }

  private def companyTrustee(companyName: String) =
    Json.obj(
      "trusteeKind" -> "company",
      "companyDetails" -> Json.obj(
        "companyName" -> companyName
      ),
      "hasCompanyNumber" -> false,
      "noCompanyNumberReason" -> "No company number",
      "hasUtr" -> false,
      "noUtrReason" -> "No UTR",
      "hasVat" -> false,
      "hasPaye" -> false
    )

  private def individualTrustee(firstName: String, lastName: String, nino: String) =
    Json.obj(
      "trusteeKind" -> "individual",
      "trusteeDetails" -> Json.obj(
        "firstName" -> firstName,
        "lastName" -> lastName,
        "isDeleted" -> false
      ),
      "dateOfBirth" -> "1999-01-13",
      "email" -> "trustee@example.com",
      "phone" -> "01234567890",
      "address" -> Json.obj(
        "addressLine1" -> "1 Test Street",
        "addressLine2" -> "Test Town",
        "postcode" -> "ZZ1 1ZZ",
        "country" -> "GB"
      ),
      "addressYears" -> true,
      "hasUtr" -> false,
      "noUtrReason" -> "No UTR",
      "hasNino" -> true,
      "nino" -> Json.obj(
        "value" -> nino,
        "isEditable" -> false
      )
    )

  private def director(firstName: String, lastName: String) =
    Json.obj(
      "directorDetails" -> Json.obj(
        "firstName" -> firstName,
        "lastName" -> lastName,
        "isDeleted" -> false
      ),
      "dateOfBirth" -> "1999-01-13",
      "directorContactDetails" -> Json.obj(
        "emailAddress" -> "director@example.com",
        "phoneNumber" -> "01234567890"
      ),
      "address" -> Json.obj(
        "addressLine1" -> "1 Test Street",
        "addressLine2" -> "Test Town",
        "postcode" -> "ZZ1 1ZZ",
        "country" -> "GB"
      ),
      "addressYears" -> true,
      "hasUtr" -> false,
      "noUtrReason" -> "No UTR",
      "hasNino" -> true,
      "nino" -> Json.obj(
        "value" -> "CS700100A",
        "isEditable" -> false
      )
    )
}


