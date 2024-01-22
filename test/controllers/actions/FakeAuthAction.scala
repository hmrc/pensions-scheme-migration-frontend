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

import base.SpecBase.controllerComponents
import models.requests.AuthenticatedRequest
import play.api.mvc.{AnyContent, BodyParser, Request, Result}
import uk.gov.hmrc.domain.PsaId
import utils.Data

import scala.concurrent.{ExecutionContext, Future}

class FakeAuthAction extends AuthAction {

  override def invokeBlock[A](request: Request[A], block: AuthenticatedRequest[A] => Future[Result]): Future[Result] =
    block(AuthenticatedRequest(request, "id", PsaId(Data.psaId)))

  override def parser: BodyParser[AnyContent] = controllerComponents.parsers.defaultBodyParser

  override protected def executionContext: ExecutionContext =
    scala.concurrent.ExecutionContext.Implicits.global
}

object FakeAuthAction extends FakeAuthAction()
