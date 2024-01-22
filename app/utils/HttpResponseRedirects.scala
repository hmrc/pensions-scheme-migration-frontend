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

package utils

import connectors.{AncillaryPsaException, ListOfSchemes5xxException}
import controllers.preMigration.routes
import play.api.mvc.Result
import play.api.mvc.Results.Redirect

import scala.concurrent.Future

object HttpResponseRedirects {
  val listOfSchemesRedirects: PartialFunction[Throwable, Future[Result]] = {
    {
      case _: AncillaryPsaException =>
        Future.successful(Redirect(routes.CannotMigrateController.onPageLoad))
      case _: ListOfSchemes5xxException =>
        Future.successful(Redirect(routes.ThereIsAProblemController.onPageLoad))
      case _: IllegalArgumentException =>
        Future.successful(Redirect(controllers.routes.NotFoundController.onPageLoad))
    }
  }
}
