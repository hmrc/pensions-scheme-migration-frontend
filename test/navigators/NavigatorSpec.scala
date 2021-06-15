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

package navigators

import base.SpecBase
import identifiers.Identifier
import models.requests.DataRequest
import play.api.libs.json.Json
import play.api.mvc.{AnyContent, Call}
import utils.UserAnswers
import utils.Data._

class NavigatorSpec extends SpecBase {

  private case object DummyIdentifier extends Identifier

  private val call1: PartialFunction[Identifier, Call] = {
    case DummyIdentifier => Call("GET", "/page1")
  }

  private val dummyNavigator: Navigator = new Navigator {
    protected def routeMap(userAnswers: UserAnswers)
                          (implicit request: DataRequest[AnyContent]): PartialFunction[Identifier, Call] = call1
  }

  "Navigator" must {
      "go to correct route" in {
        dummyNavigator.nextPageOptional(UserAnswers(Json.obj()))(request) mustBe call1
      }
    }
}
