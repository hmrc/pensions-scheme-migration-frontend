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
import models.MigrationLock
import play.api.http.Status._
import play.api.libs.json._
import play.api.mvc.Result
import play.api.mvc.Results._
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpException, HttpResponse, NotFoundException, StringContextOps}
import play.api.libs.ws.WSBodyWritables.writeableOf_JsValue

import scala.concurrent.{ExecutionContext, Future}

@ImplementedBy(classOf[UserAnswersCacheConnectorImpl])
trait UserAnswersCacheConnector {

  def fetch(pstr: String)
           (implicit ec: ExecutionContext, hc: HeaderCarrier): Future[Option[JsValue]]

  def save(lock: MigrationLock, value: JsValue)
          (implicit ec: ExecutionContext, hc: HeaderCarrier): Future[JsValue]

  def remove(pstr: String)
            (implicit ec: ExecutionContext, hc: HeaderCarrier): Future[Result]
}

class UserAnswersCacheConnectorImpl @Inject()(config: AppConfig,
                                          http: HttpClientV2
                                         ) extends UserAnswersCacheConnector {

  private def url = s"${config.dataCacheUrl}"

  def fetch(pstr: String)
                    (implicit ec: ExecutionContext, headerCarrier: HeaderCarrier): Future[Option[JsValue]] = {
    val headers: Seq[(String, String)] = Seq(("Content-Type", "application/json"), ("pstr", pstr))
    val hc: HeaderCarrier = headerCarrier.withExtraHeaders(headers*)

    http.get(url"$url")(hc).setHeader(headers*).execute[HttpResponse]
      .recoverWith(mapExceptionsToStatus)
      .map { response =>
        response.status match {
          case NOT_FOUND =>
            None
          case OK =>
            Some(Json.parse(response.body))
          case _ =>
            throw new HttpException(response.body, response.status)
        }
      }
  }

  def save(lock: MigrationLock, value: JsValue)
          (implicit ec: ExecutionContext, headerCarrier: HeaderCarrier): Future[JsValue] = {
    val headers: Seq[(String, String)] = Seq(("pstr", lock.pstr),("psaId", lock.psaId), ("Content-Type", "application/json"))
    val hc: HeaderCarrier = headerCarrier.withExtraHeaders(headers*)

    http.post(url"${config.dataCacheUrl}")(hc).setHeader(headers*).withBody(value).execute[HttpResponse]
      .recoverWith(mapExceptionsToStatus)
      .map { response =>
            response.status match {
              case OK =>
                value
              case _ =>
                throw new HttpException(response.body, response.status)
            }
          }
      }

  def remove(pstr: String)
                        (implicit ec: ExecutionContext, headerCarrier: HeaderCarrier): Future[Result] = {
    val headers: Seq[(String, String)] = Seq(("pstr", pstr))
    val hc: HeaderCarrier = headerCarrier.withExtraHeaders(headers*)
    http.delete(url"$url")(hc).setHeader(headers*).execute[HttpResponse].map { _ =>
      Ok
    }
  }

  private def mapExceptionsToStatus: PartialFunction[Throwable, Future[HttpResponse]] = {
    case _: NotFoundException =>
      Future.successful(HttpResponse(NOT_FOUND, "Not found"))
  }


}
