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

package controllers.establishers.partnership.partner.details

import controllers.ControllerSpecBase
import controllers.actions.{DataRequiredActionImpl, DataRetrievalAction, FakeAuthAction, FakeDataRetrievalAction}
import forms.UTRFormProvider
import identifiers.establishers.partnership.partner.PartnerNameId
import identifiers.establishers.partnership.partner.details.PartnerEnterUTRId
import matchers.JsonMatchers
import models.{NormalMode, PersonName, ReferenceValue}
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.scalatest.{BeforeAndAfterEach, TryValues}
import play.api.data.Form
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.{AnyContentAsFormUrlEncoded, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.twirl.api.Html
import renderer.Renderer
import services.common.details.CommonEnterReferenceValueService
import uk.gov.hmrc.nunjucks.NunjucksSupport
import utils.Data.{partnershipDetails, schemeName, ua}
import utils.{FakeNavigator, UserAnswers}
import views.html.{EnterReferenceValueView, EnterReferenceValueWithHintView}

import scala.concurrent.Future

class PartnerEnterUTRControllerSpec
  extends ControllerSpecBase
    with NunjucksSupport
    with JsonMatchers
    with TryValues
    with BeforeAndAfterEach {

  private val personName: PersonName =
    PersonName("Jane", "Doe")
  private val formProvider: UTRFormProvider =
    new UTRFormProvider()
  private val form: Form[ReferenceValue] =
    formProvider()

  private val userAnswers: UserAnswers =
    ua.set(PartnerNameId(0,0), personName).success.value

  private val formData: ReferenceValue =
    ReferenceValue(value = "1234567890")

  override def beforeEach(): Unit = {
    reset(
      mockUserAnswersCacheConnector
    )
  }

  private def controller(
                          dataRetrievalAction: DataRetrievalAction
                        ): PartnerEnterUTRController =
    new PartnerEnterUTRController(
      messagesApi               = messagesApi,
      authenticate              = new FakeAuthAction(),
      getData                   = dataRetrievalAction,
      requireData               = new DataRequiredActionImpl,
      formProvider              = formProvider,
      common = new CommonEnterReferenceValueService(
        controllerComponents = controllerComponents,
        userAnswersCacheConnector = mockUserAnswersCacheConnector,
        navigator = new FakeNavigator(desiredRoute = onwardCall),
        enterReferenceValueView = app.injector.instanceOf[EnterReferenceValueView],
        enterReferenceValueWithHintView = app.injector.instanceOf[EnterReferenceValueWithHintView],
        messagesApi = messagesApi
      )
    )

  "PartnerEnterUTRController" must {
    "return OK and the correct view for a GET" in {
      val getData = new FakeDataRetrievalAction(Some(userAnswers))

      val result: Future[Result] =
        controller(getData)
          .onPageLoad(0,0, NormalMode)(fakeDataRequest(userAnswers))

      status(result) mustBe OK

      val view = app.injector.instanceOf[EnterReferenceValueWithHintView].apply(
        form = form,
        pageTitle = messages("messages__enterUTR", messages("messages__partner")),
        pageHeading = messages("messages__enterUTR", personName.fullName),
        schemeName = schemeName,
        legendClass = "govuk-visually-hidden",
        paragraphs = Seq(messages("messages__UTR__p1"), messages("messages__UTR__p2")),
        submitCall= routes.PartnerEnterUTRController.onSubmit(0, 0, NormalMode)
      )(fakeRequest, messages)

      compareResultAndView(result, view)
    }

    "populate the view correctly on a GET when the question has previously been answered" in {
      val ua =
        userAnswers
          .set(PartnerEnterUTRId(0,0), formData).success.value

      val getData = new FakeDataRetrievalAction(Some(ua))

      val result: Future[Result] =
        controller(getData)
          .onPageLoad(0,0, NormalMode)(fakeDataRequest(userAnswers))

      status(result) mustBe OK

      contentAsString(result) must include(messages("messages__enterUTR", "the partner"))
      contentAsString(result) must include(formData.value)

    }

    "redirect to the next page when valid data is submitted" in {
      when(mockUserAnswersCacheConnector.save(any(), any())(any(), any()))
        .thenReturn(Future.successful(Json.obj()))

      val request: FakeRequest[AnyContentAsFormUrlEncoded] =
        fakeRequest
          .withFormUrlEncodedBody("value" -> "1234567890")

      val getData = new FakeDataRetrievalAction(Some(userAnswers))

      val result: Future[Result] =
        controller(getData)
          .onSubmit(0,0, NormalMode)(request)

      status(result) mustBe SEE_OTHER

      redirectLocation(result) mustBe Some(onwardCall.url)

      verify(mockUserAnswersCacheConnector, times(1))
        .save(any(), any())(any(), any())
    }

    "return a Bad Request and errors when invalid data is submitted" in {
      val request: FakeRequest[AnyContentAsFormUrlEncoded] =
        fakeRequest
          .withFormUrlEncodedBody("value" -> "invalid value")

      val getData = new FakeDataRetrievalAction(Some(userAnswers))

      val result: Future[Result] =
        controller(getData)
          .onSubmit(0,0, NormalMode)(request)

      status(result) mustBe BAD_REQUEST

      contentAsString(result) must include(messages("messages__enterUTR", "the partner"))
      contentAsString(result) must include(messages("messages__utr__error_invalid", partnershipDetails.partnershipName))

      verify(mockUserAnswersCacheConnector, times(0))
        .save(any(), any())(any(), any())
    }
  }
}
