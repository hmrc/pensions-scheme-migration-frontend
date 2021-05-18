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

import com.google.inject.Inject
import connectors.cache.UserAnswersCacheConnector
import controllers.beforeYouStartSpoke.routes._
import identifiers._
import identifiers.beforeYouStart.{EstablishedCountryId, SchemeTypeId, WorkingKnowledgeId}
import models.requests.DataRequest
import play.api.mvc.{AnyContent, Call}
import utils.UserAnswers

class BeforeYouStartNavigator @Inject()(val dataCacheConnector: UserAnswersCacheConnector)
  extends Navigator {

  override protected def routeMap(ua: UserAnswers)
                                 (implicit request: DataRequest[AnyContent]): PartialFunction[Identifier, Call] = {
    case SchemeTypeId | EstablishedCountryId | WorkingKnowledgeId =>
      CheckYourAnswersController.onPageLoad()
  }
}