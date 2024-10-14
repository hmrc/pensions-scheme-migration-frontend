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
import controllers.actions._
import matchers.JsonMatchers
import org.scalatest.TryValues
import play.api.i18n.Messages
import play.api.mvc.{Call, Result}
import play.api.test.Helpers.{status, _}
import uk.gov.hmrc.viewmodels.NunjucksSupport
import views.html.preMigration.CannotMigrateView

import scala.concurrent.Future
class CannotMigrateControllerSpec extends ControllerSpecBase with JsonMatchers with TryValues{

  private def controller(): CannotMigrateController =
    new CannotMigrateController(appConfig, messagesApi, new FakeAuthAction(), controllerComponents,
      app.injector.instanceOf[CannotMigrateView])

  "CannotMigrateController" must {
    "return OK and the correct view for a GET" in {
      val result: Future[Result] = controller().onPageLoad(fakeDataRequest())

      status(result) mustBe OK
      val view = app.injector.instanceOf[CannotMigrateView].apply(
        Messages("messages__administrator__overview"),
        Call("GET", appConfig.psaOverviewUrl)
      )(fakeRequest, messages)
      compareResultAndView(result, view)
    }
  }
}
