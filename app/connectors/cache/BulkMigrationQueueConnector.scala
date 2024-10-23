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

import com.google.inject.Inject
import config.AppConfig
import play.api.http.Status._
import play.api.libs.json._
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpException, HttpResponse, NotFoundException, StringContextOps}

import scala.concurrent.{ExecutionContext, Future}

class BulkMigrationQueueConnector @Inject()(config: AppConfig,
                                            http: HttpClientV2
                                           ) {


  def pushAll(psaId: String, requests: JsValue)
             (implicit ec: ExecutionContext, headerCarrier: HeaderCarrier): Future[JsValue] = {

    val headers: Seq[(String, String)] = Seq(("psaId", psaId), ("Content-Type", "application/json"))
    val hc: HeaderCarrier = headerCarrier.withExtraHeaders(headers: _*)

    http.post(url"${config.bulkMigrationEnqueueUrl}")(hc)
      .setHeader(headers: _*).withBody(requests).execute[HttpResponse]
      .recoverWith(mapExceptionsToStatus)
      .map { response =>
        response.status match {
          case OK => requests
          case _ => throw new HttpException(response.body, response.status)
        }
      }
  }

  def isRequestInProgress(psaId: String)
                         (implicit ec: ExecutionContext, headerCarrier: HeaderCarrier): Future[Boolean] = {
    val headers: Seq[(String, String)] = Seq(("psaId", psaId), ("Content-Type", "application/json"))
    val hc: HeaderCarrier = headerCarrier.withExtraHeaders(headers: _*)

    http.get(url"${config.bulkMigrationIsInProgressUrl}")(hc)
      .setHeader(headers: _*).execute[HttpResponse]
      .recoverWith(mapExceptionsToStatus)
      .map { response =>
        response.status match {
          case OK =>
            response.json.as[Boolean]
          case _ =>
            false
        }
      }
  }

  def isAllFailed(psaId: String)
                 (implicit ec: ExecutionContext, headerCarrier: HeaderCarrier): Future[Option[Boolean]] = {

    val headers: Seq[(String, String)] = Seq(("psaId", psaId), ("Content-Type", "application/json"))
    val hc: HeaderCarrier = headerCarrier.withExtraHeaders(headers: _*)

    http.get(url"${config.bulkMigrationIsAllFailedUrl}")(hc)
      .setHeader(headers: _*)
      .execute[HttpResponse]
      .recoverWith(mapExceptionsToStatus)
      .map { response =>
        response.status match {
          case OK =>
            Some(response.json.as[Boolean])
          case NO_CONTENT =>
            None
          case _ =>
            throw new HttpException(response.body, response.status)
        }
      }
  }

  private def mapExceptionsToStatus: PartialFunction[Throwable, Future[HttpResponse]] = {
    case _: NotFoundException =>
      Future.successful(HttpResponse(NOT_FOUND, "Not found"))
  }

  def deleteAll(psaId: String)
               (implicit ec: ExecutionContext, headerCarrier: HeaderCarrier): Future[Boolean] = {
    val headers: Seq[(String, String)] = Seq(("psaId", psaId), ("Content-Type", "application/json"))
    val hc: HeaderCarrier = headerCarrier.withExtraHeaders(headers: _*)

    http.delete(url"${config.bulkMigrationDeleteAllUrl}")(hc)
      .setHeader(headers: _*)
      .execute[HttpResponse].map { response =>
      response.status match {
        case OK =>
          response.json.as[Boolean]
        case _ =>
          throw new HttpException(response.body, response.status)
      }
    }
  }
}
