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

package services.common.address

import base.SpecBase
import connectors.cache.UserAnswersCacheConnector
import identifiers.TypedIdentifier
import matchers.JsonMatchers
import models.requests.DataRequest
import navigators.CompoundNavigator
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import play.api.data.Form
import play.api.data.Forms.boolean
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.AnyContent
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.twirl.api.Html
import renderer.Renderer
import uk.gov.hmrc.domain.PsaId
import uk.gov.hmrc.http.HeaderCarrier
import utils.Data.migrationLock
import utils.UserAnswers

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import org.scalatest.BeforeAndAfterEach
import play.api.i18n.MessagesApi

class CommonTradingTimeServiceSpec extends SpecBase with JsonMatchers with MockitoSugar with ScalaFutures with BeforeAndAfterEach {

  private val mockRenderer = mock[Renderer]
  private val mockUserAnswersCacheConnector = mock[UserAnswersCacheConnector]
  private val mockNavigator = mock[CompoundNavigator]
  override val messagesApi: MessagesApi = mock[MessagesApi]
  private val form = Form("value" -> boolean)
  private val service = new CommonTradingTimeService(mockRenderer, mockUserAnswersCacheConnector, mockNavigator, messagesApi)
  private val userAnswersId = "test-user-answers-id"
  private val tradingTimeId = new TypedIdentifier[Boolean] {
    override def toString: String = "tradingTimeId"
  }
  implicit val hc: HeaderCarrier = HeaderCarrier()
  implicit val request: DataRequest[AnyContent] = fakeDataRequest()

  override def beforeEach(): Unit = {
    reset(mockRenderer, mockUserAnswersCacheConnector, mockNavigator)
  }

  "CommonTradingTimeService" must {

    "render the view correctly on get" in {
      val userAnswers = UserAnswers(Json.obj("id" -> userAnswersId)).set(tradingTimeId, true).toOption.get
      implicit val request: DataRequest[AnyContent] = DataRequest(FakeRequest(), userAnswers, PsaId("A2110001"), migrationLock)

      val templateCaptor: ArgumentCaptor[String] = ArgumentCaptor.forClass(classOf[String])
      val jsonCaptor: ArgumentCaptor[JsObject] = ArgumentCaptor.forClass(classOf[JsObject])

      when(mockRenderer.render(templateCaptor.capture(), jsonCaptor.capture())(any())).thenReturn(Future.successful(Html("")))

      val result = service.get(Some("test-scheme"), "entityName", "entityType", form, tradingTimeId)(request, global)

      status(result) mustBe OK
      verify(mockRenderer, times(1)).render(templateCaptor.capture(), jsonCaptor.capture())(any())

      templateCaptor.getValue mustBe "address/tradingTime.njk"
      (jsonCaptor.getValue \ "schemeName").as[String] mustBe "test-scheme"
      (jsonCaptor.getValue \ "entityName").as[String] mustBe "entityName"
      (jsonCaptor.getValue \ "entityType").as[String] mustBe "entityType"
    }

//    "return a BadRequest and errors when invalid data is submitted on post" in {
//      val psaId = PsaId("A2110001")
//      val userAnswers = UserAnswers(Json.obj("id" -> userAnswersId))
//      val request: DataRequest[AnyContent] = DataRequest(FakeRequest().withFormUrlEncodedBody("value" -> ""), userAnswers, psaId, migrationLock)
//
//      when(mockRenderer.render(any(), any())(any())).thenReturn(Future.successful(Html("")))
//
//      val result = service.post(Some("test-scheme"), "entityName", "entityType", form, tradingTimeId)(request, global, hc)
//
//      status(result) mustBe BAD_REQUEST
//      verify(mockRenderer, times(1)).render(any(), any())(any())
//    }
//
//    "save the data and redirect correctly on post" in {
//      val psaId = PsaId("A2110001")
//      val userAnswers = UserAnswers(Json.obj("id" -> userAnswersId))
//      val request: DataRequest[AnyContent] = DataRequest(FakeRequest().withFormUrlEncodedBody("value" -> "true"), userAnswers, psaId, migrationLock)
//
//      when(mockUserAnswersCacheConnector.save(any(), any())(any(), any())).thenReturn(Future.successful(Json.obj()))
//      when(mockNavigator.nextPage(any(), any(), any())(any())).thenReturn(controllers.routes.TaskListController.onPageLoad)
//
//      val result = service.post(Some("test-scheme"), "entityName", "entityType", form, tradingTimeId)(request, global, hc)
//
//      status(result) mustBe SEE_OTHER
//      redirectLocation(result) mustBe Some(controllers.routes.TaskListController.onPageLoad.url)
//      verify(mockUserAnswersCacheConnector, times(1)).save(any(), any())(any(), any())
//    }
  }
}