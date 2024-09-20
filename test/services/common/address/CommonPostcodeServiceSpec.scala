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

import connectors.AddressLookupConnector
import models.{NormalMode, TolerantAddress}
import models.requests.DataRequest
import org.mockito.ArgumentMatchers.any
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import play.api.data.Form
import play.api.data.Forms.nonEmptyText
import play.api.libs.json.Json
import play.api.mvc.AnyContent
import play.api.test.FakeRequest
import play.api.test.Helpers._
import renderer.Renderer
import uk.gov.hmrc.http.HeaderCarrier
import utils.{Data, FakeNavigator, UserAnswers}
import identifiers.TypedIdentifier
import play.twirl.api.Html
import uk.gov.hmrc.domain.PsaId
import services.CommonServiceSpecBase

import scala.concurrent.Future

class CommonPostcodeServiceSpec extends CommonServiceSpecBase with MockitoSugar with ScalaFutures with BeforeAndAfterEach {

  private val navigator = new FakeNavigator(desiredRoute = onwardCall)
  private val renderer = new Renderer(mockAppConfig, mockRenderer)
  private val mockAddressLookupConnector: AddressLookupConnector = org.scalatestplus.mockito.MockitoSugar.mock[AddressLookupConnector]
  private val service = new CommonPostcodeService(renderer, navigator, mockAddressLookupConnector, mockUserAnswersCacheConnector)
  private val form = Form("postcode" -> nonEmptyText)
  private val userAnswersId = "test-user-answers-id"
  private val postcodeId = new TypedIdentifier[Seq[TolerantAddress]] {
    override def toString: String = "postcodeId"
  }
  implicit val hc: HeaderCarrier = HeaderCarrier()
  implicit val request: DataRequest[AnyContent] = DataRequest(FakeRequest(), UserAnswers(Json.obj("id" -> userAnswersId)), PsaId("A2110001"), Data.migrationLock)

  override def beforeEach(): Unit = {
    reset(mockAddressLookupConnector, mockUserAnswersCacheConnector)
  }

  "CommonPostcodeService" must {

    "render the view correctly on get" in {
      when(mockRenderer.render(any(), any())(any())).thenReturn(Future.successful(Html("")))

      val result = service.get(form => CommonPostcodeTemplateData(form, "entityType", "entityName", "enterManuallyUrl", "schemeName", "h1MessageKey"), form)(request, global)

      status(result) mustBe OK
      verify(mockRenderer, times(1)).render(any(), any())(any())
    }

    "return a BadRequest and errors when invalid data is submitted on post" in {
      val invalidRequest: DataRequest[AnyContent] = DataRequest(FakeRequest().withFormUrlEncodedBody("postcode" -> ""), UserAnswers(Json.obj("id" -> userAnswersId)), PsaId("A2110001"), Data.migrationLock)

      when(mockRenderer.render(any(), any())(any())).thenReturn(Future.successful(Html("")))

      val result = service.post(form => CommonPostcodeTemplateData(form, "entityType", "entityName", "enterManuallyUrl", "schemeName"), postcodeId, "error.required", Some(NormalMode), form)(invalidRequest, global, hc)

      status(result) mustBe BAD_REQUEST
      verify(mockRenderer, times(2)).render(any(), any())(any())
    }

    "save the data and redirect correctly on post" in {
      val validRequest: DataRequest[AnyContent] = DataRequest(FakeRequest().withFormUrlEncodedBody("postcode" -> "AA1 1AA"), UserAnswers(Json.obj("id" -> userAnswersId)), PsaId("A2110001"), Data.migrationLock)
      val addresses = Seq(TolerantAddress(Some("line1"), None, None, None, Some("AA1 1AA"), Some("GB")))

      when(mockAddressLookupConnector.addressLookupByPostCode(any())(any(), any())).thenReturn(Future.successful(addresses))
      when(mockUserAnswersCacheConnector.save(any(), any())(any(), any())).thenReturn(Future.successful(Json.obj()))

      val result = service.post(form => CommonPostcodeTemplateData(form, "entityType", "entityName", "enterManuallyUrl", "schemeName"), postcodeId, "error.required", Some(NormalMode), form)(validRequest, global, hc)

      status(result) mustBe SEE_OTHER
      verify(mockUserAnswersCacheConnector, times(1)).save(any(), any())(any(), any())
    }
  }
}
