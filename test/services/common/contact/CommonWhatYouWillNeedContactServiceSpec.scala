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

package services.common.contact

import models.requests.DataRequest
import org.mockito.ArgumentMatchers.any
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.json.Json
import play.api.mvc.{AnyContent, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.twirl.api.Html
import renderer.Renderer
import services.CommonServiceSpecBase
import uk.gov.hmrc.domain.PsaId
import uk.gov.hmrc.http.HeaderCarrier
import utils.{Data, UserAnswers}
import viewmodels.Message

import scala.concurrent.Future

class CommonWhatYouWillNeedContactServiceSpec extends CommonServiceSpecBase with MockitoSugar with ScalaFutures with BeforeAndAfterEach {

  val renderer: Renderer = new Renderer(mockAppConfig, mockRenderer)
  val service = new CommonWhatYouWillNeedContactService(controllerComponents, renderer, messagesApi)

  private val userAnswersId = "test-user-answers-id"
  implicit val hc: HeaderCarrier = HeaderCarrier()
  implicit val request: DataRequest[AnyContent] = DataRequest(FakeRequest(),
    UserAnswers(Json.obj("id" -> userAnswersId)), PsaId("A2110001"), Data.migrationLock)

  override def beforeEach(): Unit = {
    reset(mockRenderer)
  }

  "CommonWhatYouWillNeedContactService" must {

    "render the view correctly on get" in {
      val name = "Test Name"
      val pageHeading = Message("page.heading")
      val entityType = Message("entity.type")
      val continueUrl = "continue-url"
      val schemeName = "scheme-name"

      when(mockRenderer.render(any(), any())(any())).thenReturn(Future.successful(Html("")))

      val result: Future[Result] = service.get(name, pageHeading, entityType, continueUrl, schemeName)(request, global)

      status(result) mustBe OK
      verify(mockRenderer, times(1)).render(any(), any())(any())
    }
  }
}
