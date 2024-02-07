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
import connectors.PensionsSchemeConnector
import models.psa.PsaDetails
import models.requests.AuthenticatedRequest
import play.api.Logging
import play.api.libs.json.Json
import play.api.mvc.Results.NotFound
import play.api.mvc.{ActionFunction, Result}
import renderer.Renderer
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendHeaderCarrierProvider

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

private class PsaSchemeActionImpl (pstr:String, pensionsSchemeConnector: PensionsSchemeConnector, renderer: Renderer, config: AppConfig)
                          (implicit val executionContext: ExecutionContext)
  extends ActionFunction[AuthenticatedRequest, AuthenticatedRequest] with FrontendHeaderCarrierProvider with Logging {

  private def notFoundTemplate(implicit request: AuthenticatedRequest[_]) =
    renderer.render("notFound.njk", Json.obj("yourPensionSchemesUrl" -> config.yourPensionSchemesUrl)).map(NotFound(_))

  override def invokeBlock[A](request: AuthenticatedRequest[A], block: AuthenticatedRequest[A] => Future[Result]): Future[Result] = {

    val psaId = request.psaId

    val schemeDetails = pensionsSchemeConnector.getSchemeDetails(
      psaId = psaId.id,
      idNumber = pstr,
      schemeIdType = "pstr"
    )(hc(request), executionContext)

    schemeDetails.flatMap { schemeDetails =>
      val admins = (schemeDetails.data \ "psaDetails").as[Seq[PsaDetails]].map(_.id)
      if (admins.contains(psaId.id)) {
        block(request)
      } else {
        notFoundTemplate(request)
      }
    } recoverWith {
      case err =>
        logger.error("scheme details request failed", err)
        notFoundTemplate(request)
    }
  }
}


class PsaSchemeAuthAction @Inject()(pensionsSchemeConnector: PensionsSchemeConnector, renderer: Renderer, config: AppConfig)(implicit ec: ExecutionContext) {
  def apply(pstr: String): ActionFunction[AuthenticatedRequest, AuthenticatedRequest] =
    new PsaSchemeActionImpl(pstr, pensionsSchemeConnector, renderer, config)
}
