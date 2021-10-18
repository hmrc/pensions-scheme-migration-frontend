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

import models.requests.{AuthenticatedRequest, BulkDataRequest}
import models.{Items, MinPSA}
import play.api.mvc.Result

import scala.concurrent.Future

class MutableFakeBulkDataAction(isRequired: Boolean) extends BulkDataAction {
  override def apply(isRequired: Boolean): BulkRetrieval = new MutableFakeBulkRetrieval
}

class MutableFakeBulkRetrieval extends BulkRetrieval {
  override implicit val executionContext: scala.concurrent.ExecutionContext = scala.concurrent.ExecutionContext.global
  private var minPSAData: MinPSA = MinPSA("test@test.com", false, Some("test company"), None, false, false)
  private var listOfSchemesData: List[Items] = List(Items("test-pstr", "", true, "test-scheme", "", Some("")))

  def setMinPSAReturn(minPSA: MinPSA): Unit = minPSAData = minPSA

  def setLockToReturn(listOfSchemes: List[Items]): Unit = listOfSchemesData = listOfSchemes

  override protected def refine[A](request: AuthenticatedRequest[A]): Future[Either[Result, BulkDataRequest[A]]] = {
    Future(Right(BulkDataRequest(request, minPSAData, listOfSchemesData)))
  }
}
