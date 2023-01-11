/*
 * Copyright 2023 HM Revenue & Customs
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
import models.{CheckMode, NormalMode}
import play.api.libs.json.Json
import play.api.mvc.{AnyContent, Call}
import utils.Data._
import utils.UserAnswers

class NavigatorSpec extends SpecBase {

  private case object DummyIdentifier extends Identifier

  private val call1: PartialFunction[Identifier, Call] = {
    case DummyIdentifier => Call("GET", "/page1")
  }

  private val call2: PartialFunction[Identifier, Call] = {
    case DummyIdentifier => Call("GET", "/page1")
  }

  private val dummyNavigator: Navigator = new Navigator {
    protected def routeMap(userAnswers: UserAnswers)
                          (implicit request: DataRequest[AnyContent]): PartialFunction[Identifier, Call] = call1
    protected def editRouteMap(userAnswers: UserAnswers)
                          (implicit request: DataRequest[AnyContent]): PartialFunction[Identifier, Call] = call2
  }

  "Navigator" must {
      "go to correct route" in {
        dummyNavigator.nextPageOptional(UserAnswers(Json.obj()), NormalMode)(request) mustBe call1
        dummyNavigator.nextPageOptional(UserAnswers(Json.obj()), CheckMode)(request) mustBe call2
      }
    }
}
