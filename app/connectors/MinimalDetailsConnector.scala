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

package connectors

import com.google.inject.ImplementedBy
import config.AppConfig
import models.MinPSA
import play.api.Logger
import play.api.http.Status._
import play.api.libs.json.{JsError, JsResultException, JsSuccess, Json}
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpResponse}
import utils.HttpResponseHelper

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Try}

@ImplementedBy(classOf[MinimalDetailsConnectorImpl])
trait MinimalDetailsConnector {

  def getPSAEmail(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[String]

  def getPSAName(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[String]

  def getPSADetails(psaId: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[MinPSA]

}

@Singleton
class MinimalDetailsConnectorImpl @Inject()(http: HttpClient, config: AppConfig)
  extends MinimalDetailsConnector with HttpResponseHelper {

  private val logger  = Logger(classOf[MinimalDetailsConnectorImpl])

  def getPSAEmail(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[String] = {

    val url = config.getPSAEmail

    http.GET[HttpResponse](url) map { response =>
      require(response.status == OK)

      response.body

    } andThen logExceptions("email")

  }

  private def logExceptions(token: String): PartialFunction[Try[String], Unit] = {
    case Failure(t: Throwable) => logger.error(s"Unable to retrieve $token for PSA", t)
  }

  private def logExceptions: PartialFunction[Try[MinPSA], Unit] = {
    case Failure(t: Throwable) => logger.error(s"Unable to retrieve details for PSA", t)
  }

  def getPSAName(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[String] = {

    val url = config.getPSAName

    http.GET[HttpResponse](url) map { response =>
      require(response.status == OK)

      response.body

    } andThen logExceptions("name")
  }

  private val delimitedErrorMsg: String = "DELIMITED_PSAID"

  def getPSADetails(psaId: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[MinPSA] =
    http.GET[HttpResponse](config.getPSAMinDetails)(implicitly, hc.withExtraHeaders("psaId" -> psaId), implicitly) map { response =>
      response.status match {
        case OK =>
          Json.parse(response.body).validate[MinPSA] match {
            case JsSuccess(value, _) => value
            case JsError(errors) => throw JsResultException(errors)
          }
        case FORBIDDEN if response.body.contains(delimitedErrorMsg) => throw new DelimitedAdminException
        case _ => handleErrorResponse("GET", config.getPSAMinDetails)(response)
      }

    } andThen logExceptions
}

class DelimitedAdminException extends
  Exception("The administrator has already de-registered. The minimal details API has returned a DELIMITED PSA response")
