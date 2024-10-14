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

package controllers

import controllers.actions.MutableFakeDataRetrievalAction
import matchers.JsonMatchers
import play.api.Application
import play.api.test.Helpers._
import uk.gov.hmrc.nunjucks.NunjucksSupport
import utils.Data.ua
import utils.{Data, Enumerable}
import views.html.YourActionWasNotProcessedView

class YourActionWasNotProcessedControllerSpec extends ControllerSpecBase with JsonMatchers with Enumerable.Implicits {

  private val mutableFakeDataRetrievalAction: MutableFakeDataRetrievalAction = new MutableFakeDataRetrievalAction()

  private val application: Application = applicationBuilderMutableRetrievalAction(mutableFakeDataRetrievalAction).build()

  private def httpPathSchemeGET: String = controllers.routes.YourActionWasNotProcessedController.onPageLoadScheme.url
  private def httpPathRacDacGET: String = controllers.routes.YourActionWasNotProcessedController.onPageLoadRacDac.url


  "YourActionWasNotProcessedController" must {

    "return OK and the correct view for Scheme GET" in {
      mutableFakeDataRetrievalAction.setDataToReturn(Some(ua))
      val request = httpGETRequest(httpPathSchemeGET)
      val result = route(application, request).value

      status(result) mustEqual OK

      val view = application.injector.instanceOf[YourActionWasNotProcessedView].apply(
        controllers.routes.TaskListController.onPageLoad.url,
        Data.schemeName
      )(request, messages)

      compareResultAndView(result, view)

    }

    "return OK and the correct view for RacDac GET" in {
      mutableFakeDataRetrievalAction.setDataToReturn(Some(ua))

      val request = httpGETRequest(httpPathRacDacGET)
      val result = route(application, request).value

      status(result) mustEqual OK

      val view = application.injector.instanceOf[YourActionWasNotProcessedView].apply(
        controllers.racdac.individual.routes.CheckYourAnswersController.onPageLoad.url,
        Data.schemeName
      )(request, messages)

      compareResultAndView(result, view)

    }

  }
}
