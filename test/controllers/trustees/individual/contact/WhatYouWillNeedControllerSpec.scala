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

package controllers.trustees.individual.contact

import controllers.ControllerSpecBase
import controllers.actions._
import identifiers.trustees.individual.TrusteeNameId
import matchers.JsonMatchers
import models.{NormalMode, PersonName}
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.scalatest.TryValues
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.{Request, Result}
import play.api.test.Helpers._
import play.twirl.api.Html

import uk.gov.hmrc.viewmodels.NunjucksSupport
import utils.Data.{schemeName, ua}
import utils.UserAnswers
import viewmodels.Message
import views.html.WhatYouWillNeedContactView

import scala.concurrent.Future
class WhatYouWillNeedControllerSpec
  extends ControllerSpecBase
    with JsonMatchers
    with TryValues {

  private val personName: PersonName = PersonName("Jane", "Doe")
  private val userAnswers: UserAnswers = ua.set(TrusteeNameId(0), personName).success.value

  private def getView(req: Request[_]) = {
    app.injector.instanceOf[WhatYouWillNeedContactView].apply(
      Message("messages__title_individual"),
      controllers.trustees.individual.contact.routes.EnterEmailController.onPageLoad(0, NormalMode).url,
      personName.fullName,
      schemeName
    )(req, implicitly)
  }

  private def controller(
                          dataRetrievalAction: DataRetrievalAction
                        ): WhatYouWillNeedController =
    new WhatYouWillNeedController(
      messagesApi = messagesApi,
      authenticate = new FakeAuthAction(),
      getData = dataRetrievalAction,
      requireData = new DataRequiredActionImpl,
      view = app.injector.instanceOf[WhatYouWillNeedContactView]
    )

  "WhatYouWillNeedController" must {
    "return OK and the correct view for a GET" in {


      val getData = new FakeDataRetrievalAction(Some(userAnswers))
      val req = fakeDataRequest(userAnswers)
      val result: Future[Result] = controller(getData).onPageLoad(0)(req)

      status(result) mustBe OK
      compareResultAndView(result, getView(req))
    }
  }
}
