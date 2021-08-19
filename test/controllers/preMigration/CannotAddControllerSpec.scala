/*
 * Copyright 2021 HM Revenue & Customs
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
import org.mockito.ArgumentCaptor
import org.mockito.Matchers.any
import org.mockito.Mockito.{times, verify, when}
import org.scalatest.TryValues
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.Result
import play.api.test.Helpers.{status, _}
import play.twirl.api.Html
import renderer.Renderer
import uk.gov.hmrc.viewmodels.NunjucksSupport

import scala.concurrent.Future

class CannotAddControllerSpec extends ControllerSpecBase with NunjucksSupport with JsonMatchers with TryValues with MockitoSugar {

  private val templateToBeRendered: String = "preMigration/cannotAdd.njk"

  private def schemeJson: JsObject = Json.obj(
    "param1" -> msg"messages__cannotAdd__pension_scheme",
    "param2" -> msg"messages__cannotAdd__scheme",
    "continueUrl" -> controllers.routes.IndexController.onPageLoad().url,
    "contactHmrcUrl" -> appConfig.contactHmrcUrl
  )

  val racDacJson: JsObject = Json.obj(
    "param1" -> msg"messages__cannotAdd__racdac",
    "param2" -> msg"messages__cannotAdd__racdac",
    "continueUrl" -> controllers.routes.IndexController.onPageLoad().url,
    "contactHmrcUrl" -> appConfig.contactHmrcUrl
  )

  private def controller(): CannotAddController =
    new CannotAddController(appConfig, messagesApi, new FakeAuthAction(), controllerComponents, new Renderer(mockAppConfig, mockRenderer))

  "CannotAddController" must {
    "return OK and the correct view for a GET for scheme" in {
      when(mockRenderer.render(any(), any())(any())).thenReturn(Future.successful(Html("")))
      val templateCaptor = ArgumentCaptor.forClass(classOf[String])
      val jsonCaptor = ArgumentCaptor.forClass(classOf[JsObject])

      val result: Future[Result] = controller().onPageLoadScheme()(fakeDataRequest())

      status(result) mustBe OK

      verify(mockRenderer, times(1)).render(templateCaptor.capture(), jsonCaptor.capture())(any())

      templateCaptor.getValue mustEqual templateToBeRendered

      jsonCaptor.getValue must containJson(schemeJson)
    }

    "return OK and the correct view for a GET for rac dac" in {
      when(mockRenderer.render(any(), any())(any())).thenReturn(Future.successful(Html("")))
      val templateCaptor = ArgumentCaptor.forClass(classOf[String])
      val jsonCaptor = ArgumentCaptor.forClass(classOf[JsObject])

      val result: Future[Result] = controller().onPageLoadRacDac()(fakeDataRequest())

      status(result) mustBe OK

      verify(mockRenderer, times(1)).render(templateCaptor.capture(), jsonCaptor.capture())(any())

      templateCaptor.getValue mustEqual templateToBeRendered

      jsonCaptor.getValue must containJson(racDacJson)
    }
  }
}