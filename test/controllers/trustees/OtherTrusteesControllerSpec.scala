/*
 * Copyright 2021 HM Revenue & Customs
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

package controllers.trustees

import controllers.ControllerSpecBase
import controllers.actions.{DataRetrievalAction, FakeAuthAction, DataRequiredActionImpl, FakeDataRetrievalAction}
import forms.HasReferenceNumberFormProvider
import identifiers.trustees.OtherTrusteesId
import matchers.JsonMatchers
import models.NormalMode
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.scalatest.{BeforeAndAfterEach, TryValues}
import play.api.data.Form
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.{Call, AnyContentAsFormUrlEncoded, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers.{status, _}
import play.twirl.api.Html
import renderer.Renderer
import uk.gov.hmrc.nunjucks.NunjucksSupport
import uk.gov.hmrc.viewmodels.Radios
import utils.Data.{schemeName, ua}
import utils.FakeNavigator

import scala.concurrent.Future

class OtherTrusteesControllerSpec extends ControllerSpecBase
  with NunjucksSupport
  with JsonMatchers
  with TryValues
  with BeforeAndAfterEach{

  private val onwardRoute: Call =
    controllers.routes.IndexController.onPageLoad()
  private val formProvider: HasReferenceNumberFormProvider =
    new HasReferenceNumberFormProvider()

  private val templateToBeRendered: String =
    "hasReferenceValueWithHint.njk"
  private val form: Form[Boolean] =
    formProvider("Select yes if you need to tell us about other trustees")

  private val commonJson: JsObject =
    Json.obj(
      "pageTitle"     -> "Do you need to tell us about other trustees?",
      "pageHeading"     -> "Do you need to tell us about other trustees?",
      "schemeName"    -> schemeName,
      "paragraphs"    -> Json.arr(
        "You do not have to add them now. We may contact you if we need this information."
      ),
      "legendClass"   -> "govuk-visually-hidden",
      "isPageHeading" -> true
    )

  override def beforeEach: Unit = {
    reset(
      mockRenderer,
      mockUserAnswersCacheConnector
    )
  }

  private def controller(
                          dataRetrievalAction: DataRetrievalAction
                        ): OtherTrusteesController =
    new OtherTrusteesController(
      messagesApi               = messagesApi,
      navigator                 = new FakeNavigator(desiredRoute = onwardRoute),
      authenticate              = new FakeAuthAction(),
      getData                   = dataRetrievalAction,
      requireData               = new DataRequiredActionImpl,
      formProvider              = formProvider,
      controllerComponents      = controllerComponents,
      userAnswersCacheConnector = mockUserAnswersCacheConnector,
      renderer                  = new Renderer(mockAppConfig, mockRenderer)
    )

  "OtherTrusteesController" must {

    "return OK and the correct view for a GET" in{

      val getData = new FakeDataRetrievalAction(Some(ua))
      val templateCaptor = ArgumentCaptor.forClass(classOf[String])
      val jsonCaptor = ArgumentCaptor.forClass(classOf[JsObject])

      when(mockRenderer.render(any(), any())(any()))
        .thenReturn(Future.successful(Html("")))

      val result: Future[Result] =
        controller(getData)
          .onPageLoad(NormalMode)(fakeDataRequest(ua))

      status(result) mustBe OK

      verify(mockRenderer, times(1))
        .render(templateCaptor.capture(), jsonCaptor.capture())(any())

      templateCaptor.getValue mustEqual  templateToBeRendered

      val json: JsObject =
        Json.obj("radios" -> Radios.yesNo(form("value")))

      jsonCaptor.getValue must containJson(commonJson ++ json)
    }

    "populate the view correctly on a GET when the question has previously been answered Yes" in {
      when(mockRenderer.render(any(), any())(any()))
        .thenReturn(Future.successful(Html("")))

      val userAnswers =
        ua
          .set(OtherTrusteesId, true).success.value

      val templateCaptor = ArgumentCaptor.forClass(classOf[String])

      val jsonCaptor = ArgumentCaptor.forClass(classOf[JsObject])

      val getData = new FakeDataRetrievalAction(Some(userAnswers))

      val result: Future[Result] =
        controller(getData)
          .onPageLoad(NormalMode)(fakeDataRequest(userAnswers))

      status(result) mustBe OK

      verify(mockRenderer, times(1))
        .render(templateCaptor.capture(), jsonCaptor.capture())(any())

      templateCaptor.getValue mustEqual templateToBeRendered

      val json: JsObject =
        Json.obj("radios" -> Radios.yesNo(form.fill(true).apply("value")))

      jsonCaptor.getValue must containJson(commonJson ++ json)
    }

    "populate the view correctly on a GET when the question has previously been answered No" in {
      when(mockRenderer.render(any(), any())(any()))
        .thenReturn(Future.successful(Html("")))

      val userAnswers =
        ua
          .set(OtherTrusteesId, false).success.value

      val templateCaptor = ArgumentCaptor.forClass(classOf[String])

      val jsonCaptor = ArgumentCaptor.forClass(classOf[JsObject])

      val getData = new FakeDataRetrievalAction(Some(userAnswers))

      val result: Future[Result] =
        controller(getData)
          .onPageLoad(NormalMode)(fakeDataRequest(userAnswers))

      status(result) mustBe OK

      verify(mockRenderer, times(1))
        .render(templateCaptor.capture(), jsonCaptor.capture())(any())

      templateCaptor.getValue mustEqual templateToBeRendered

      val json: JsObject =
        Json.obj("radios" -> Radios.yesNo(form.fill(false).apply("value")))

      jsonCaptor.getValue must containJson(commonJson ++ json)
    }

    "redirect to the next page when valid data is submitted" in {
      when(mockUserAnswersCacheConnector.save(any(), any())(any(), any()))
        .thenReturn(Future.successful(Json.obj()))

      val request: FakeRequest[AnyContentAsFormUrlEncoded] =
        fakeRequest
          .withFormUrlEncodedBody(("value", "true"))

      val getData = new FakeDataRetrievalAction(Some(ua))

      val result: Future[Result] =
        controller(getData)
          .onSubmit(NormalMode)(request)

      status(result) mustBe SEE_OTHER

      redirectLocation(result) mustBe Some(onwardRoute.url)

      verify(mockUserAnswersCacheConnector, times(1))
        .save(any(), any())(any(), any())
    }

    "return a Bad Request and errors when invalid data is submitted" in {
      when(mockRenderer.render(any(), any())(any()))
        .thenReturn(Future.successful(Html("")))

      val request =
        fakeRequest
          .withFormUrlEncodedBody(("value", "invalid value"))

      val getData = new FakeDataRetrievalAction(Some(ua))

      val jsonCaptor = ArgumentCaptor.forClass(classOf[JsObject])

      val templateCaptor = ArgumentCaptor.forClass(classOf[String])

      val result: Future[Result] =
        controller(getData)
          .onSubmit(NormalMode)(request)

      val boundForm = form.bind(Map("value" -> "invalid value"))

      status(result) mustBe BAD_REQUEST

      verify(mockRenderer, times(1))
        .render(templateCaptor.capture(), jsonCaptor.capture())(any())

      templateCaptor.getValue mustEqual templateToBeRendered

      val json: JsObject =
        Json.obj("radios" -> Radios.yesNo(boundForm.apply("value")))

      jsonCaptor.getValue must containJson(commonJson ++ json)

      verify(mockUserAnswersCacheConnector, times(0))
        .save(any(), any())(any(), any())
    }


  }

}
