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

import identifiers.beforeYouStart.SchemeNameId
import org.scalatest._
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.{JsPath, JsString, Json}

class UserAnswersSpec
  extends AnyWordSpec
    with Matchers
    with OptionValues
    with RecoverMethods
    with TryValues {

  import utils.UserAnswersSpec._

  "UserAnswers" when {
    ".get by path" must {

      "get a matching result" in {
        val userAnswers: UserAnswers = UserAnswers(schemeNameJson)
        userAnswers.get(JsPath \ "schemeName").value mustBe JsString("Test scheme name")
      }

      "return empty when no matches" in {
        val userAnswers = UserAnswers(schemeNameJson)
        userAnswers.get(JsPath \ "schemeNameXYZ").isDefined mustBe false
      }
    }

    ".get by id" must {

      "get a matching result" in {
        val userAnswers: UserAnswers = UserAnswers(schemeNameJson)
        userAnswers.get[String](SchemeNameId).value mustBe "Test scheme name"
      }

      "return empty when no matches" in {
        val userAnswers = UserAnswers(Json.obj())
        userAnswers.get[String](SchemeNameId).isDefined mustBe false
      }
    }

    ".getOrException" must {
      "get a matching result" in {
        val userAnswers: UserAnswers = UserAnswers(schemeNameJson)
        userAnswers.getOrException[String](SchemeNameId) mustBe "Test scheme name"
      }

      "throw RuntimeException if not matched" in {
        val userAnswers: UserAnswers = UserAnswers(Json.obj())
        val ex = intercept[RuntimeException] {
          userAnswers.getOrException[String](SchemeNameId)
        }

        ex.getMessage mustBe "Expected a value but none found for schemeName"
      }
    }

    ".set by path" must {
      "set values" in {
        val userAnswers: UserAnswers = UserAnswers(Json.obj())
        userAnswers.set(JsPath \ "schemeName", JsString(schemeName)).success.value mustBe
          UserAnswers(Json.obj("schemeName" -> schemeName))
      }
    }

    ".set by id" must {
      "set values" in {
        val userAnswers: UserAnswers = UserAnswers(Json.obj())
        userAnswers.set[String](SchemeNameId, schemeName).success.value mustBe
          UserAnswers(Json.obj("schemeName" -> schemeName))
      }
    }

    ".setOrException by path" must {
      "set values" in {
        val userAnswers: UserAnswers = UserAnswers(Json.obj())
        userAnswers.setOrException(JsPath \ "schemeName", JsString(schemeName)) mustBe
          UserAnswers(Json.obj("schemeName" -> schemeName))
      }
    }

    ".setOrException by id" must {
      "set values" in {
        val userAnswers: UserAnswers = UserAnswers(Json.obj())
        userAnswers.setOrException[String](SchemeNameId, schemeName) mustBe
          UserAnswers(Json.obj("schemeName" -> schemeName))
      }
    }

    ".remove by path" must {
      "remove values if matched" in {
        val userAnswers: UserAnswers = UserAnswers(schemeNameJson)
        val ua2: UserAnswers = userAnswers.remove(JsPath \ "schemeName")
        ua2.get(JsPath \ "schemeName").isDefined mustBe false
      }
    }

    ".remove by id" must {
      "remove values if matched" in {
        val userAnswers: UserAnswers = UserAnswers(schemeNameJson)
        val ua2: UserAnswers = userAnswers.remove(SchemeNameId)
        ua2.get[String](SchemeNameId).isDefined mustBe false
      }
    }

    "readEstablishers" must {
      "throw UnrecognisedEstablisherKindException for unrecognized establisher kind" in {
        val invalidJson = Json.obj(
          "establishers" -> Json.arr(
            Json.obj(
              "establisherKind" -> "unknownKind"
            )
          )
        )

        val userAnswers = UserAnswers(invalidJson)

        assertThrows[UnrecognisedEstablisherKindException.type] {
          userAnswers.readEstablishers.reads(invalidJson).get
        }
      }
    }

  }
}

object UserAnswersSpec {
  private val schemeName = "Test scheme name"
  private val schemeNameJson = Json.obj("schemeName" -> schemeName)
}