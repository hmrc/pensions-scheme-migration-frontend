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

package utils

import base.SpecBase.fakeRequest
import identifiers.aboutMembership.{FutureMembersId, CurrentMembersId}
import identifiers.beforeYouStart.{SchemeNameId, SchemeTypeId, EstablishedCountryId, WorkingKnowledgeId}
import identifiers.benefitsAndInsurance.{AreBenefitsSecuredId, BenefitsInsurancePolicyId, BenefitsInsuranceNameId, InsurerAddressId, BenefitsTypeId, HowProvideBenefitsId, IsInvestmentRegulatedId, IsOccupationalId}
import identifiers.establishers.individual.EstablisherNameId
import models.benefitsAndInsurance.{BenefitsProvisionType, BenefitsType}
import models.{Address, PersonName, MigrationLock, SchemeType, Members}
import models.requests.DataRequest
import play.api.libs.json.Format.GenericFormat
import play.api.libs.json.Json
import play.api.mvc.AnyContent
import uk.gov.hmrc.domain.PsaId
import utils.Data.ua.writes

object Data {

  val psaId: String = "A2100000"
  val pspId: String = "21000000"
  val credId: String = "id"
  val pstr: String = "pstr"
  val migrationLock: MigrationLock = MigrationLock(pstr, credId, psaId)
  val schemeName: String = "Test scheme name"
  val establisherIndividualName = PersonName("test", "name")
  val ua: UserAnswers =
    UserAnswers()
      .setOrException(SchemeNameId, Data.schemeName)
      .setOrException(EstablisherNameId(0), Data.establisherIndividualName)
  //  UserAnswers(
  //  Json.obj(
  //    SchemeNameId.toString -> Data.schemeName
  //  )
  //)


  val insurerName= "test insurer"
  val insurerPolicyNo = "test"
  val insurerAddress = Address("addr1", "addr2", None, None, Some("ZZ11ZZ"), "GB")

  val completeUserAnswers: UserAnswers = UserAnswers().set(SchemeNameId, schemeName).flatMap(
    _.set(SchemeTypeId, SchemeType.BodyCorporate).flatMap(
    _.set(EstablishedCountryId, "GB").flatMap(
    _.set(WorkingKnowledgeId, true).flatMap(
    _.set(CurrentMembersId, Members.None).flatMap(
    _.set(FutureMembersId, Members.One)
    ))))).get
    .setOrException(IsInvestmentRegulatedId, true)
    .setOrException(IsOccupationalId, true)
    .setOrException(HowProvideBenefitsId, BenefitsProvisionType.MoneyPurchaseOnly)
    .setOrException(BenefitsTypeId, BenefitsType.CashBalanceBenefits)
    .setOrException(AreBenefitsSecuredId, true)
    .setOrException(BenefitsInsuranceNameId, insurerName)
    .setOrException(BenefitsInsurancePolicyId, insurerPolicyNo)
    .setOrException(InsurerAddressId, insurerAddress)

  implicit val request: DataRequest[AnyContent] =
    DataRequest(
      request = fakeRequest,
      userAnswers = UserAnswers(Json.obj()),
      psaId = PsaId(psaId),
      lock = MigrationLock(pstr, "dummy cred", psaId)
    )
}
