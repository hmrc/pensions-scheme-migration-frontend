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

package controllers.establishers.company.address

import controllers.ControllerSpecBase
import controllers.actions._
import identifiers.establishers.company.CompanyDetailsId
import matchers.JsonMatchers
import models.NormalMode
import org.scalatest.TryValues
import play.api.i18n.Messages
import play.api.mvc.Result
import play.api.test.Helpers._
import utils.Data.ua
import utils.{Data, UserAnswers}
import views.html.address.WhatYouWillNeedView

import scala.concurrent.Future

class WhatYouWillNeedControllerSpec extends ControllerSpecBase with JsonMatchers with TryValues {

  private val userAnswers: UserAnswers = ua.set(CompanyDetailsId(0), Data.companyDetails).success.value

  private def controller(
                          dataRetrievalAction: DataRetrievalAction
                        ): WhatYouWillNeedController =
    new WhatYouWillNeedController(
      messagesApi = messagesApi,
      authenticate = new FakeAuthAction(),
      getData = dataRetrievalAction,
      requireData = new DataRequiredActionImpl,
      controllerComponents = controllerComponents,
      whatYouWillNeedView = app.injector.instanceOf[WhatYouWillNeedView]
    )

  "WhatYouWillNeedController" must {
    "return OK and the correct view for a GET" in {
      val getData = new FakeDataRetrievalAction(Some(userAnswers))
      val result: Future[Result] = controller(getData).onPageLoad(0)(fakeDataRequest(userAnswers))

      status(result) mustBe OK

      val view = app.injector.instanceOf[WhatYouWillNeedView].apply(
        "test company",
        Messages("messages__title_company"),
        routes.EnterPostcodeController.onPageLoad(0, NormalMode).url,
        "Test scheme name"
      )(fakeRequest, messages)

      compareResultAndView(result, view)
    }
  }
}
