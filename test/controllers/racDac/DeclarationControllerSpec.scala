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

package controllers.racDac

import connectors.MinimalDetailsConnector
import controllers.ControllerSpecBase
import controllers.actions.FakeAuthAction
import matchers.JsonMatchers
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import play.api.libs.json.{JsObject, Json}
import play.api.test.Helpers._
import play.twirl.api.Html
import renderer.Renderer
import uk.gov.hmrc.nunjucks.NunjucksSupport
import utils.Data.psaName
import utils.Enumerable

import scala.concurrent.Future

class DeclarationControllerSpec extends ControllerSpecBase with NunjucksSupport with JsonMatchers with Enumerable.Implicits {

  private val templateToBeRendered = "racDac/declaration.njk"
  private val mockMinimalDetailsConnector: MinimalDetailsConnector = mock[MinimalDetailsConnector]

  private def jsonToPassToTemplate: JsObject =
    Json.obj(
      "psaName" -> psaName,
      "submitUrl" -> controllers.racDac.routes.DeclarationController.onSubmit().url
    )

  override def beforeEach: Unit = {
    super.beforeEach
    when(mockRenderer.render(any(), any())(any())).thenReturn(Future.successful(Html("")))
    when(mockMinimalDetailsConnector.getPSAName(any(),any())).thenReturn(Future.successful(psaName))
  }

  private def controller(): DeclarationController =
    new DeclarationController(
      messagesApi,
      new FakeAuthAction(),
      mockMinimalDetailsConnector,
      controllerComponents,
      new Renderer(mockAppConfig, mockRenderer)
    )

  "RacDac DeclarationController" must {

    "return OK and the correct view for a GET" in {
      val templateCaptor:ArgumentCaptor[String] = ArgumentCaptor.forClass(classOf[String])
      val jsonCaptor:ArgumentCaptor[JsObject] = ArgumentCaptor.forClass(classOf[JsObject])
      val result = controller().onPageLoad()(fakeDataRequest())
      status(result) mustEqual OK

      verify(mockRenderer, times(1)).render(templateCaptor.capture(), jsonCaptor.capture())(any())

      templateCaptor.getValue mustEqual templateToBeRendered

      jsonCaptor.getValue must containJson(jsonToPassToTemplate)
    }

    "redirect to next page when button is clicked" in {

      val result = controller().onSubmit()(fakeDataRequest())

      status(result) mustEqual SEE_OTHER
      redirectLocation(result) mustBe Some(controllers.routes.IndexController.onPageLoad().url)
    }
  }
}
