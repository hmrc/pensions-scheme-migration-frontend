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

import com.google.inject.{ImplementedBy, Inject}
import config.AppConfig
import play.api.http.Status._
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, NotFoundException, StringContextOps}

import scala.concurrent.{ExecutionContext, Future}

@ImplementedBy(classOf[BulkMigrationEventsLogConnectorImpl])
trait BulkMigrationEventsLogConnector {
  def getStatus(implicit ec: ExecutionContext, hc: HeaderCarrier): Future[Int]
}

class BulkMigrationEventsLogConnectorImpl @Inject()(config: AppConfig, http: HttpClientV2) extends BulkMigrationEventsLogConnector {

  private def url = s"${config.bulkMigrationEventsLogStatusUrl}"

  def getStatus(implicit ec: ExecutionContext, headerCarrier: HeaderCarrier): Future[Int] = {

    val headers: Seq[(String, String)] = Seq(("Content-Type", "application/json"))
    val hc: HeaderCarrier = headerCarrier.withExtraHeaders(headers: _*)

    http.get(url"$url")(hc).execute[HttpResponse]
      .recoverWith(mapExceptionsToStatus)
      .map { response =>
        response.status
      }
  }

  private def mapExceptionsToStatus: PartialFunction[Throwable, Future[HttpResponse]] = {
    case _: NotFoundException =>
        Future.successful(HttpResponse(NOT_FOUND, "Not found"))
  }
}
