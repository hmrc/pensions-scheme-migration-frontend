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

package connectors

import com.google.inject.{ImplementedBy, Inject, Singleton}
import config.AppConfig
import play.api.Logger
import play.api.http.Status._
import play.api.libs.json.JsValue
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps}

import scala.concurrent.{ExecutionContext, Future}

@ImplementedBy(classOf[LegacySchemeDetailsConnectorImpl])
trait LegacySchemeDetailsConnector {

  def getLegacySchemeDetails(psaId: String, pstr: String)
                      (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Either[HttpResponse, JsValue]]

}

@Singleton
class LegacySchemeDetailsConnectorImpl @Inject()(
                                            http: HttpClientV2,
                                            config: AppConfig
                                          ) extends LegacySchemeDetailsConnector {

  private val logger = Logger(classOf[LegacySchemeDetailsConnectorImpl])

  def getLegacySchemeDetails(psaId: String, pstr: String)
                      (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Either[HttpResponse, JsValue]] = {
    val (url, schemeHc) = (config.legacySchemeDetailsUrl, hc.withExtraHeaders("psaId" -> psaId, "pstr" -> pstr))

    http.get(url"$url")(schemeHc).execute[HttpResponse].map { response =>
      response.status match {
        case OK =>
            Right(response.json)
        case _ =>
          logger.error(response.body)
          Left(response)
      }
    }
  }

}

sealed trait LegacySchemeDetailsConnectorException extends Exception

