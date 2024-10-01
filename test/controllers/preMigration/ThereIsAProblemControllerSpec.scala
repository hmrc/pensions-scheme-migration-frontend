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

package controllers.preMigration

import controllers.ControllerSpecBase
import controllers.actions.MutableFakeDataRetrievalAction
import matchers.JsonMatchers
import play.api.Application
import play.api.test.Helpers._
import uk.gov.hmrc.nunjucks.NunjucksSupport
import utils.{Enumerable, UserAnswers}
import views.html.preMigration.ThereIsAProblemView
class ThereIsAProblemControllerSpec extends ControllerSpecBase with NunjucksSupport with JsonMatchers with Enumerable.Implicits {

  private val mutableFakeDataRetrievalAction: MutableFakeDataRetrievalAction = new MutableFakeDataRetrievalAction()

  private val application: Application = applicationBuilderMutableRetrievalAction(mutableFakeDataRetrievalAction).build()

  private def httpPathGET: String = routes.ThereIsAProblemController.onPageLoad.url


  override def beforeEach(): Unit = {
    super.beforeEach()
  }

  "ThereIsAProblemController" must {

    "return OK and the correct view for a GET" in {
      mutableFakeDataRetrievalAction.setDataToReturn(Some(UserAnswers()))

      val request = httpGETRequest(httpPathGET)
      val result = route(application, request).value

      status(result) mustEqual OK
      val view = application.injector.instanceOf[ThereIsAProblemView].apply()(request, messages)
      compareResultAndView(result, view)
    }
  }
}
