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
import identifiers.aboutMembership.{CurrentMembersId, FutureMembersId}
import identifiers.beforeYouStart.{EstablishedCountryId, SchemeNameId, SchemeTypeId, WorkingKnowledgeId}
import models.{Members, MigrationLock, SchemeType}
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
  val ua: UserAnswers = UserAnswers(Json.obj(SchemeNameId.toString -> Data.schemeName))
  val completeUserAnswers: UserAnswers = UserAnswers().set(SchemeNameId, schemeName).flatMap(
    _.set(SchemeTypeId, SchemeType.BodyCorporate).flatMap(
    _.set(EstablishedCountryId, "GB").flatMap(
    _.set(WorkingKnowledgeId, true).flatMap(
    _.set(CurrentMembersId, Members.None).flatMap(
    _.set(FutureMembersId, Members.One)
    ))))).get

  implicit val request: DataRequest[AnyContent] =
    DataRequest(
      request = fakeRequest,
      userAnswers = UserAnswers(Json.obj()),
      psaId = PsaId(psaId),
      lock = MigrationLock(pstr, "dummy cred", psaId)
    )
}
