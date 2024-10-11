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
import controllers.establishers.company.director.address.routes
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
import services.CommonServiceSpecBase
import uk.gov.hmrc.domain.PsaId
import uk.gov.hmrc.http.HeaderCarrier
import utils.{Data, FakeNavigator, UserAnswers}
import views.html.address.AddressListView

import scala.concurrent.Future

class CommonAddressListServiceSpec extends CommonServiceSpecBase
  with MockitoSugar with ScalaFutures with BeforeAndAfterEach with Retrievals {

  private val navigator = new FakeNavigator(desiredRoute = onwardCall)
  private val form = Form("value" -> number)
  private val service = new CommonAddressListService(
    mockUserAnswersCacheConnector,
    navigator,
    messagesApi,
    addressListView = app.injector.instanceOf[AddressListView]
   )

  private val userAnswersId = "test-user-answers-id"
  implicit val hc: HeaderCarrier = HeaderCarrier()
  implicit val request: DataRequest[AnyContent] = DataRequest(FakeRequest(),
    UserAnswers(Json.obj("id" -> userAnswersId)), PsaId("A2110001"), Data.migrationLock)
  private val postcodeId: TypedIdentifier[Seq[TolerantAddress]] = new TypedIdentifier[Seq[TolerantAddress]] {}
  private val addressPages = new AddressPages(postcodeId, AddressListId(0), AddressId(0))

  override def beforeEach(): Unit = {
    reset(mockUserAnswersCacheConnector)
  }

  "CommonAddressListService" must {

    "render the view correctly on get" in {
      val templateData = CommonAddressListTemplateData(
        form,
        Seq(TolerantAddress(Some("line1"), Some("line2"), None, None, Some("Test Address"), None)),
        "entityType",
        "entityName",
        "enterManuallyUrl",
        "schemeName",
        "addressList.title"
      )

      val result = service.get(templateData, form,
        submitUrl = routes.SelectAddressController.onSubmit(establisherIndex = 0, directorIndex = 0, mode = NormalMode)
      )(request, global)

      status(result) mustBe OK
    }

    "return a BadRequest and errors when invalid data is submitted on post" in {
      val invalidRequest: DataRequest[AnyContent] = DataRequest(FakeRequest()
        .withFormUrlEncodedBody("value" -> "invalid"), UserAnswers(Json.obj("id" -> userAnswersId)), PsaId("A2110001"), Data.migrationLock)
      val formWithErrors = form.withError("value", "error.required")
      val templateData = CommonAddressListTemplateData(
        formWithErrors,
        Seq(TolerantAddress(Some("line1"), Some("line2"), None, None, Some("Test Address"), None)),
        "entityType",
        "entityName",
        "enterManuallyUrl",
        "schemeName",
        "addressList.title"
      )

      val result = service.post(_ => templateData, addressPages, Some(NormalMode), Call("GET", "manualUrl"), form,
        submitUrl = routes.SelectAddressController.onSubmit(establisherIndex = 0, directorIndex = 0, mode = NormalMode)
      )(invalidRequest, global, hc)

      status(result) mustBe BAD_REQUEST
    }

    "save the data and redirect correctly on post" in {
      val validRequest: DataRequest[AnyContent] = DataRequest(FakeRequest()
        .withFormUrlEncodedBody("value" -> "0"), UserAnswers(Json.obj("id" -> userAnswersId)), PsaId("A2110001"), Data.migrationLock)
      val templateData = CommonAddressListTemplateData(
        form,
        Seq(TolerantAddress(Some("line1"), Some("line2"), None, None, Some("Test Address"), None)),
        "entityType",
        "entityName",
        "enterManuallyUrl",
        "schemeName",
        "addressList.title"
      )

      when(mockUserAnswersCacheConnector.save(any(), any())(any(), any())).thenReturn(Future.successful(Json.obj()))

      val result = service.post(_ => templateData, addressPages, Some(NormalMode), Call("GET", "manualUrl"), form,
        submitUrl = routes.SelectAddressController.onSubmit(establisherIndex = 0, directorIndex = 0, mode = NormalMode)
      )(validRequest, global, hc)

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(controllers.routes.SessionExpiredController.onPageLoad().absoluteURL()(request))
    }
  }
}