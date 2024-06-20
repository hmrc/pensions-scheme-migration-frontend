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

package controllers.actions

import config.AppConfig
import connectors.LegacySchemeDetailsConnector
import models.requests.AuthenticatedRequest
import play.api.Logging
import play.api.libs.json.{JsObject, JsValue, Json}
import play.api.mvc.Results.NotFound
import play.api.mvc.{ActionFunction, Result}
import renderer.Renderer
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendHeaderCarrierProvider

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

private class PsaSchemeAuthActionImpl (pstr:String, legacySchemeDetailsConnector: LegacySchemeDetailsConnector,
                                   renderer: Renderer, config: AppConfig)
                                  (implicit val executionContext: ExecutionContext)
  extends ActionFunction[AuthenticatedRequest, AuthenticatedRequest] with FrontendHeaderCarrierProvider with Logging {

  private def notFoundTemplate(implicit request: AuthenticatedRequest[_]): Future[Result] =
    renderer.render("notFound.njk", Json.obj("yourPensionSchemesUrl" -> config.yourPensionSchemesUrl)).map(NotFound(_))

  override def invokeBlock[A](request: AuthenticatedRequest[A], block: AuthenticatedRequest[A] => Future[Result]): Future[Result] = {

    val psaIdStr: String = request.psaId.id

    val legacySchemeDetails: Future[Either[HttpResponse, JsValue]] = legacySchemeDetailsConnector
      .getLegacySchemeDetails(psaIdStr, pstr)(hc(request), executionContext)

    val futureSchemeDetailsForPsaWithPstr: Future[JsObject] = legacySchemeDetails.map {
      case Right(data) => data.as[JsObject]
      case Left(response) =>
        logger.error(s"Called getLegacySchemeDetails, IF responded with status ${response.status}, ${response.body}")
        Json.obj()
    }

    (for {
      result <- futureSchemeDetailsForPsaWithPstr
    } yield {
      if (result.fields.isEmpty) {
        logger.info("getLegacySchemeDetails returned no data for this combination of PsaId and PSTR")
        notFoundTemplate(request)
      } else {
        block(request)
      }
    } recoverWith {
        case err =>
          logger.error("Error resolving futureSchemeDetailsForPsaWithPstr: ", err)
          notFoundTemplate(request)
    }).flatten
  }
}

class PsaSchemeAuthAction @Inject()(legacySchemeDetailsConnector: LegacySchemeDetailsConnector, renderer: Renderer, config: AppConfig)
                                   (implicit ec: ExecutionContext) {
  def apply(pstr: String): ActionFunction[AuthenticatedRequest, AuthenticatedRequest] =
    new PsaSchemeAuthActionImpl(pstr, legacySchemeDetailsConnector, renderer, config)
}
