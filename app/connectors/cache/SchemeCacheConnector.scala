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

package connectors.cache

import com.google.inject.{ImplementedBy, Inject}
import config.AppConfig
import connectors.cache.CacheConnector.headers
import play.api.http.Status._
import play.api.libs.json._
import play.api.libs.ws.WSClient
import play.api.mvc.Result
import play.api.mvc.Results._
import uk.gov.hmrc.crypto.PlainText
import uk.gov.hmrc.http.{HeaderCarrier, HttpException}

import scala.concurrent.{ExecutionContext, Future}

@ImplementedBy(classOf[SchemeCacheConnectorImpl])
trait SchemeCacheConnector {

  def fetch(implicit ec: ExecutionContext, hc: HeaderCarrier): Future[Option[JsValue]]

  def save(value: JsValue)(implicit ec: ExecutionContext, hc: HeaderCarrier): Future[JsValue]

  def remove(implicit ec: ExecutionContext, hc: HeaderCarrier): Future[Result]
}

class SchemeCacheConnectorImpl @Inject()(config: AppConfig, http: WSClient) extends SchemeCacheConnector {

  def fetch(implicit ec: ExecutionContext, hc: HeaderCarrier): Future[Option[JsValue]] =
    http
      .url(config.schemeDataCacheUrl)
      .withHttpHeaders(headers(hc): _*)
      .get()
      .flatMap { response =>
        response.status match {
          case NOT_FOUND =>
            Future.successful(None)
          case OK =>
            Future.successful(Some(Json.parse(response.body)))
          case _ =>
            Future.failed(new HttpException(response.body, response.status))
        }
      }

  def save(value: JsValue)
          (implicit ec: ExecutionContext, hc: HeaderCarrier): Future[JsValue] =
    http
      .url(config.schemeDataCacheUrl)
      .withHttpHeaders(headers(hc): _*)
      .post(PlainText(Json.stringify(value)).value)
      .flatMap { response =>
        response.status match {
          case OK =>
            Future.successful(value)
          case _ =>
            Future.failed(new HttpException(response.body, response.status))
        }
      }

  def remove(implicit ec: ExecutionContext, hc: HeaderCarrier): Future[Result] =
    http
      .url(config.schemeDataCacheUrl)
      .withHttpHeaders(headers(hc): _*)
      .delete()
      .map(_ => Ok)
}
