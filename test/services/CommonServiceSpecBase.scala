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

package services

import base.SpecBase
import matchers.JsonMatchers
import models.requests.DataRequest
import org.scalatestplus.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfterEach, TryValues}
import play.api.mvc.{AnyContent, AnyContentAsFormUrlEncoded}
import play.api.test.FakeRequest
import utils.UserAnswers

trait CommonServiceSpecBase extends SpecBase with JsonMatchers with TryValues
  with BeforeAndAfterEach with MockitoSugar {

  val fakeDataRequest: DataRequest[AnyContent] = fakeDataRequest()

  def fakeDataRequest(answers: UserAnswers, request: FakeRequest[AnyContentAsFormUrlEncoded]): DataRequest[AnyContent] =
    DataRequest(request, answers, fakeDataRequest.psaId, fakeDataRequest.lock)
}
