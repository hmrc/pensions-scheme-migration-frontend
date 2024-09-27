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

import controllers.actions._
import matchers.JsonMatchers
import models.{RacDac, Scheme}
import org.scalatest.TryValues
import play.api.Application
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import play.api.test.Helpers.{status, _}
import utils.Data.{schemeName, ua}
import views.html.SchemeLockedView

class SchemeLockedControllerSpec extends ControllerSpecBase with JsonMatchers with TryValues  {

  private val mutableFakeDataRetrievalAction: MutableFakeDataRetrievalAction = new MutableFakeDataRetrievalAction()

  private val application: Application = applicationBuilderMutableRetrievalAction(mutableFakeDataRetrievalAction).build()

  private val schemeType = messages("messages__scheme")
  private val returnUrl = controllers.preMigration.routes.ListOfSchemesController.onPageLoad(Scheme).url

  private val racDacSchemeType = messages("messages__racdac")
  private val racDacReturnUrl = controllers.preMigration.routes.ListOfSchemesController.onPageLoad(RacDac).url

  private val httpPathGET = controllers.routes.SchemeLockedController.onPageLoadScheme.url
  private val racDacHttpPathGET = controllers.routes.SchemeLockedController.onPageLoadRacDac.url

  override def beforeEach(): Unit = {
    super.beforeEach()
    mutableFakeDataRetrievalAction.setDataToReturn(Some(ua))
  }

  "SchemeLockedController" must {
    "return OK and the correct view for a GET for scheme" in {
      val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, httpPathGET)

      val view = application.injector.instanceOf[SchemeLockedView].apply(
        schemeName, schemeType, returnUrl
      )(request, messages)

      val result = route(application, httpGETRequest(httpPathGET)).value

      status(result) mustBe OK

      compareResultAndView(result, view)
    }
    "return OK and the correct view for a GET for rac dac" in {
      val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, racDacHttpPathGET)

      val view = application.injector.instanceOf[SchemeLockedView].apply(
        schemeName, racDacSchemeType, racDacReturnUrl
      )(request, messages)

      val result = route(application, httpGETRequest(racDacHttpPathGET)).value

      status(result) mustBe OK

      compareResultAndView(result, view)
    }
  }
}
