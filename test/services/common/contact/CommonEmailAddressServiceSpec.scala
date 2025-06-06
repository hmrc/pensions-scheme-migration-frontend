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

import controllers.ControllerSpecBase
import identifiers.TypedIdentifier
import models.requests.DataRequest
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import org.mockito.Mockito._
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.Messages
import play.api.libs.json.Json
import play.api.mvc.AnyContent
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.twirl.api.Html
import services.CommonServiceSpecBase
import uk.gov.hmrc.domain.PsaId
import uk.gov.hmrc.http.HeaderCarrier
import utils.{Data, FakeNavigator, UserAnswers}
import views.html.EmailView

import scala.concurrent.Future

class CommonEmailAddressServiceSpec extends ControllerSpecBase with CommonServiceSpecBase with MockitoSugar with ScalaFutures with BeforeAndAfterEach {

  private val navigator = new FakeNavigator(desiredRoute = onwardCall)
  private val form = Form("value" -> email)

  val emailView: EmailView = mock[views.html.EmailView]

  private val service = new CommonEmailAddressService(
    controllerComponents, mockUserAnswersCacheConnector, navigator, messagesApi, emailView
  )

  private val userAnswersId = "test-user-answers-id"
  implicit val hc: HeaderCarrier = HeaderCarrier()
  implicit val request: DataRequest[AnyContent] = DataRequest(FakeRequest(),
    UserAnswers(Json.obj("id" -> userAnswersId)), PsaId("A2110001"), Data.migrationLock)
  private val emailId: TypedIdentifier[String] = new TypedIdentifier[String] {}

  override def beforeEach(): Unit = {
    reset(emailView)
    reset(mockUserAnswersCacheConnector)
  }

  "CommonEmailAddressService" must {

    "get the view correctly on get" in {
      when(emailView.apply(eqTo(form), eqTo("schemeName"), eqTo("entityName"), any(), any(), any())(any(), any())).thenReturn(Html("email content"))

      val result = service.get("entityName", Messages("entityType"), emailId, form, "schemeName", submitCall = onwardCall)(request)

      status(result) mustBe OK

      verify(emailView, times(1)).apply(eqTo(form), eqTo("schemeName"), eqTo("entityName"), any(), any(), any())(any(), any())
    }

    "return a BadRequest and errors when invalid data is submitted on post" in {
      val invalidRequest: DataRequest[AnyContent] = DataRequest(FakeRequest().withFormUrlEncodedBody("value" -> "invalid"),
        UserAnswers(Json.obj("id" -> userAnswersId)), PsaId("A2110001"), Data.migrationLock)

      when(emailView.apply(any(), eqTo("schemeName"), eqTo("entityName"), any(), any(), any())(any(), any())).thenReturn(Html("email content"))

      val result = service.post("entityName", Messages("entityType"), emailId, form.withError("value", "error.required"), "schemeName",
        submitCall = onwardCall)(invalidRequest, global)

      status(result) mustBe BAD_REQUEST
      verify(emailView, times(1)).apply(any(), eqTo("schemeName"), eqTo("entityName"), any(), any(), any())(any(), any())
    }

    "save the data and redirect correctly on post" in {
      val validRequest: DataRequest[AnyContent] = DataRequest(FakeRequest().withFormUrlEncodedBody("value" -> "test@example.com"),
        UserAnswers(Json.obj("id" -> userAnswersId)), PsaId("A2110001"), Data.migrationLock)

      when(mockUserAnswersCacheConnector.save(any(), any())(any(), any())).thenReturn(Future.successful(Json.obj()))

      val result = service.post("entityName", Messages("entityType"), emailId, form, "schemeName", submitCall = onwardCall)(validRequest, global)

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(onwardCall.url)
    }
  }
}