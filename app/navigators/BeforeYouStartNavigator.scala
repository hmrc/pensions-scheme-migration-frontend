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

import controllers.beforeYouStartSpoke.routes._
import identifiers._
import identifiers.beforeYouStart.{HaveAnyTrusteesId, SchemeTypeId, EstablishedCountryId, WorkingKnowledgeId}
import models.SchemeType
import models.requests.DataRequest
import play.api.mvc.{Call, AnyContent}
import utils.UserAnswers

class BeforeYouStartNavigator extends Navigator {

  import BeforeYouStartNavigator._

  override protected def routeMap(ua: UserAnswers)
                                 (implicit request: DataRequest[AnyContent]): PartialFunction[Identifier, Call] = {
    case SchemeTypeId if trusteeQuestionRequired(ua.get(SchemeTypeId)) =>
      HaveAnyTrusteesController.onPageLoad()
    case SchemeTypeId | EstablishedCountryId | WorkingKnowledgeId | HaveAnyTrusteesId =>
      CheckYourAnswersController.onPageLoad()
  }

  override protected def editRouteMap(ua: UserAnswers)
                                     (implicit request: DataRequest[AnyContent]): PartialFunction[Identifier, Call] = {
    case SchemeTypeId | EstablishedCountryId | WorkingKnowledgeId | HaveAnyTrusteesId =>
      CheckYourAnswersController.onPageLoad()
  }
}

object BeforeYouStartNavigator {
  private def trusteeQuestionRequired(schemeType:Option[SchemeType]):Boolean = {
    schemeType match {
      case None => false
      case Some(st) => st == SchemeType.BodyCorporate || st == SchemeType.GroupLifeDeath
    }
  }
}
