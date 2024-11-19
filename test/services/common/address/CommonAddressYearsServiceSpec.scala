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

import controllers.ControllerSpecBase
import identifiers.TypedIdentifier
import identifiers.establishers.individual.address.AddressYearsId
import models.NormalMode
import models.requests.DataRequest
import org.mockito.ArgumentMatchers.any
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import play.api.data.Form
import play.api.data.Forms._
import play.api.libs.json.Json
import play.api.mvc.AnyContent
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.CommonServiceSpecBase
import uk.gov.hmrc.domain.PsaId
import uk.gov.hmrc.http.HeaderCarrier
import utils.{Data, FakeNavigator, UserAnswers}
import views.html.address.AddressYearsView

import scala.concurrent.Future

class CommonAddressYearsServiceSpec extends ControllerSpecBase with CommonServiceSpecBase
  with MockitoSugar with ScalaFutures with BeforeAndAfterEach {

  private val navigator = new FakeNavigator(desiredRoute = onwardCall)
  private val form = Form("value" -> boolean)
  private val service = new CommonAddressYearsService(
    mockUserAnswersCacheConnector,
    navigator,
    messagesApi,
    addressYearsView = app.injector.instanceOf[AddressYearsView]
  )

  private val userAnswersId = "test-user-answers-id"
  implicit val hc: HeaderCarrier = HeaderCarrier()
  implicit val request: DataRequest[AnyContent] = DataRequest(FakeRequest(),
    UserAnswers(Json.obj("id" -> userAnswersId)), PsaId("A2110001"), Data.migrationLock)
  private val addressYearsId: TypedIdentifier[Boolean] = AddressYearsId(0)

  override def beforeEach(): Unit = {
    reset(mockUserAnswersCacheConnector)
  }

  "CommonAddressYearsService" must {

    "render the view correctly on get" in {
      val result = service.get(Some("schemeName"), "entityName", "entityType", form, addressYearsId, submitUrl = onwardCall)(request)
      status(result) mustBe OK
    }

    "return a BadRequest and errors when invalid data is submitted on post" in {
      val invalidRequest: DataRequest[AnyContent] = DataRequest(FakeRequest()
        .withFormUrlEncodedBody("value" -> "invalid"), UserAnswers(Json.obj("id" -> userAnswersId)), PsaId("A2110001"), Data.migrationLock)

      val result = service.post(Some("schemeName"), "entityName", "entityType", form, addressYearsId, Some(NormalMode),
        submitUrl = onwardCall)(invalidRequest, global)

      status(result) mustBe BAD_REQUEST
    }

    "save the data and redirect correctly on post" in {
      val validRequest: DataRequest[AnyContent] = DataRequest(FakeRequest()
        .withFormUrlEncodedBody("value" -> "true"), UserAnswers(Json.obj("id" -> userAnswersId)), PsaId("A2110001"), Data.migrationLock)

      when(mockUserAnswersCacheConnector.save(any(), any())(any(), any())).thenReturn(Future.successful(Json.obj()))

      val result = service.post(Some("schemeName"), "entityName", "entityType", form, addressYearsId, Some(NormalMode),
        submitUrl = onwardCall)(validRequest, global)

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(onwardCall.url)
    }
  }
}
