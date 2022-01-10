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
import play.api.mvc.Result
import play.api.mvc.Results._
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpException, HttpResponse}

import scala.concurrent.{ExecutionContext, Future}

class LockCacheConnector @Inject()(config: AppConfig,
                                   http: HttpClient
                                  ) {



  def setLock(lock: MigrationLock)
          (implicit ec: ExecutionContext, headerCarrier: HeaderCarrier): Future[JsValue] = {

    val headers: Seq[(String, String)] = Seq(("pstr", lock.pstr),("psaId", lock.psaId), ("Content-Type", "application/json"))
    val hc: HeaderCarrier = headerCarrier.withExtraHeaders(headers: _*)

    http.POST[JsValue, HttpResponse](config.lockUrl, Json.obj())(implicitly, implicitly, hc, implicitly)
      .map { response =>
        response.status match {
          case OK => Json.toJson(lock)
          case CONFLICT => throw new HttpException("Trying to lock a scheme that is already locked", CONFLICT)
          case _ => throw new HttpException(response.body, response.status)
        }
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
                 (implicit ec: ExecutionContext, headerCarrier: HeaderCarrier): Future[Option[MigrationLock]] = {

    val hc: HeaderCarrier = headerCarrier.withExtraHeaders(headers: _*)

  http.GET[HttpResponse](url)(implicitly, hc, implicitly)
      .map { response =>
            response.status match {
              case NOT_FOUND =>
                None
              case OK =>
                response.json.asOpt[MigrationLock]
              case _ =>
                throw new HttpException(response.body, response.status)
            }
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
            (implicit ec: ExecutionContext, headerCarrier: HeaderCarrier): Future[Result] = {

    val hc: HeaderCarrier = headerCarrier.withExtraHeaders(headers: _*)

    http.DELETE[HttpResponse](url)(implicitly, hc, implicitly).map { _ =>
      Ok
    }
  }
}
