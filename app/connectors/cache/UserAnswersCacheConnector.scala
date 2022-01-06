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
import models.MigrationLock
import play.api.http.Status._
import play.api.libs.json._
import play.api.mvc.Result
import play.api.mvc.Results._
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpException, HttpResponse, NotFoundException}

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
                                          http: HttpClient
                                         ) extends UserAnswersCacheConnector {

  private def url = s"${config.dataCacheUrl}"

  def fetch(pstr: String)
                    (implicit ec: ExecutionContext, headerCarrier: HeaderCarrier): Future[Option[JsValue]] = {
    val headers: Seq[(String, String)] = Seq(("Content-Type", "application/json"), ("pstr", pstr))
    val hc: HeaderCarrier = headerCarrier.withExtraHeaders(headers: _*)

    http.GET[HttpResponse](url)(implicitly, hc, implicitly)
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
          (implicit ec: ExecutionContext, hc: HeaderCarrier): Future[JsValue] = {
    val allExtraHeaders = Seq(
     ???
    )
    savePost(allExtraHeaders, url, value)

//    http
//      .url(config.dataCacheUrl)
//      .withHttpHeaders(lockHeaders(hc, lock): _*)
//      .post(PlainText(Json.stringify(value)).value)
//      .flatMap { response =>
//        response.status match {
//          case OK =>
//            Future.successful(value)
//          case _ =>
//            Future.failed(new HttpException(response.body, response.status))
//        }
  }

  def remove(pstr: String)
                        (implicit ec: ExecutionContext, headerCarrier: HeaderCarrier): Future[Result] = {
    val headers: Seq[(String, String)] = Seq(("id", pstr))
    val hc: HeaderCarrier = headerCarrier.withExtraHeaders(headers: _*)
    http.DELETE[HttpResponse](url)(implicitly, hc, implicitly).map { _ =>
      Ok
    }
  }

  private def mapExceptionsToStatus: PartialFunction[Throwable, Future[HttpResponse]] = {
    case _: NotFoundException =>
      Future.successful(HttpResponse(NOT_FOUND, "Not found"))
  }


}
