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

package controllers.trustees.partnership.details

import controllers.ControllerSpecBase
import controllers.actions.{DataRequiredActionImpl, DataRetrievalAction, FakeAuthAction, FakeDataRetrievalAction}
import forms.HasReferenceNumberFormProvider
import identifiers.trustees.partnership.PartnershipDetailsId
import identifiers.trustees.partnership.details.HaveVATId
import matchers.JsonMatchers
import models.{Index, NormalMode}
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.scalatest.{BeforeAndAfterEach, TryValues}
import play.api.data.Form
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.{AnyContentAsFormUrlEncoded, Call, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers.{status, _}
import play.twirl.api.Html
import renderer.Renderer
import uk.gov.hmrc.viewmodels.{NunjucksSupport, Radios}
import utils.Data.{partnershipDetails, schemeName, ua}
import utils.{FakeNavigator, UserAnswers}
import viewmodels.Message

import scala.concurrent.Future
class HaveVATControllerSpec extends ControllerSpecBase with NunjucksSupport with JsonMatchers with TryValues with BeforeAndAfterEach {

  private val index: Index = Index(0)
  private val userAnswers: UserAnswers = ua.set(PartnershipDetailsId(index), partnershipDetails).success.value
  private val templateCaptor: ArgumentCaptor[String] = ArgumentCaptor.forClass(classOf[String])
  private val jsonCaptor: ArgumentCaptor[JsObject] = ArgumentCaptor.forClass(classOf[JsObject])
  private val formProvider: HasReferenceNumberFormProvider = new HasReferenceNumberFormProvider()
  private val form: Form[Boolean] = formProvider(Message("messages__genericHaveVat__error__required", partnershipDetails.partnershipName))
  private val onwardRoute: Call = controllers.routes.IndexController.onPageLoad
  private val templateToBeRendered: String = "hasReferenceValue.njk"

  private val commonJson: JsObject =
    Json.obj(
      "pageTitle"     -> messages("messages__haveVAT", messages("messages__partnership")),
      "pageHeading"     -> messages("messages__haveVAT", partnershipDetails.partnershipName),
      "schemeName"    -> schemeName,
      "legendClass"   -> "govuk-visually-hidden",
      "isPageHeading" -> true
    )

  private def controller(dataRetrievalAction: DataRetrievalAction): HaveVATController =
    new HaveVATController(messagesApi, new FakeNavigator(desiredRoute = onwardRoute), new FakeAuthAction(), dataRetrievalAction,
      new DataRequiredActionImpl, formProvider, controllerComponents, mockUserAnswersCacheConnector, new Renderer(mockAppConfig, mockRenderer))

  override def beforeEach(): Unit = reset(mockRenderer, mockUserAnswersCacheConnector)

  "HaveVATController" must {
    "return OK and the correct view for a GET" in {
      when(mockRenderer.render(any(), any())(any())).thenReturn(Future.successful(Html("")))

      val getData = new FakeDataRetrievalAction(Some(userAnswers))

      val result: Future[Result] = controller(getData).onPageLoad(0, NormalMode)(fakeDataRequest(userAnswers))

      status(result) mustBe OK

      verify(mockRenderer, times(1)).render(templateCaptor.capture(), jsonCaptor.capture())(any())

      templateCaptor.getValue mustEqual templateToBeRendered
      val json: JsObject = Json.obj("radios" -> Radios.yesNo(form("value")))
      jsonCaptor.getValue must containJson(commonJson ++ json)

    }

    "populate the view correctly on a GET when the question has previously been answered" in {
      when(mockRenderer.render(any(), any())(any())).thenReturn(Future.successful(Html("")))

      val ua = userAnswers.set(HaveVATId(0), true).success.value
      val getData = new FakeDataRetrievalAction(Some(ua))

      val result: Future[Result] = controller(getData).onPageLoad(0, NormalMode)(fakeDataRequest(userAnswers))

      status(result) mustBe OK

      verify(mockRenderer, times(1)).render(templateCaptor.capture(), jsonCaptor.capture())(any())

      templateCaptor.getValue mustEqual templateToBeRendered
      val json: JsObject = Json.obj("radios" -> Radios.yesNo(form.fill(true).apply("value")))
      jsonCaptor.getValue must containJson(commonJson ++ json)
    }

    "redirect to the next page when valid data is submitted" in {
      when(mockUserAnswersCacheConnector.save(any(), any())(any(), any())).thenReturn(Future.successful(Json.obj()))
      val request: FakeRequest[AnyContentAsFormUrlEncoded] = fakeRequest.withFormUrlEncodedBody(("value", "true"))
      val getData = new FakeDataRetrievalAction(Some(userAnswers))

      val result: Future[Result] = controller(getData).onSubmit(0, NormalMode)(request)

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(onwardRoute.url)
      verify(mockUserAnswersCacheConnector, times(1)).save(any(), any())(any(), any())
    }

    "return a Bad Request and errors when invalid data is submitted" in {
      when(mockRenderer.render(any(), any())(any())).thenReturn(Future.successful(Html("")))

      val request = fakeRequest.withFormUrlEncodedBody(("value", "invalid value"))
      val getData = new FakeDataRetrievalAction(Some(userAnswers))

      val result: Future[Result] = controller(getData).onSubmit(0, NormalMode)(request)
      val boundForm = form.bind(Map("value" -> "invalid value"))

      status(result) mustBe BAD_REQUEST

      verify(mockRenderer, times(1)).render(templateCaptor.capture(), jsonCaptor.capture())(any())
      templateCaptor.getValue mustEqual templateToBeRendered
      val json: JsObject = Json.obj("radios" -> Radios.yesNo(boundForm.apply("value")))
      jsonCaptor.getValue must containJson(commonJson ++ json)
      verify(mockUserAnswersCacheConnector, times(0)).save(any(), any())(any(), any())
    }
  }
}
