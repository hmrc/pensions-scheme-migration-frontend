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
import identifiers.{Identifier, TypedIdentifier}
import identifiers.trustees.company.CompanyDetailsId
import models._
import org.scalatest.TryValues
import org.scalatest.prop.TableFor3
import play.api.mvc.Call
import utils.Data.{establisherCompanyDetails, ua}
import utils.{Enumerable, UserAnswers}
import controllers.trustees.company.details.{routes => detailsRoutes}
import identifiers.trustees.company.details._
import play.api.libs.json.Writes


class TrusteesCompanyNavigatorSpec extends SpecBase with NavigatorBehaviour with Enumerable.Implicits with TryValues {

  private val navigator: CompoundNavigator = injector.instanceOf[CompoundNavigator]
  private val index: Index = Index(0)

  private val addTrusteePage: Call = controllers.trustees.routes.AddTrusteeController.onPageLoad()
  private val detailsUa: UserAnswers =
    ua.set(CompanyDetailsId(0), establisherCompanyDetails).success.value
  private def uaWithValue[A](idType:TypedIdentifier[A], idValue:A)(implicit writes: Writes[A]) =
    detailsUa.set(idType, idValue).toOption
  private def companyNumber(mode: Mode = NormalMode): Call = detailsRoutes.CompanyNumberController.onPageLoad(index, mode)
  private def noCompanyNumber(mode: Mode = NormalMode): Call = detailsRoutes.NoCompanyNumberReasonController.onPageLoad(index, mode)
  private def haveUtr(mode: Mode = NormalMode): Call = detailsRoutes.HaveUTRController.onPageLoad(index, mode)
  private def utr(mode: Mode = NormalMode): Call = detailsRoutes.UTRController.onPageLoad(index, mode)
  private def noUtr(mode: Mode = NormalMode): Call = detailsRoutes.NoUTRReasonController.onPageLoad(index, mode)
  private def haveVat(mode: Mode = NormalMode): Call = detailsRoutes.HaveVATController.onPageLoad(index, mode)
  private def vat(mode: Mode = NormalMode): Call = detailsRoutes.VATController.onPageLoad(index, mode)
  private def havePaye(mode: Mode = NormalMode): Call = detailsRoutes.HavePAYEController.onPageLoad(index, mode)
  private def paye(mode: Mode = NormalMode): Call = detailsRoutes.PAYEController.onPageLoad(index, mode)
  private val cyaDetails: Call = detailsRoutes.CheckYourAnswersController.onPageLoad(index)

  "TrusteesCompanyNavigator" when {
    def navigation: TableFor3[Identifier, UserAnswers, Call] =
      Table(
        ("Id", "Next Page", "UserAnswers (Optional)"),
        row(CompanyDetailsId(index))(addTrusteePage),
        row(HaveCompanyNumberId(index))(companyNumber(), uaWithValue(HaveCompanyNumberId(index), true)),
        row(HaveCompanyNumberId(index))(noCompanyNumber(), uaWithValue(HaveCompanyNumberId(index), false)),
        row(CompanyNumberId(index))(haveUtr()),
        row(NoCompanyNumberReasonId(index))(haveUtr()),
        row(HaveUTRId(index))(utr(), uaWithValue(HaveUTRId(index), true)),
        row(HaveUTRId(index))(noUtr(), uaWithValue(HaveUTRId(index), false)),
        row(CompanyUTRId(index))(haveVat()),
        row(NoUTRReasonId(index))(haveVat()),
        row(HaveVATId(index))(vat(), uaWithValue(HaveVATId(index), true)),
        row(HaveVATId(index))(havePaye(), uaWithValue(HaveVATId(index), false)),
        row(VATId(index))(havePaye()),
        row(HavePAYEId(index))(paye(), uaWithValue(HavePAYEId(index), true)),
        row(HavePAYEId(index))(cyaDetails, uaWithValue(HavePAYEId(index), false)),
        row(PAYEId(index))(cyaDetails)
      )

    def editNavigation: TableFor3[Identifier, UserAnswers, Call] =
      Table(
        ("Id", "Next Page", "UserAnswers (Optional)"),
        row(CompanyDetailsId(index))(controllers.routes.IndexController.onPageLoad()),
        row(HaveCompanyNumberId(index))(companyNumber(CheckMode), uaWithValue(HaveCompanyNumberId(index), true)),
        row(HaveCompanyNumberId(index))(noCompanyNumber(CheckMode), uaWithValue(HaveCompanyNumberId(index), false)),
        row(CompanyNumberId(index))(cyaDetails),
        row(NoCompanyNumberReasonId(index))(cyaDetails),
        row(HaveUTRId(index))(utr(CheckMode), uaWithValue(HaveUTRId(index), true)),
        row(HaveUTRId(index))(noUtr(CheckMode), uaWithValue(HaveUTRId(index), false)),
        row(CompanyUTRId(index))(cyaDetails),
        row(NoUTRReasonId(index))(cyaDetails),
        row(HaveVATId(index))(vat(CheckMode), uaWithValue(HaveVATId(index), true)),
        row(HaveVATId(index))(havePaye(CheckMode), uaWithValue(HaveVATId(index), false)),
        row(VATId(index))(cyaDetails),
        row(HavePAYEId(index))(paye(CheckMode), uaWithValue(HavePAYEId(index), true)),
        row(HavePAYEId(index))(cyaDetails, uaWithValue(HavePAYEId(index), false)),
        row(PAYEId(index))(cyaDetails)
      )

    "in NormalMode" must {
      behave like navigatorWithRoutesForMode(NormalMode)(navigator, navigation)
    }

    "CheckMode" must {
      behave like navigatorWithRoutesForMode(CheckMode)(navigator, editNavigation)
    }
  }
}
