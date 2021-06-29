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


  val insurerName= "test insurer"
  val insurerPolicyNo = "test"
  val address = Address("addr1", "addr2", None, None, Some("ZZ11ZZ"), "GB")

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
    .setOrException(InsurerAddressId, address)

  implicit val request: DataRequest[AnyContent] =
    DataRequest(
      request = fakeRequest,
      userAnswers = UserAnswers(Json.obj()),
      psaId = PsaId(psaId),
      lock = MigrationLock(pstr, "dummy cred", psaId)
    )

  val countryCodes: Seq[String] = "AD,AE,AF,AG,AI,AL,AM,AN,AO,AQ,AR,AS,AT,AU,AW,AX,AZ,BA,BB,BD,BE,BF,BG,BH,BI,BJ,BL,BM,BN,BO,BQ,BR,BS,BT,BV,BW,BY,BZ,CA,CC,CD,CF,CG,CH,CI,CK,CL,CM,CN,CO,CR,CS,CU,CV,CW,CX,CY,CZ,DE,DJ,DK,DM,DO,DZ,EC,EE,EG,EH,ER,ES,ET,EU,FC,FI,FJ,FK,FM,FO,FR,GA,GB,GD,GE,GF,GG,GH,GI,GL,GM,GN,GP,GQ,GR,GS,GT,GU,GW,GY,HK,HM,HN,HR,HT,HU,ID,IE,IL,IM,IN,IO,IQ,IR,IS,IT,JE,JM,JO,JP,KE,KG,KH,KI,KM,KN,KP,KR,KW,KY,KZ,LA,LB,LC,LI,LK,LR,LS,LT,LU,LV,LY,MA,MC,MD,ME,MF,MG,MH,MK,ML,MM,MN,MO,MP,MQ,MR,MS,MT,MU,MV,MW,MX,MY,MZ,NA,NC,NE,NF,NG,NI,NL,NO,NP,NR,NT,NU,NZ,OM,OR,PA,PE,PF,PG,PH,PK,PL,PM,PN,PR,PS,PT,PW,PY,QA,RE,RO,RS,RU,RW,SA,SB,SC,SD,SE,SG,SH,SI,SJ,SK,SL,SM,SN,SO,SR,SS,ST,SV,SX,SY,SZ,TC,TD,TF,TG,TH,TJ,TK,TL,TM,TN,TO,TP,TR,TT,TV,TW,TZ,UA,UG,UM,UN,US,UY,UZ,VA,VC,VE,VG,VI,VN,VU,WF,WS,YE,YT,ZA,ZM,ZW".split(",").toSeq
}
