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
import controllers.beforeYouStartSpoke.routes._
import identifiers.beforeYouStart.{EstablishedCountryId, HaveAnyTrusteesId, SchemeTypeId, WorkingKnowledgeId}
import identifiers.{Identifier, TypedIdentifier}
import models.{NormalMode, SchemeType}
import org.scalatest.prop.TableFor3
import play.api.libs.json.Writes
import play.api.mvc.Call
import utils.UserAnswers

class BeforeYouStartNavigatorSpec
  extends SpecBase
    with NavigatorBehaviour {

  import BeforeYouStartNavigatorSpec._

  private val navigator: CompoundNavigator = injector.instanceOf[CompoundNavigator]

  "BeforeYouStartNavigator" when {
    def navigation: TableFor3[Identifier, UserAnswers, Call] =
      Table(
        ("Id", "UserAnswers", "Next Page"),
        row(SchemeTypeId)(CheckYourAnswersController.onPageLoad()),
        row(SchemeTypeId)(HaveAnyTrusteesController.onPageLoad(), uaWithValue(SchemeTypeId, SchemeType.BodyCorporate)),
        row(SchemeTypeId)(HaveAnyTrusteesController.onPageLoad(), uaWithValue(SchemeTypeId, SchemeType.GroupLifeDeath)),
        row(HaveAnyTrusteesId)(CheckYourAnswersController.onPageLoad()),
        row(EstablishedCountryId)(CheckYourAnswersController.onPageLoad()),
        row(WorkingKnowledgeId)(CheckYourAnswersController.onPageLoad())
      )

    "in NormalMode" must {
      behave like navigatorWithRoutesForMode(NormalMode)(navigator, navigation)
    }
  }
}

object BeforeYouStartNavigatorSpec {
  private def uaWithValue[A](idType:TypedIdentifier[A], idValue:A)(implicit writes: Writes[A]) =
    UserAnswers().set(idType, idValue).toOption
}

