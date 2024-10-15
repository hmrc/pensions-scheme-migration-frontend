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

package controllers.establishers.partnership.contact

import controllers.ControllerSpecBase
import controllers.actions.{DataRequiredActionImpl, DataRetrievalAction, FakeAuthAction, FakeDataRetrievalAction}
import identifiers.establishers.partnership.PartnershipDetailsId
import matchers.JsonMatchers
import models.{NormalMode, PartnershipDetails}
import org.scalatest.TryValues
import play.api.i18n.MessagesApi
import play.api.mvc.{Request, Result}
import play.api.test.Helpers._
import utils.Data.{schemeName, ua}
import utils.UserAnswers
import viewmodels.Message
import views.html.WhatYouWillNeedContactView

import scala.concurrent.Future

class WhatYouWillNeedControllerSpec
  extends ControllerSpecBase
    with JsonMatchers
    with TryValues {

  private val partnership: PartnershipDetails = PartnershipDetails("test")
  private val userAnswers: UserAnswers = ua.set(PartnershipDetailsId(0), partnership).success.value

  private def getView(req: Request[_]) = {
    app.injector.instanceOf[WhatYouWillNeedContactView].apply(
      Message("messages__title_partnership"),
      routes.EnterEmailController.onPageLoad(0, NormalMode).url,
      partnership.partnershipName,
      schemeName
    )(req, implicitly)
  }

  private def createController(
                                dataRetrievalAction: DataRetrievalAction
                              ): WhatYouWillNeedController = {
    new WhatYouWillNeedController(
      authenticate = new FakeAuthAction(),
      getData = dataRetrievalAction,
      requireData = new DataRequiredActionImpl,
      view = app.injector.instanceOf[WhatYouWillNeedContactView],
      messagesApi = app.injector.instanceOf[MessagesApi]
    )
  }

  "WhatYouWillNeedPartnershipContactController" must {

    "return OK and the correct view for a GET" in {
      val getData = new FakeDataRetrievalAction(Some(userAnswers))

      val req  = fakeDataRequest(userAnswers)
      val result: Future[Result] = createController(getData).onPageLoad(0)(req)

      status(result) mustBe OK
      compareResultAndView(result, getView(req))
    }

  }

  }
