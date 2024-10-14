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
import org.mockito.ArgumentMatchers.any
import org.scalatest.TryValues
import play.api.i18n.Messages
import play.api.mvc.Result
import play.api.test.Helpers.{status, _}
import uk.gov.hmrc.viewmodels.NunjucksSupport
import views.html.preMigration.NotRegisterView

import scala.concurrent.Future
class NotRegisterControllerSpec extends ControllerSpecBase with JsonMatchers with TryValues  {

  private val psaName: String = "Nigel"

  private def controller(): NotRegisterController =
    new NotRegisterController(appConfig, messagesApi, new FakeAuthAction(), controllerComponents,
      mockMinimalDetailsConnector, app.injector.instanceOf[NotRegisterView])

  "NotRegisterController" must {
    "return OK and the correct view for a GET for scheme" in {
      when(mockMinimalDetailsConnector.getPSAName(any(),any())).thenReturn(Future.successful(psaName))
      val result: Future[Result] = controller().onPageLoadScheme(fakeDataRequest())

      status(result) mustBe OK
      val view = app.injector.instanceOf[NotRegisterView].apply(
        Messages("messages__pension_scheme"),
        appConfig.contactHmrcUrl,
        appConfig.psaOverviewUrl,
        psaName
      )(fakeRequest, messages)

      compareResultAndView(result, view)
    }

    "return OK and the correct view for a GET for rac dac" in {
      when(mockMinimalDetailsConnector.getPSAName(any(),any())).thenReturn(Future.successful(psaName))
      val result: Future[Result] = controller().onPageLoadRacDac(fakeDataRequest())

      status(result) mustBe OK

      val view = app.injector.instanceOf[NotRegisterView].apply(
        Messages("messages__racdac"),
        appConfig.contactHmrcUrl,
        appConfig.psaOverviewUrl,
        psaName
      )(fakeRequest, messages)

      compareResultAndView(result, view)
    }
  }
}
