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
import connectors.{LegacySchemeDetailsConnector, ListOfSchemesConnector}
import models.requests.AuthenticatedRequest
import models.{Items, ListOfLegacySchemes}
import play.api.Logging
import play.api.libs.json.{JsObject, JsValue, Json}
import play.api.mvc.Results.NotFound
import play.api.mvc.{ActionFunction, Result}
import renderer.Renderer
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendHeaderCarrierProvider

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

private class PsaSchemeAuthActionImpl (pstr:String, listOfSchemesConnector: ListOfSchemesConnector, legacySchemeDetailsConnector: LegacySchemeDetailsConnector,
                                   renderer: Renderer, config: AppConfig)
                                  (implicit val executionContext: ExecutionContext)
  extends ActionFunction[AuthenticatedRequest, AuthenticatedRequest] with FrontendHeaderCarrierProvider with Logging {

  private def notFoundTemplate(implicit request: AuthenticatedRequest[_]): Future[Result] =
    renderer.render("notFound.njk", Json.obj("yourPensionSchemesUrl" -> config.yourPensionSchemesUrl)).map(NotFound(_))

  // TODO: address scalastyle complaints
  //scalastyle:off cyclomatic.complexity method.length
  override def invokeBlock[A](request: AuthenticatedRequest[A], block: AuthenticatedRequest[A] => Future[Result]): Future[Result] = {

    val psaIdStr: String = request.psaId.id

    val listOfSchemes: Future[Either[HttpResponse, ListOfLegacySchemes]] = listOfSchemesConnector
      .getListOfSchemes(psaIdStr)(hc(request), executionContext)

    val legacySchemeDetails: Future[Either[HttpResponse, JsValue]] = legacySchemeDetailsConnector
      .getLegacySchemeDetails(psaIdStr, pstr)(hc(request), executionContext)

    val futureMaybeListOfSchemesForPsa: Future[Option[List[Items]]] = listOfSchemes.flatMap {
      case Right(list) => Future.successful(list.items)
      case _ =>
        logger.info("getListOfSchemes returned None for this PsaID")
        Future.successful(None)
    }

    val futureSchemeDetailsForPsaWithPstr: Future[JsObject] = legacySchemeDetails.flatMap {
      case Right(data) => Future.successful(data.as[JsObject])
      case _ =>
        logger.info("getLegacySchemeDetails returned no data for this PsaId and PSTR")
        Future.successful(Json.obj())
    }

    val schemeNameFromListOfSchemes: Future[String] = futureMaybeListOfSchemesForPsa.map {
      case Some(schemes) =>
        schemes.find(_.pstr == pstr).map { scheme => scheme.schemeName }.getOrElse("")
      case _ => ""
    }

    val schemeNameFromSchemeDetails = futureSchemeDetailsForPsaWithPstr.map { schemeDetails =>
      (schemeDetails \ "schemeName").asOpt[String].getOrElse("")
    }

    val futures = for {
      res1 <- schemeNameFromListOfSchemes
      res2 <- schemeNameFromSchemeDetails
    } yield {
      if (res1.isEmpty || res2.isEmpty) {
        logger.info("Scheme name not retrieved from getListOfSchemes/getLegacySchemeDetails or both")
        notFoundTemplate(request)
      } else if (res1 == res2) {
        block(request)
      } else {
        logger.info("Scheme name from getListOfSchemes did not match scheme name from getLegacySchemeDetails")
        notFoundTemplate(request)
      }
    } recoverWith {
      case err =>
        logger.error("Error resolving futures: ", err)
        notFoundTemplate(request)
    }
    futures.flatten
  }
}

class PsaSchemeAuthAction @Inject()(listOfSchemesConnector: ListOfSchemesConnector,
                                    legacySchemeDetailsConnector: LegacySchemeDetailsConnector,renderer: Renderer, config: AppConfig)
                                   (implicit ec: ExecutionContext) {
  def apply(pstr: String): ActionFunction[AuthenticatedRequest, AuthenticatedRequest] =
    new PsaSchemeAuthActionImpl(pstr, listOfSchemesConnector, legacySchemeDetailsConnector, renderer, config)
}
