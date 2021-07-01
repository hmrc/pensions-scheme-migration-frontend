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
import controllers.trustees.individual.details.{routes => detailsRoutes}
import controllers.trustees.routes
import identifiers.Identifier
import identifiers.trustees.TrusteeKindId
import identifiers.trustees.individual.TrusteeNameId
import identifiers.trustees.individual.details._
import models.trustees.TrusteeKind
import models._
import org.scalatest.TryValues
import org.scalatest.prop.TableFor3
import play.api.mvc.Call
import utils.Data.ua
import utils.{Enumerable, UserAnswers}

import java.time.LocalDate

class TrusteesNavigatorSpec
  extends SpecBase
    with NavigatorBehaviour
    with Enumerable.Implicits
    with TryValues {

  private val navigator: CompoundNavigator = injector.instanceOf[CompoundNavigator]
  private val index: Index = Index(0)
  private val uaWithTrusteeKind: TrusteeKind => UserAnswers = kind => UserAnswers().set(TrusteeKindId(index), kind).get
  private val trusteeNamePage: Call = controllers.trustees.individual.routes.TrusteeNameController.onPageLoad(index)
  private val addTrusteePage: Call = controllers.trustees.routes.AddTrusteeController.onPageLoad()
  private val taskListPage: Call = controllers.routes.TaskListController.onPageLoad()
  private val trusteeKindPage: Call = routes.TrusteeKindController.onPageLoad(index)
  private val indexPage: Call = controllers.routes.IndexController.onPageLoad()
  private val detailsUa: UserAnswers =
    ua.set(TrusteeNameId(0), PersonName("Jane", "Doe")).success.value
  private def hasNinoPage(mode: Mode): Call =
    detailsRoutes.TrusteeHasNINOController.onPageLoad(index, mode)
  private def enterNinoPage(mode: Mode): Call =
    detailsRoutes.TrusteeEnterNINOController.onPageLoad(index, mode)
  private def noNinoPage(mode: Mode): Call =
    detailsRoutes.TrusteeNoNINOReasonController.onPageLoad(index, mode)
  private def hasUtrPage(mode: Mode): Call =
    detailsRoutes.TrusteeHasUTRController.onPageLoad(index, mode)
  private def enterUtrPage(mode: Mode): Call =
    detailsRoutes.TrusteeEnterUTRController.onPageLoad(index, mode)
  private def noUtrPage(mode: Mode): Call =
    detailsRoutes.TrusteeNoUTRReasonController.onPageLoad(index, mode)
  private val cya: Call =
    detailsRoutes.CheckYourAnswersController.onPageLoad(index)

  "TrusteesNavigator" when {
    def navigation: TableFor3[Identifier, UserAnswers, Call] =
      Table(
        ("Id", "Next Page", "UserAnswers (Optional)"),
        row(TrusteeKindId(index))(trusteeNamePage, Some(uaWithTrusteeKind(TrusteeKind.Individual))),
        //row(TrusteeKindId(index))(indexPage, Some(uaWithTrusteeKind(TrusteeKind.Company))),
        //row(TrusteeNameId(index))(addTrusteePage),
        //row(AddTrusteeId(Some(true)))(trusteeKindPage),
        //row(AddTrusteeId(Some(false)))(taskListPage),
        row(TrusteeDOBId(index))(hasNinoPage(NormalMode), Some(detailsUa.set(TrusteeDOBId(index), LocalDate.parse("2000-01-01")).success.value)),
        row(TrusteeHasNINOId(index))(enterNinoPage(NormalMode), Some(detailsUa.set(TrusteeHasNINOId(index), true).success.value)),
        row(TrusteeHasNINOId(index))(noNinoPage(NormalMode), Some(detailsUa.set(TrusteeHasNINOId(index), false).success.value)),
        row(TrusteeNINOId(index))(hasUtrPage(NormalMode), Some(detailsUa.set(TrusteeNINOId(index), ReferenceValue("AB123456C")).success.value)),
        row(TrusteeNoNINOReasonId(index))(hasUtrPage(NormalMode), Some(detailsUa.set(TrusteeNoNINOReasonId(index), "Reason").success.value)),
        row(TrusteeHasUTRId(index))(enterUtrPage(NormalMode), Some(detailsUa.set(TrusteeHasUTRId(index), true).success.value)),
        row(TrusteeHasUTRId(index))(noUtrPage(NormalMode), Some(detailsUa.set(TrusteeHasUTRId(index), false).success.value)),
        row(TrusteeUTRId(index))(cya, Some(detailsUa.set(TrusteeUTRId(index), ReferenceValue("1234567890")).success.value)),
        row(TrusteeNoUTRReasonId(index))(cya, Some(detailsUa.set(TrusteeNoUTRReasonId(index), "Reason").success.value)),
      )

    def editNavigation: TableFor3[Identifier, UserAnswers, Call] =
      Table(
        ("Id", "Next Page", "UserAnswers (Optional)"),
        row(TrusteeDOBId(index))(cya, Some(detailsUa.set(TrusteeDOBId(index), LocalDate.parse("2000-01-01")).success.value)),
        row(TrusteeHasNINOId(index))(enterNinoPage(CheckMode), Some(detailsUa.set(TrusteeHasNINOId(index), true).success.value)),
        row(TrusteeHasNINOId(index))(noNinoPage(CheckMode), Some(detailsUa.set(TrusteeHasNINOId(index), false).success.value)),
        row(TrusteeNINOId(index))(cya, Some(detailsUa.set(TrusteeNINOId(index), ReferenceValue("AB123456C")).success.value)),
        row(TrusteeNoNINOReasonId(index))(cya, Some(detailsUa.set(TrusteeNoNINOReasonId(index), "Reason").success.value)),
        row(TrusteeHasUTRId(index))(enterUtrPage(CheckMode), Some(detailsUa.set(TrusteeHasUTRId(index), true).success.value)),
        row(TrusteeHasUTRId(index))(noUtrPage(CheckMode), Some(detailsUa.set(TrusteeHasUTRId(index), false).success.value)),
        row(TrusteeUTRId(index))(cya, Some(detailsUa.set(TrusteeUTRId(index), ReferenceValue("1234567890")).success.value)),
        row(TrusteeNoUTRReasonId(index))(cya, Some(detailsUa.set(TrusteeNoUTRReasonId(index), "Reason").success.value))
      )
    "in NormalMode" must {
      behave like navigatorWithRoutesForMode(NormalMode)(navigator, navigation)
    }

    "CheckMode" must {
      behave like navigatorWithRoutesForMode(CheckMode)(navigator, editNavigation)
    }
  }
}
