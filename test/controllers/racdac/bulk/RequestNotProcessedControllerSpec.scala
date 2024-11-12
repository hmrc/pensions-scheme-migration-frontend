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

package controllers.racdac.bulk

import controllers.ControllerSpecBase
import controllers.actions.MutableFakeDataRetrievalAction
import matchers.JsonMatchers
import play.api.Application
import play.api.test.Helpers._
import utils.Enumerable

class RequestNotProcessedControllerSpec extends ControllerSpecBase with JsonMatchers with Enumerable.Implicits {


  private val mutableFakeDataRetrievalAction: MutableFakeDataRetrievalAction = new MutableFakeDataRetrievalAction()

  override def fakeApplication(): Application = applicationBuilderMutableRetrievalAction(mutableFakeDataRetrievalAction).build()

  private def httpPathGET: String = controllers.racdac.bulk.routes.RequestNotProcessedController.onPageLoad.url

  override def beforeEach(): Unit = {
    super.beforeEach()
  }

  "RequestNotProcessedController" must {

    "return OK and the correct view for a GET" in {
      val req = httpGETRequest(httpPathGET)
      val result = route(app, req).value

      status(result) mustEqual OK
      compareResultAndView(result,
        app.injector.instanceOf[views.html.racdac.RequestNotProcessedView].apply(
          routes.TransferAllController.onPageLoad.url
        )(req, implicitly))
    }

  }
}
