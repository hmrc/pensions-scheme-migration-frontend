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

package controllers.trustees.partnership.details

import controllers.ControllerSpecBase
import controllers.actions._
import identifiers.trustees.partnership.PartnershipDetailsId
import matchers.JsonMatchers
import models.{Index, NormalMode}
import org.scalatest.TryValues
import play.api.i18n.Messages
import play.api.mvc.Result
import play.api.test.Helpers.{status, _}
import utils.Data.{partnershipDetails, ua}
import utils.UserAnswers

import scala.concurrent.Future
class WhatYouWillNeedControllerSpec extends ControllerSpecBase with JsonMatchers with TryValues {

  private val index: Index = Index(0)
  private val userAnswers: UserAnswers = ua.set(PartnershipDetailsId(0), partnershipDetails).success.value

  private def controller(dataRetrievalAction: DataRetrievalAction): WhatYouWillNeedController =
    new WhatYouWillNeedController(messagesApi, new FakeAuthAction(), dataRetrievalAction,
      new DataRequiredActionImpl, app.injector.instanceOf[views.html.trustees.partnership.details.WhatYouWillNeedView])

  "WhatYouWillNeedController" must {
    "return OK and the correct view for a GET" in {

      val getData = new FakeDataRetrievalAction(Some(userAnswers))
      val req = fakeDataRequest(userAnswers)
      val result: Future[Result] = controller(getData).onPageLoad(0)(req)

      status(result) mustBe OK
      compareResultAndView(result,
        app.injector.instanceOf[views.html.trustees.partnership.details.WhatYouWillNeedView]
          .apply(
            Messages("messages__partnershipDetails__whatYouWillNeed_title"),
            routes.HaveUTRController.onPageLoad(index, NormalMode).url,
            "test partnership"
          )(req, implicitly)
      )
    }
  }
}
