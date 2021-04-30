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

package controllers.actions

import com.google.inject.{ImplementedBy, Inject}
import controllers.routes
import models.requests.AuthenticatedRequest
import play.api.mvc.Results._
import play.api.mvc._
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve._
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.domain.{PsaId, PspId}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import scala.concurrent.{ExecutionContext, Future}

class AuthActionImpl @Inject()(val authConnector: AuthConnector,
               val parser: BodyParsers.Default)
              (implicit val executionContext: ExecutionContext) extends AuthAction with AuthorisedFunctions {

  override def invokeBlock[A](request: Request[A], block: AuthenticatedRequest[A] => Future[Result]): Future[Result] = {
    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)

    authorised().retrieve(Retrievals.externalId and Retrievals.allEnrolments) {
      case Some(id) ~ enrolments =>
        createAuthRequest(id, enrolments, request, block)
      case _ =>
        Future.successful(Redirect(routes.IndexController.onPageLoad()))
    }
  }

  private def createAuthRequest[A](id: String, enrolments: Enrolments, request: Request[A],
                                   block: AuthenticatedRequest[A] => Future[Result]): Future[Result] = {
    (enrolments.getEnrolment("HMRC-PODS-ORG").flatMap(_.getIdentifier("PSAID")).map(p=> PsaId(p.value)),
      enrolments.getEnrolment("HMRC-PODSPP-ORG").flatMap(_.getIdentifier("PSPID")).map(p=> PspId(p.value))) match {
      case (psaId@Some(_), None) => block(AuthenticatedRequest(request, id, psaId, None))
      case (None, pspId@Some(_)) => block(AuthenticatedRequest(request, id, None, pspId))
      case _ => block(AuthenticatedRequest(request, id, None, None))
    }
  }

}

@ImplementedBy(classOf[AuthActionImpl])
trait AuthAction extends ActionBuilder[AuthenticatedRequest, AnyContent] with ActionFunction[Request,
  AuthenticatedRequest]

case class IdNotFound(msg: String = "PsaIdNotFound") extends AuthorisationException(msg)
