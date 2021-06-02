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

package connectors

import com.google.inject.ImplementedBy
import config.AppConfig
import play.api.Logger
import play.api.http.Status._
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpResponse}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Try}

@ImplementedBy(classOf[MinimalDetailsConnectorImpl])
trait MinimalDetailsConnector {

  def getPSAEmail(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[String]

  def getPSAName(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[String]

}

@Singleton
class MinimalDetailsConnectorImpl @Inject()(http: HttpClient, config: AppConfig)
  extends MinimalDetailsConnector {

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

  def getPSAName(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[String] = {

    val url = config.getPSAName

    http.GET[HttpResponse](url) map { response =>
      require(response.status == OK)

      response.body

    } andThen logExceptions("name")
  }
}