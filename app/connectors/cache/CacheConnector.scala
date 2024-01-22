/*
 * Copyright 2024 HM Revenue & Customs
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

package connectors.cache

import models.MigrationLock
import uk.gov.hmrc.http.HeaderCarrier

object CacheConnector {

  val names: HeaderCarrier => Seq[String] = hc => Seq(hc.names.authorisation, hc.names.xRequestId, hc.names.xSessionId)

  val headers: HeaderCarrier => Seq[(String, String)] =
    hc => hc.headers(CacheConnector.names(hc)) ++ hc.withExtraHeaders(
      ("content-type", "application/json")
    ).extraHeaders

  val lockHeaders: (HeaderCarrier, MigrationLock) => Seq[(String, String)] =
    (hc, lock) => hc.headers(CacheConnector.names(hc)) ++ hc.withExtraHeaders(
      ("pstr", lock.pstr),
      ("psaId", lock.psaId),
      ("content-type", "application/json")
    ).extraHeaders

  val pstrHeaders: (HeaderCarrier, String) => Seq[(String, String)] =
    (hc, pstr) => hc.headers(CacheConnector.names(hc)) ++ hc.withExtraHeaders(("pstr", pstr)).extraHeaders

  val queueHeaders: (HeaderCarrier, String) => Seq[(String, String)] =
    (hc, psaId) => hc.headers(CacheConnector.names(hc)) ++ hc.withExtraHeaders(
      ("psaId", psaId),
      ("content-type", "application/json")
    ).extraHeaders

}
