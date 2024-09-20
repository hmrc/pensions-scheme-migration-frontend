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
import models.{Address, AddressConfiguration, NormalMode, TolerantAddress}
import models.requests.DataRequest
import org.mockito.ArgumentMatchers.any
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.json.Json
import play.api.mvc.AnyContent
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.twirl.api.Html
import renderer.Renderer
import uk.gov.hmrc.http.HeaderCarrier
import utils.{Data, FakeNavigator, UserAnswers}
import identifiers.TypedIdentifier
import play.api.data.Form
import play.api.data.Forms.{mapping, nonEmptyText, optional, text}
import uk.gov.hmrc.domain.PsaId
import services.CommonServiceSpecBase

import scala.concurrent.Future

class CommonManualAddressServiceSpec extends CommonServiceSpecBase with MockitoSugar with ScalaFutures with BeforeAndAfterEach with Retrievals {

  private val navigator = new FakeNavigator(desiredRoute = onwardCall)
  val renderer = new Renderer(mockAppConfig, mockRenderer)
  private val form = Form(mapping(
    "line1" -> nonEmptyText,
    "line2" -> text,
    "line3" -> optional(text),
    "line4" -> optional(text),
    "postcode" -> optional(text),
    "country" -> nonEmptyText
  )(Address.apply)(Address.unapply))
  private val service = new CommonManualAddressService(renderer, mockUserAnswersCacheConnector, navigator, messagesApi, appConfig)
  private val userAnswersId = "test-user-answers-id"
  private val addressPage = new TypedIdentifier[Address] {
    override def toString: String = "addressPage"
  }
  private val selectedAddress = new TypedIdentifier[TolerantAddress] {
    override def toString: String = "selectedAddress"
  }
  implicit val hc: HeaderCarrier = HeaderCarrier()
  implicit val request: DataRequest[AnyContent] = DataRequest(FakeRequest(), UserAnswers(Json.obj("id" -> userAnswersId)), PsaId("A2110001"), Data.migrationLock)

  override def beforeEach(): Unit = {
    reset(mockRenderer, mockUserAnswersCacheConnector)
  }

  "CommonManualAddressService" must {

    "render the view correctly on get" in {
      when(mockRenderer.render(any(), any())(any())).thenReturn(Future.successful(Html("")))

      val result = service.get(Some("schemeName"), "entityName", addressPage, selectedAddress, AddressConfiguration.PostcodeFirst, form)(request, global)

      status(result) mustBe OK
      verify(mockRenderer, times(1)).render(any(), any())(any())
    }

    "return a BadRequest and errors when invalid data is submitted on post" in {
      val invalidRequest: DataRequest[AnyContent] = DataRequest(FakeRequest().withFormUrlEncodedBody("line1" -> ""), UserAnswers(Json.obj("id" -> userAnswersId)), PsaId("A2110001"), Data.migrationLock)
      val formWithErrors = form.withError("line1", "error.required")

      when(mockRenderer.render(any(), any())(any())).thenReturn(Future.successful(Html("")))

      val result = service.post(Some("schemeName"), "entityName", addressPage, AddressConfiguration.PostcodeFirst, Some(NormalMode), form)(invalidRequest, global)

      status(result) mustBe BAD_REQUEST
      verify(mockRenderer, times(1)).render(any(), any())(any())
    }

    "save the data and redirect correctly on post" in {
      val validRequest: DataRequest[AnyContent] = DataRequest(
        FakeRequest().withFormUrlEncodedBody("line1" -> "line1", "line2" -> "line2", "country" -> "GB"),
        UserAnswers(Json.obj("id" -> userAnswersId)),
        PsaId("A2110001"),
        Data.migrationLock
      )

      when(mockRenderer.render(any(), any())(any())).thenReturn(Future.successful(Html("")))
      when(mockUserAnswersCacheConnector.save(any(), any())(any(), any())).thenReturn(Future.successful(Json.obj()))

      val result = service.post(Some("schemeName"), "entityName", addressPage, AddressConfiguration.PostcodeFirst, Some(NormalMode), form)(validRequest, global)

      status(result) mustBe SEE_OTHER
      verify(mockUserAnswersCacheConnector, times(1)).save(any(), any())(any(), any())
    }
  }
}
