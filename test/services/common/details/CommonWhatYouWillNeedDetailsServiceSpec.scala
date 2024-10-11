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

package services.common.details

import controllers.ControllerSpecBase
import identifiers.beforeYouStart.SchemeNameId
import models.requests.DataRequest
import org.mockito.ArgumentMatchers.any
import play.api.mvc.AnyContent
import play.api.test.Helpers._
import play.twirl.api.Html
import renderer.Renderer
import services.CommonServiceSpecBase
import utils.{Data, UserAnswers}

import scala.concurrent.Future

class CommonWhatYouWillNeedDetailsServiceSpec extends ControllerSpecBase with CommonServiceSpecBase {

  // Instantiate service
  val service = new CommonWhatYouWillNeedDetailsService(
    controllerComponents = controllerComponents,
    renderer = new Renderer(mockAppConfig, mockRenderer),
    messagesApi = messagesApi
  )

  override def beforeEach(): Unit = reset(mockRenderer)

  val userAnswers: UserAnswers = UserAnswers().setOrException(SchemeNameId, Data.schemeName)
  val fakeReq: DataRequest[AnyContent] = fakeDataRequest(userAnswers)

  "get" should {

    "return OK and render the correct template with all parameters" in {
      when(mockRenderer.render(any(), any())(any())).thenReturn(Future.successful(Html("")))

      val result = service.get(
        template = "test-template.njk",
        name = Some("Test Name"),
        pageTitle = Some("Test Page Title"),
        entityType = Some("Test Entity Type"),
        continueUrl = "/test/continue",
        schemeName = "Test Scheme"
      )(fakeReq, global)

      status(result) mustBe OK
      verify(mockRenderer).render(any(), any())(any())
    }

    "return OK and render the correct template with only mandatory parameters" in {
      when(mockRenderer.render(any(), any())(any())).thenReturn(Future.successful(Html("")))

      val result = service.get(
        template = "test-template.njk",
        continueUrl = "/test/continue",
        schemeName = "Test Scheme"
      )(fakeReq, global)

      status(result) mustBe OK
      verify(mockRenderer).render(any(), any())(any())
    }
  }
}
