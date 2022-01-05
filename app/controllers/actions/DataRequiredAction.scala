/*
 * Copyright 2022 HM Revenue & Customs
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
import models.requests.{DataRequest, OptionalDataRequest}
import models.{RacDac, Scheme}
import play.api.mvc.Results.Redirect
import play.api.mvc.{ActionRefiner, Result}

import scala.concurrent.{ExecutionContext, Future}

class DataRequiredImpl @Inject()(isRacDac: Boolean)(implicit val executionContext: ExecutionContext) extends DataRequired {

  override protected def refine[A](request: OptionalDataRequest[A]): Future[Either[Result, DataRequest[A]]] =
    (request.userAnswers, request.lock) match {
      case (Some(data), Some(lock)) =>
        Future.successful(Right(DataRequest(request.request, data, request.psaId, lock, request.viewOnly)))
      case _ =>
        val migrationType= if(isRacDac) RacDac else Scheme
        Future.successful(Left(Redirect(controllers.preMigration.routes.ListOfSchemesController.onPageLoad(migrationType))))
    }
}

@ImplementedBy(classOf[DataRequiredImpl])
trait DataRequired extends ActionRefiner[OptionalDataRequest, DataRequest]

class DataRequiredActionImpl @Inject()
                              (implicit ec: ExecutionContext) extends DataRequiredAction {

  override def apply(isRacDac: Boolean = false): DataRequired = new DataRequiredImpl(isRacDac)
}

@ImplementedBy(classOf[DataRequiredActionImpl])
trait DataRequiredAction {
  def apply(isRacDac: Boolean = false): DataRequired
}


