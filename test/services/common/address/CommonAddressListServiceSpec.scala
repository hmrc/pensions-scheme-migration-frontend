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

import controllers.Retrievals
import identifiers.TypedIdentifier
import identifiers.establishers.individual.address.AddressId
import identifiers.trustees.individual.address.AddressListId
import models.establishers.AddressPages
import models.requests.DataRequest
import models.{NormalMode, TolerantAddress}
import org.mockito.ArgumentMatchers.any
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import play.api.data.Form
import play.api.data.Forms._
import play.api.libs.json.Json
import play.api.mvc.{AnyContent, Call}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.twirl.api.Html
import renderer.Renderer
import services.CommonServiceSpecBase
import uk.gov.hmrc.domain.PsaId
import uk.gov.hmrc.http.HeaderCarrier
import utils.{Data, FakeNavigator, UserAnswers}

import scala.concurrent.Future

class CommonAddressListServiceSpec extends CommonServiceSpecBase with MockitoSugar with ScalaFutures with BeforeAndAfterEach with Retrievals {

  private val navigator = new FakeNavigator(desiredRoute = onwardCall)
  val renderer = new Renderer(mockAppConfig, mockRenderer)
  private val form = Form("value" -> number)
  private val service = new CommonAddressListService(renderer, mockUserAnswersCacheConnector, navigator, messagesApi, mockAppConfig)

  private val userAnswersId = "test-user-answers-id"
  implicit val hc: HeaderCarrier = HeaderCarrier()
  implicit val request: DataRequest[AnyContent] = DataRequest(FakeRequest(), UserAnswers(Json.obj("id" -> userAnswersId)), PsaId("A2110001"), Data.migrationLock)
  private val postcodeId: TypedIdentifier[Seq[TolerantAddress]] = new TypedIdentifier[Seq[TolerantAddress]] {}
  private val addressPages = new AddressPages(postcodeId, AddressListId(0), AddressId(0))

  override def beforeEach(): Unit = {
    reset(mockRenderer, mockUserAnswersCacheConnector)
  }

  "CommonAddressListService" must {

    "render the view correctly on get" in {
      val templateData = CommonAddressListTemplateData(form, Seq(Json.obj("value" -> 0, "text" -> "Test Address")), "entityType", "entityName", "enterManuallyUrl", "schemeName")

      when(mockRenderer.render(any(), any())(any())).thenReturn(Future.successful(Html("")))

      val result = service.get(templateData)(request, global)

      status(result) mustBe OK
      verify(mockRenderer, times(1)).render(any(), any())(any())
    }

    "return a BadRequest and errors when invalid data is submitted on post" in {
      val invalidRequest: DataRequest[AnyContent] = DataRequest(FakeRequest().withFormUrlEncodedBody("value" -> "invalid"), UserAnswers(Json.obj("id" -> userAnswersId)), PsaId("A2110001"), Data.migrationLock)
      val formWithErrors = form.withError("value", "error.required")
      val templateData = CommonAddressListTemplateData(formWithErrors, Seq(Json.obj("value" -> 0, "text" -> "Test Address")), "entityType", "entityName", "enterManuallyUrl", "schemeName")

      when(mockRenderer.render(any(), any())(any())).thenReturn(Future.successful(Html("")))

      val result = service.post(_ => templateData, addressPages, Some(NormalMode), Call("GET", "manualUrl"), form)(invalidRequest, global, hc)

      status(result) mustBe BAD_REQUEST
      verify(mockRenderer, times(1)).render(any(), any())(any())
    }

    "save the data and redirect correctly on post" in {
      val validRequest: DataRequest[AnyContent] = DataRequest(FakeRequest().withFormUrlEncodedBody("value" -> "0"), UserAnswers(Json.obj("id" -> userAnswersId)), PsaId("A2110001"), Data.migrationLock)
      val templateData = CommonAddressListTemplateData(form, Seq(Json.obj("value" -> 0, "text" -> "Test Address")), "entityType", "entityName", "enterManuallyUrl", "schemeName")

      when(mockUserAnswersCacheConnector.save(any(), any())(any(), any())).thenReturn(Future.successful(Json.obj()))
      when(mockRenderer.render(any(), any())(any())).thenReturn(Future.successful(Html("")))

      val result = service.post(_ => templateData, addressPages, Some(NormalMode), Call("GET", "manualUrl"), form)(validRequest, global, hc)

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(controllers.routes.SessionExpiredController.onPageLoad().url)
    }
  }
}