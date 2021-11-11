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
import config.AppConfig
import controllers.routes._
import models.requests.AuthenticatedRequest
import play.api.mvc.Results._
import play.api.mvc._
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve._
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.domain.PsaId
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import scala.concurrent.{ExecutionContext, Future}

class AuthActionImpl @Inject()(val authConnector: AuthConnector,
                               val parser: BodyParsers.Default,
                               config: AppConfig
                              )(implicit val executionContext: ExecutionContext)
                              extends AuthAction with AuthorisedFunctions {

  override def invokeBlock[A](
                               request: Request[A],
                               block: AuthenticatedRequest[A] => Future[Result]
                             ): Future[Result] = {

    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)

    authorised().retrieve(Retrievals.externalId and Retrievals.allEnrolments) {

      case Some(id) ~ enrolments =>           createAuthRequest(id, enrolments, request, block)
      case _ =>                               Future.successful(Redirect(UnauthorisedController.onPageLoad()))

    } recover {

      case _: NoActiveSession =>              Redirect(config.loginUrl, Map("continue" -> Seq(config.psaOverviewUrl)))
      case _: InsufficientEnrolments =>       Redirect(UnauthorisedController.onPageLoad())
      case _: InsufficientConfidenceLevel =>  Redirect(UnauthorisedController.onPageLoad())
      case _: UnsupportedAuthProvider =>      Redirect(UnauthorisedController.onPageLoad())
      case _: UnsupportedAffinityGroup =>     Redirect(UnauthorisedController.onPageLoad())
      case _: UnsupportedCredentialRole =>    Redirect(UnauthorisedController.onPageLoad())
      case _: IdNotFound =>                   Redirect(YouNeedToRegisterController.onPageLoad())
    }
  }

  private def createAuthRequest[A](id: String,
                                   enrolments: Enrolments,
                                   request: Request[A],
                                   block: AuthenticatedRequest[A] => Future[Result]
                                  ): Future[Result] = {

    enrolments.getEnrolment("HMRC-PODS-ORG").flatMap(_.getIdentifier("PSAID")) match {
      case Some(psaId) => block(AuthenticatedRequest(request, id, PsaId(psaId.value)))
      case _ => Future.successful(Redirect(YouNeedToRegisterController.onPageLoad()))
    }
  }

}

@ImplementedBy(classOf[AuthActionImpl])
trait AuthAction extends ActionBuilder[AuthenticatedRequest, AnyContent] with ActionFunction[Request,
  AuthenticatedRequest]

case class IdNotFound(msg: String = "PsaIdNotFound") extends AuthorisationException(msg)
