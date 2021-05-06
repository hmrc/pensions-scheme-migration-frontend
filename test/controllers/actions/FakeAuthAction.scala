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

import base.SpecBase.controllerComponents
import models.requests.AuthenticatedRequest
import play.api.mvc.{AnyContent, BodyParser, PlayBodyParsers, Request, Result}
import uk.gov.hmrc.domain.{PsaId, PspId}
import utils.Data

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class FakeAuthAction extends AuthAction {

  override def invokeBlock[A](request: Request[A], block: AuthenticatedRequest[A] => Future[Result]): Future[Result] =
    block(AuthenticatedRequest(request, "id", Some(PsaId(Data.psaId)), None))

  override def parser: BodyParser[AnyContent] = controllerComponents.parsers.defaultBodyParser

  override protected def executionContext: ExecutionContext =
    scala.concurrent.ExecutionContext.Implicits.global
}

object FakeAuthAction extends FakeAuthAction()

class FakeAuthActionPsp @Inject()(bodyParsers: PlayBodyParsers) extends AuthAction {

  override def invokeBlock[A](request: Request[A], block: AuthenticatedRequest[A] => Future[Result]): Future[Result] = {
    block(AuthenticatedRequest(request, "id", None, Some(PspId(Data.pspId))))
  }

  override def parser: BodyParser[AnyContent] =
    bodyParsers.default

  override protected def executionContext: ExecutionContext =
    scala.concurrent.ExecutionContext.Implicits.global
}
