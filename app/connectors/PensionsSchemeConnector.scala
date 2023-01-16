/*
 * Copyright 2023 HM Revenue & Customs
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

import com.google.inject.Inject
import config.AppConfig
import models.MigrationType
import org.apache.commons.lang3.StringUtils
import play.api.http.Status._
import play.api.libs.json._
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpResponse}
import utils.{HttpResponseHelper, UserAnswers}

import scala.concurrent.{ExecutionContext, Future}

class PensionsSchemeConnector @Inject()(config: AppConfig,
                                        http: HttpClient
                                       ) extends HttpResponseHelper {

  def registerScheme(answers: UserAnswers, psaId: String, migrationType: MigrationType)
                    (implicit hc: HeaderCarrier,
                     ec: ExecutionContext): Future[String] = {

    val url = config.registerSchemeUrl(migrationType)
    http
      .POST[JsValue, HttpResponse](url, Json.toJson(answers.data), Seq("psaId" -> psaId)).map { response =>
      response.status match {
        case OK =>
          val json = Json.parse(response.body)
          (json \ "schemeReferenceNumber").validate[String] match {
            case JsSuccess(value, _) => value
            case JsError(errors) => throw JsResultException(errors)
          }
        case NO_CONTENT => StringUtils.EMPTY
        case _ =>
          handleErrorResponse("POST", url)(response)
      }
    }
  }
}
