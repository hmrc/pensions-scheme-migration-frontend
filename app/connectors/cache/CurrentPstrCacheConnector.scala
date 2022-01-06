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

import com.google.inject.{ImplementedBy, Inject}
import config.AppConfig
import play.api.http.Status._
import play.api.libs.json._
import play.api.mvc.Result
import play.api.mvc.Results._
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpException, HttpResponse, NotFoundException}

import scala.concurrent.{ExecutionContext, Future}

@ImplementedBy(classOf[CurrentPstrCacheConnectorImpl])
trait CurrentPstrCacheConnector {

  def fetch(implicit ec: ExecutionContext, hc: HeaderCarrier): Future[Option[JsValue]]

  def save(value: JsValue)(implicit ec: ExecutionContext, hc: HeaderCarrier): Future[JsValue]

  def remove(implicit ec: ExecutionContext, hc: HeaderCarrier): Future[Result]
}

class CurrentPstrCacheConnectorImpl @Inject()(config: AppConfig, http: HttpClient) extends CurrentPstrCacheConnector {

  private def url = s"${config.schemeDataCacheUrl}"

  def fetch(implicit ec: ExecutionContext, headerCarrier: HeaderCarrier): Future[Option[JsValue]] =

  http.GET[HttpResponse](url)
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

  def save(value: JsValue)
          (implicit ec: ExecutionContext, headerCarrier: HeaderCarrier): Future[JsValue] = {

    val headers: Seq[(String, String)] = Seq(("Content-Type", "application/json"))
    val hc: HeaderCarrier = headerCarrier.withExtraHeaders(headers: _*)

    http.POST[JsValue, HttpResponse](url, value)(implicitly, implicitly, hc, implicitly)
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
  def remove(implicit ec: ExecutionContext, hc: HeaderCarrier): Future[Result] = {
    http.DELETE[HttpResponse](url).map { _ =>
      Ok
    }
  }

  private def mapExceptionsToStatus: PartialFunction[Throwable, Future[HttpResponse]] = {
    case _: NotFoundException =>
        Future.successful(HttpResponse(NOT_FOUND, "Not found"))
  }
}
