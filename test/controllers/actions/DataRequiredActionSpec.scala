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

import base.SpecBase
import models.requests.{DataRequest, OptionalDataRequest}
import models.{RacDac, Scheme}
import org.mockito.MockitoSugar
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures.whenReady
import play.api.mvc.Result
import play.api.test.Helpers._
import uk.gov.hmrc.domain.PsaId
import utils.Data.{migrationLock, psaId, ua}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class DataRequiredActionSpec
  extends SpecBase
    with BeforeAndAfterEach
    with MockitoSugar {

  class Harness(isRacDac:Boolean)
    extends DataRequiredImpl(isRacDac) {
    def callTransform[A](request: OptionalDataRequest[A]): Future[Either[Result, DataRequest[A]]] =
      refine(request)
  }
  "DataRequired Action" when {
    "the user has no request and no lock for pension scheme" must {
      "redirect to List of pension scheme" in {
        val action = new Harness(false)

        val futureResult = action.callTransform(
          OptionalDataRequest(
            request = fakeRequest,
            userAnswers = None,
            psaId = PsaId(psaId),
            lock = None
          )
        )
        whenReady(futureResult) { result =>
          val response = result.swap.toOption.get
          response.header.headers.get(LOCATION) mustBe Some(controllers.preMigration.routes.ListOfSchemesController.onPageLoad(Scheme).url)
        }
      }
    }
    "the user has no request and no lock for Rac/Dac" must {
      "redirect to List of Rac/Dac" in {
        val action = new Harness(true)

        val futureResult = action.callTransform(
          OptionalDataRequest(
            request = fakeRequest,
            userAnswers = None,
            psaId = PsaId(psaId),
            lock = None
          )
        )
        whenReady(futureResult) { result =>
          val response = result.swap.toOption.get
          response.header.headers.get(LOCATION) mustBe Some(controllers.preMigration.routes.ListOfSchemesController.onPageLoad(RacDac).url)
        }
      }
    }

    "the user has request and lock for Rac/Dac" must {
      "return data request" in {
        val action = new Harness(true)

        val futureResult = action.callTransform(
          OptionalDataRequest(
            request = fakeRequest,
            userAnswers = Some(ua),
            psaId = PsaId(psaId),
            lock = Some(migrationLock)
          )
        )
        whenReady(futureResult) { result =>
          val response = result.toOption.get
          response.userAnswers mustBe ua
          response.lock mustBe migrationLock
          response.viewOnly mustBe false
        }
      }
    }
  }
}


