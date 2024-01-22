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

package navigators

import base.SpecBase
import identifiers.Identifier
import models.Mode
import org.scalatest.matchers.must.Matchers
import org.scalatest.prop.TableFor3
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.mvc.Call
import utils.Data._
import utils.UserAnswers

trait NavigatorBehaviour
  extends SpecBase
    with Matchers
    with ScalaCheckPropertyChecks {

  protected def row(id: Identifier)
                   (call: Call, ua: Option[UserAnswers] = None): (Identifier, UserAnswers, Call) =
    Tuple3(id, ua.getOrElse(UserAnswers()), call)

  protected def navigatorWithRoutesForMode(
                                            mode: Mode
                                          )(
                                            navigator: CompoundNavigator,
                                            routes: TableFor3[Identifier, UserAnswers, Call]
                                          ): Unit = {
    forAll(routes) {
      (id: Identifier, userAnswers: UserAnswers, call: Call) =>
        s"move from $id to $call in ${Mode.jsLiteral.to(mode)} with data: ${userAnswers.toString}" in {
          val result = navigator.nextPage(id, userAnswers, mode)(request)
          result mustBe call
        }
    }
  }


}
