/*
 * Copyright 2022 HM Revenue & Customs
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
import connectors.cache.CacheConnector._
import play.api.http.Status._
import play.api.libs.json._
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpException, HttpResponse, NotFoundException}

import scala.concurrent.{ExecutionContext, Future}

class BulkMigrationQueueConnector @Inject()(config: AppConfig,
                                            http: HttpClient
                                           ) {


  override protected def url = s"${config.bulkMigrationEnqueueUrl}"

  def pushAll(psaId: String, requests: JsValue)
             (implicit ec: ExecutionContext, hc: HeaderCarrier): Future[JsValue] =



    http
      .url(config.bulkMigrationEnqueueUrl)
      .withHttpHeaders(queueHeaders(hc, psaId): _*)
      .post(requests)
      .flatMap { response =>
        response.status match {
          case ACCEPTED => Future.successful(requests)
          case _ => Future.failed(new HttpException(response.body, response.status))
        }
      }

  def isRequestInProgress(psaId: String)
                         (implicit ec: ExecutionContext, hc: HeaderCarrier): Future[Boolean] =
    http
      .url(config.bulkMigrationIsInProgressUrl)
      .withHttpHeaders(queueHeaders(hc, psaId): _*)
      .get()
      .flatMap { response =>
        response.status match {
          case OK =>
            Future.successful(response.json.as[Boolean])
          case _ =>
            Future.successful(false)
        }
      }

  def isAllFailed(psaId: String)
                         (implicit ec: ExecutionContext, hc: HeaderCarrier): Future[Option[Boolean]] =
    http
      .url(config.bulkMigrationIsAllFailedUrl)
      .withHttpHeaders(queueHeaders(hc, psaId): _*)
      .get()
      .flatMap { response =>
        response.status match {
          case OK =>
            Future.successful(Some(response.json.as[Boolean]))
          case NO_CONTENT =>
            Future.successful(None)
          case _ =>
            Future.failed(new HttpException(response.body, response.status))
        }
      }

  def deleteAll(psaId: String)
                 (implicit ec: ExecutionContext, hc: HeaderCarrier): Future[Boolean] =
    http
      .url(config.bulkMigrationDeleteAllUrl)
      .withHttpHeaders(queueHeaders(hc, psaId): _*)
      .delete()
      .flatMap { response =>
        response.status match {
          case OK =>
            Future.successful(response.json.as[Boolean])
          case _ =>
            Future.failed(new HttpException(response.body, response.status))
        }
      }

  private def mapExceptionsToStatus: PartialFunction[Throwable, Future[HttpResponse]] = {
    case _: NotFoundException =>
      Future.successful(HttpResponse(NOT_FOUND, "Not found"))
  }
}
