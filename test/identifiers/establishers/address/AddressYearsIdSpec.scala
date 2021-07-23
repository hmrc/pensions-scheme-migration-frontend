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

package identifiers.establishers.address

import base.SpecBase
import identifiers.establishers.individual.address.{AddressYearsId, EnterPreviousPostCodeId, PreviousAddressId, PreviousAddressListId}
import org.scalatest.{MustMatchers, OptionValues}
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.libs.json.Json
import utils.{Data, Enumerable, UserAnswers}

class AddressYearsIdSpec extends SpecBase with MustMatchers with ScalaCheckPropertyChecks with OptionValues with Enumerable.Implicits {

  "Cleanup" when {

    val answers = UserAnswers(Json.obj())
      .setOrException(AddressYearsId(0), false)
      .setOrException(PreviousAddressId(0), Data.address)
      .setOrException(PreviousAddressListId(0), 2)
      .setOrException(EnterPreviousPostCodeId(0), Nil)

    "`AddressYearsId` is set to `true`" must {

      val result: UserAnswers = answers.setOrException(AddressYearsId(0), true)

      "remove the data for `PreviousAddressListId`" in {
        result.get(PreviousAddressListId(0)).isDefined mustBe false
      }
      "remove the data for `PreviousAddressId`" in {
        result.get(PreviousAddressId(0)).isDefined mustBe false
      }
      "remove the data for `EnterPreviousPostCodeId`" in {
        result.get(EnterPreviousPostCodeId(0)).isDefined mustBe false
      }
    }

    "`AddressYearsId` is set to `false`" must {

      val result: UserAnswers = answers.setOrException(AddressYearsId(0), false)

      "not remove the data for `PreviousAddressListId`" in {
        result.get(PreviousAddressListId(0)).isDefined mustBe true
      }
      "not remove the data for `PreviousAddressId`" in {
        result.get(PreviousAddressId(0)).isDefined mustBe true
      }
      "not remove the data for `EnterPreviousPostCodeId`" in {
        result.get(EnterPreviousPostCodeId(0)).isDefined mustBe true
      }
    }

  }
}
