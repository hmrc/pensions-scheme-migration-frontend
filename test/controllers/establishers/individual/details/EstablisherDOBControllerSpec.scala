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

package controllers.establishers.individual.details

import controllers.ControllerSpecBase
import controllers.actions.{DataRequiredActionImpl, DataRetrievalAction, FakeAuthAction, FakeDataRetrievalAction}
import forms.DOBFormProvider
import identifiers.establishers.individual.EstablisherNameId
import identifiers.establishers.partnership.partner.details.PartnerDOBId
import matchers.JsonMatchers
import models.{Index, NormalMode, PersonName}
import org.mockito.ArgumentMatchers.any
import org.scalatest.{BeforeAndAfterEach, TryValues}
import play.api.data.Form
import play.api.libs.json.Json
import play.api.mvc.{AnyContentAsFormUrlEncoded, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.common.details.CommonDateOfBirthService
import utils.Data.ua
import utils.{FakeNavigator, UserAnswers}

import java.time.LocalDate
import scala.concurrent.Future

class EstablisherDOBControllerSpec
  extends ControllerSpecBase
    with JsonMatchers
    with TryValues
    with BeforeAndAfterEach {

  private val formProvider: DOBFormProvider =
    new DOBFormProvider()
  private val form: Form[LocalDate] =
    formProvider()
  private val personName: PersonName =
    PersonName("Jane", "Doe")
  private val userAnswers: UserAnswers =
    ua.set(EstablisherNameId(0), personName).success.value

  val index = Index(0)
  val mode = NormalMode
  private def onPageLoadUrl: String = routes.EstablisherDOBController.onPageLoad(index, mode).url
  private val formData: LocalDate =
    LocalDate.parse("2000-01-01")

  private val day: Int = formData.getDayOfMonth
  private val month: Int = formData.getMonthValue
  private val year: Int = formData.getYear
  val view = app.injector.instanceOf[views.html.DobView]

  private def controller(
                          dataRetrievalAction: DataRetrievalAction
                        ): EstablisherDOBController =
    new EstablisherDOBController(
      messagesApi               = messagesApi,
      authenticate              = new FakeAuthAction(),
      getData                   = dataRetrievalAction,
      requireData               = new DataRequiredActionImpl,
      formProvider              = formProvider,
      common = new CommonDateOfBirthService(
        controllerComponents = controllerComponents,
        dobView = view,
        userAnswersCacheConnector = mockUserAnswersCacheConnector,
        navigator = new FakeNavigator(desiredRoute = onwardCall),
        messagesApi = messagesApi
      )
    )

  override def beforeEach(): Unit = {
    reset(
      mockUserAnswersCacheConnector
    )
  }

  "EstablisherDOBController" must {
    "return OK and the correct view for a GET" in {
      val request = FakeRequest(GET, onPageLoadUrl)

      val getData = new FakeDataRetrievalAction(Some(userAnswers))

      val result: Future[Result] =
        controller(getData)
          .onPageLoad(0, NormalMode)(fakeDataRequest(userAnswers))
      status(result) mustEqual OK

      val expectedView = view(
        form,
        "John Doe",
        "Test scheme name",
        "the partner",
        routes.EstablisherDOBController.onSubmit(index, mode)
      )(request, messages)

      contentAsString(result)
        .replaceAll("&amp;referrerUrl=%2F\\[.*?\\]", "&amp;referrerUrl=%2F[]")
        .removeAllNonces() contains expectedView.toString()
    }

    "populate the view correctly on a GET when the question has previously been answered" in {
      val request = FakeRequest(GET, onPageLoadUrl)

      val ua =
        userAnswers
          .set(PartnerDOBId(0,0), formData).success.value

      val getData = new FakeDataRetrievalAction(Some(ua))

      val result: Future[Result] =
        controller(getData)
          .onPageLoad(0, NormalMode)(fakeDataRequest(userAnswers))

      status(result) mustBe OK

      val expectedView = view(
        form,
        "John Doe",
        "Test scheme name",
        "the partner",
        routes.EstablisherDOBController.onSubmit(index, mode)
      )(request, messages)

      contentAsString(result)
        .replaceAll("&amp;referrerUrl=%2F\\[.*?\\]", "&amp;referrerUrl=%2F[]")
        .removeAllNonces() contains expectedView.toString()
    }

    "redirect to the next page when valid data is submitted" in {
      when(mockUserAnswersCacheConnector.save(any(), any())(any(), any()))
        .thenReturn(Future.successful(Json.obj()))

      val request: FakeRequest[AnyContentAsFormUrlEncoded] =
        fakeRequest
          .withFormUrlEncodedBody(
            ("date.day", day.toString),
            ("date.month", month.toString),
            ("date.year", year.toString)
          )

      val getData = new FakeDataRetrievalAction(Some(userAnswers))

      val result: Future[Result] =
        controller(getData)
          .onSubmit(0, NormalMode)(request)

      status(result) mustBe SEE_OTHER

      redirectLocation(result) mustBe Some(onwardCall.url)

      verify(mockUserAnswersCacheConnector, times(1))
        .save(any(), any())(any(), any())
    }

    "return a Bad Request and errors when invalid data is submitted" in {
      val request: FakeRequest[AnyContentAsFormUrlEncoded] =
        fakeRequest
          .withFormUrlEncodedBody("date" -> "invalid value")

      val getData = new FakeDataRetrievalAction(Some(userAnswers))

      val result: Future[Result] =
        controller(getData)
          .onSubmit(0, NormalMode)(request)

      status(result) mustBe BAD_REQUEST


      verify(mockUserAnswersCacheConnector, times(0))
        .save(any(), any())(any(), any())
    }
  }
}
