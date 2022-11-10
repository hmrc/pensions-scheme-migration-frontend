/*
 * Copyright 2022 HM Revenue & Customs
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

package controllers.trustees.individual.details

import controllers.ControllerSpecBase
import controllers.actions.{DataRequiredActionImpl, DataRetrievalAction, FakeAuthAction, FakeDataRetrievalAction}
import forms.DOBFormProvider
import identifiers.trustees.individual.TrusteeNameId
import identifiers.trustees.individual.details.TrusteeDOBId
import matchers.JsonMatchers
import models.{NormalMode, PersonName}
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, times, verify, when}
import org.scalatest.{BeforeAndAfterEach, TryValues}
import play.api.data.Form
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.{AnyContentAsFormUrlEncoded, Call, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.twirl.api.Html
import renderer.Renderer
import uk.gov.hmrc.viewmodels.{DateInput, NunjucksSupport}
import utils.Data.ua
import utils.{FakeNavigator, UserAnswers}

import java.time.LocalDate
import scala.concurrent.Future
class TrusteeDOBControllerSpec
  extends ControllerSpecBase
    with NunjucksSupport
    with JsonMatchers
    with TryValues
    with BeforeAndAfterEach {

  private val formProvider: DOBFormProvider = new DOBFormProvider()

  private val form: Form[LocalDate] = formProvider()

  private val onwardRoute: Call = controllers.routes.IndexController.onPageLoad

  private val personName: PersonName = PersonName("Jane", "Doe")

  private val userAnswers: UserAnswers = ua.set(TrusteeNameId(0), personName).success.value

  private val templateToBeRendered: String = "dob.njk"

  private val commonJson: JsObject =
    Json.obj(
      "name"       -> "Jane Doe",
      "schemeName" -> "Test scheme name",
      "entityType" -> "the individual"
    )

  private val formData: LocalDate =
    LocalDate.parse("2000-01-01")

  private val day: Int = formData.getDayOfMonth
  private val month: Int = formData.getMonthValue
  private val year: Int = formData.getYear

  private def controller(
                          dataRetrievalAction: DataRetrievalAction
                        ): TrusteeDOBController =
    new TrusteeDOBController(
      messagesApi               = messagesApi,
      navigator                 = new FakeNavigator(desiredRoute = onwardRoute),
      authenticate              = new FakeAuthAction(),
      getData                   = dataRetrievalAction,
      requireData               = new DataRequiredActionImpl,
      formProvider              = formProvider,
      dataUpdateService         = mockDataUpdateService,
      controllerComponents      = controllerComponents,
      userAnswersCacheConnector = mockUserAnswersCacheConnector,
      renderer                  = new Renderer(mockAppConfig, mockRenderer)
    )

  override def beforeEach(): Unit = {
    reset(
      mockRenderer,
      mockUserAnswersCacheConnector
    )
  }

  "TrusteeDOBController" must {
    "return OK and the correct view for a GET" in {
      when(mockRenderer.render(any(), any())(any()))
        .thenReturn(Future.successful(Html("")))

      val templateCaptor : ArgumentCaptor[String] = ArgumentCaptor.forClass(classOf[String])

      val jsonCaptor: ArgumentCaptor[JsObject] = ArgumentCaptor.forClass(classOf[JsObject])

      val getData = new FakeDataRetrievalAction(Some(userAnswers))

      val result: Future[Result] =
        controller(getData)
          .onPageLoad(0, NormalMode)(fakeDataRequest(userAnswers))

      status(result) mustBe OK

      verify(mockRenderer, times(1))
        .render(templateCaptor.capture(), jsonCaptor.capture())(any())

      templateCaptor.getValue mustEqual templateToBeRendered

      val json: JsObject =
        Json.obj("date" -> DateInput.localDate(form("date")))

      jsonCaptor.getValue must containJson(commonJson ++ json)
    }

    "populate the view correctly on a GET when the question has previously been answered" in {
      when(mockRenderer.render(any(), any())(any()))
        .thenReturn(Future.successful(Html("")))

      val ua =
        userAnswers
          .set(TrusteeDOBId(0), formData).success.value

      val templateCaptor : ArgumentCaptor[String] = ArgumentCaptor.forClass(classOf[String])

      val jsonCaptor: ArgumentCaptor[JsObject] = ArgumentCaptor.forClass(classOf[JsObject])

      val getData = new FakeDataRetrievalAction(Some(ua))

      val result: Future[Result] =
        controller(getData)
          .onPageLoad(0, NormalMode)(fakeDataRequest(userAnswers))

      status(result) mustBe OK

      verify(mockRenderer, times(1))
        .render(templateCaptor.capture(), jsonCaptor.capture())(any())

      templateCaptor.getValue mustEqual templateToBeRendered

      val json: JsObject =
        Json.obj("date" -> DateInput.localDate(form.fill(formData).apply("date")))

      jsonCaptor.getValue must containJson(commonJson ++ json)
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

      redirectLocation(result) mustBe Some(onwardRoute.url)

      verify(mockUserAnswersCacheConnector, times(1))
        .save(any(), any())(any(), any())
    }

    "return a Bad Request and errors when invalid data is submitted" in {
      when(mockRenderer.render(any(), any())(any()))
        .thenReturn(Future.successful(Html("")))

      val request: FakeRequest[AnyContentAsFormUrlEncoded] =
        fakeRequest
          .withFormUrlEncodedBody("date" -> "invalid value")

      val getData = new FakeDataRetrievalAction(Some(userAnswers))

      val jsonCaptor: ArgumentCaptor[JsObject] = ArgumentCaptor.forClass(classOf[JsObject])

      val templateCaptor : ArgumentCaptor[String] = ArgumentCaptor.forClass(classOf[String])

      val result: Future[Result] =
        controller(getData)
          .onSubmit(0, NormalMode)(request)

      val boundForm = form.bind(Map("date" -> "invalid value"))

      status(result) mustBe BAD_REQUEST

      verify(mockRenderer, times(1))
        .render(templateCaptor.capture(), jsonCaptor.capture())(any())

      templateCaptor.getValue mustEqual templateToBeRendered

      val json: JsObject =
        Json.obj("date" -> DateInput.localDate(boundForm.apply("date")))

      jsonCaptor.getValue must containJson(commonJson ++ json)

      verify(mockUserAnswersCacheConnector, times(0))
        .save(any(), any())(any(), any())
    }
  }
}
