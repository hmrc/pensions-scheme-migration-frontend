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
import controllers.establishers.routes.EstablisherKindController
import identifiers.Identifier
import identifiers.establishers.{AddEstablisherId, EstablisherKindId}
import identifiers.establishers.individual.EstablisherNameId
import models.Index
import models.establishers.EstablisherKind
import org.scalatest.prop.TableFor3
import play.api.mvc.Call
import utils.{Enumerable, UserAnswers}

class EstablishersNavigatorSpec
  extends SpecBase
    with NavigatorBehaviour
    with Enumerable.Implicits {

  private val navigator: CompoundNavigator = injector.instanceOf[CompoundNavigator]
  private val index: Index = Index(0)
  private val uaWithEstablisherKind: EstablisherKind => UserAnswers = kind => UserAnswers().set(EstablisherKindId(index), kind).get
  private val establisherNamePage: Call = controllers.establishers.individual.routes.EstablisherNameController.onPageLoad(index)
  private val addEstablisherPage: Call = controllers.establishers.routes.AddEstablisherController.onPageLoad()
  private val taskListPage: Call = controllers.routes.TaskListController.onPageLoad()
  private val establisherKindPage: Call = EstablisherKindController.onPageLoad(index)
  private val indexPage: Call = controllers.routes.IndexController.onPageLoad()

  "EstablishersNavigator" when {
    def navigation: TableFor3[Identifier, UserAnswers, Call] =
      Table(
        ("Id", "UserAnswers", "Next Page"),
        row(EstablisherKindId(index))(establisherNamePage, Some(uaWithEstablisherKind(EstablisherKind.Individual))),
        row(EstablisherKindId(index))(indexPage, Some(uaWithEstablisherKind(EstablisherKind.Company))),
        row(EstablisherNameId(index))(addEstablisherPage),
        row(AddEstablisherId(Some(true)))(establisherKindPage),
        row(AddEstablisherId(Some(false)))(taskListPage)
      )

    "in NormalMode" must {
      behave like navigatorWithRoutesForMode(navigator, navigation)
    }
  }
}
