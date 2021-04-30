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

import identifiers.Identifier
import models.requests.DataRequest
import play.api.mvc.{AnyContent, Call}
import utils.UserAnswers

trait Navigator {
  protected def routeMap(userAnswers: UserAnswers)(implicit request: DataRequest[AnyContent]): PartialFunction[Identifier, Call]

  def nextPageOptional(userAnswers: UserAnswers)
                      (implicit request: DataRequest[AnyContent]): PartialFunction[Identifier, Call] = routeMap(userAnswers)
}
