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

import identifiers.Identifier
import models.Mode
import models.requests.DataRequest
import navigators.{CompoundNavigator, Navigator}
import play.api.mvc.{AnyContent, Call}

class FakeNavigator(desiredRoute: Call) extends CompoundNavigator(new java.util.HashSet[Navigator]) {

  private[this] var userAnswers: Option[UserAnswers] = None

  def lastUserAnswers: Option[UserAnswers] = userAnswers

  override def nextPage(id: Identifier, ua: UserAnswers, mode: Mode)
                       (implicit request: DataRequest[AnyContent]): Call = {
    this.userAnswers = Some(ua)
    desiredRoute
  }

}

object FakeNavigator extends FakeNavigator(Call("GET", "www.example.com"))
