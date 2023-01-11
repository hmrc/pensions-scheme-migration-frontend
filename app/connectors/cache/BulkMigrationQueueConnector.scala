/*
 * Copyright 2023 HM Revenue & Customs
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
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpException, HttpResponse, NotFoundException}

import scala.concurrent.{ExecutionContext, Future}

class BulkMigrationQueueConnector @Inject()(config: AppConfig,
                                            http: HttpClient
                                           ) {


  def pushAll(psaId: String, requests: JsValue)
             (implicit ec: ExecutionContext, headerCarrier: HeaderCarrier): Future[JsValue] = {

    val headers: Seq[(String, String)] = Seq(("psaId", psaId), ("Content-Type", "application/json"))
    val hc: HeaderCarrier = headerCarrier.withExtraHeaders(headers: _*)

    http.POST[JsValue, HttpResponse](config.bulkMigrationEnqueueUrl, requests)(implicitly, implicitly, hc, implicitly)
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

    http.GET[HttpResponse](config.bulkMigrationIsInProgressUrl)(implicitly, hc, implicitly)
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

    http.GET[HttpResponse](config.bulkMigrationIsAllFailedUrl)(implicitly, hc, implicitly)
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

    http.DELETE[HttpResponse](config.bulkMigrationDeleteAllUrl)(implicitly, hc, implicitly).map { response =>
      response.status match {
        case OK =>
          response.json.as[Boolean]
        case _ =>
          throw new HttpException(response.body, response.status)
      }
    }
  }
}
