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
import models.MigrationLock
import play.api.http.Status._
import play.api.libs.json._
import play.api.libs.ws.WSClient
import play.api.mvc.Result
import play.api.mvc.Results._
import uk.gov.hmrc.http.{HeaderCarrier, HttpException}

import scala.concurrent.{ExecutionContext, Future}

class LockCacheConnector @Inject()(config: AppConfig,
                                   http: WSClient
                                  ) {



  def setLock(lock: MigrationLock)
          (implicit ec: ExecutionContext, hc: HeaderCarrier): Future[JsValue] =
    http
      .url(config.lockUrl)
      .withHttpHeaders(lockHeaders(hc, lock): _*)
      .post(Json.obj())
      .flatMap { response =>
        response.status match {
          case OK => Future.successful(Json.toJson(lock))
          case CONFLICT =>//TODO Potential redirect to error/interrupt page or back to beginning of journey
            Future.failed(new HttpException("Trying to lock a scheme that is already locked", CONFLICT))
          case _ => Future.failed(new HttpException(response.body, response.status))
        }
      }

  def getLock(lock: MigrationLock)
             (implicit ec: ExecutionContext, hc: HeaderCarrier): Future[Option[MigrationLock]] =
    get(config.lockUrl, lockHeaders(hc, lock))

  def getLockOnScheme(pstr: String)
           (implicit ec: ExecutionContext, hc: HeaderCarrier): Future[Option[MigrationLock]] =
    get(config.lockOnSchemeUrl, pstrHeaders(hc, pstr))

  def getLockByUser(implicit ec: ExecutionContext, hc: HeaderCarrier): Future[Option[MigrationLock]] =
    get(config.lockByUserUrl, headers(hc))

  private def get(url: String, headers: Seq[(String, String)])
                 (implicit ec: ExecutionContext): Future[Option[MigrationLock]] =

    http
      .url(url)
      .withHttpHeaders(headers: _*)
      .get()
      .flatMap { response =>
        response.status match {
          case NOT_FOUND =>
            Future.successful(None)
          case OK =>
            Future.successful(response.json.asOpt[MigrationLock])
          case _ =>
            Future.failed(new HttpException(response.body, response.status))
        }
      }

  def removeLock(lock: MigrationLock)
            (implicit ec: ExecutionContext, hc: HeaderCarrier): Future[Result] =
    remove(config.lockUrl, lockHeaders(hc, lock))

  def removeLockOnScheme(pstr: String)
                (implicit ec: ExecutionContext, hc: HeaderCarrier): Future[Result] =
    remove(config.lockOnSchemeUrl, pstrHeaders(hc, pstr))

  def removeLockByUser(implicit ec: ExecutionContext, hc: HeaderCarrier): Future[Result] =
    remove(config.lockByUserUrl, headers(hc))

  private def remove(url: String, headers: Seq[(String, String)])
            (implicit ec: ExecutionContext): Future[Result] =
    http
      .url(url)
      .withHttpHeaders(headers: _*)
      .delete()
      .map(_ => Ok)
}
