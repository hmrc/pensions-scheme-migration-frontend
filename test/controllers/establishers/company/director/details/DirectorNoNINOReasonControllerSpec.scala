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

package controllers.establishers.company.director.details

import controllers.ReasonControllerSpecBase
import identifiers.establishers.company.director.DirectorNameId
import identifiers.establishers.company.director.details.DirectorNoNINOReasonId
import matchers.JsonMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{when, verify, times}
import org.scalatest.{BeforeAndAfterEach, TryValues}
import play.api.data.Form
import play.api.mvc.AnyContentAsEmpty
import play.api.mvc.Results.{BadRequest, Ok, Redirect}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import utils.Data.ua
import utils.UserAnswers
import views.html.ReasonView

import scala.concurrent.Future

class DirectorNoNINOReasonControllerSpec
  extends ReasonControllerSpecBase
    with JsonMatchers
    with TryValues
    with BeforeAndAfterEach {


  private val form: Form[String] =
    formProvider(s"Enter a reason why ${personName.fullName} does not have a NINO")
  private val userAnswers: UserAnswers =
    ua.set(DirectorNameId(0,0), personName).success.value

  private val pageTitle = "Why does the director not have a National Insurance number?"
  private val pageHeading = "Why does Jane Doe not have a National Insurance number?"

  override def beforeEach(): Unit = {
    super.beforeEach()
    mutableFakeDataRetrievalAction.setDataToReturn(Some(ua))
  }

  private val httpPathGET = controllers.establishers.company.director.details.routes.DirectorNoNINOReasonController
    .onPageLoad(establisherIndex, directorIndex, mode).url
  private val submitCall = controllers.establishers.company.director.details.routes.DirectorNoNINOReasonController
    .onSubmit(establisherIndex, directorIndex, mode)
  private val httpPathPOST = submitCall.url
  val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, httpPathGET)

  "DirectorNoNINOReasonController" must {
    "return OK and the correct view for a GET" in {
      val view = application.injector.instanceOf[ReasonView].apply(
        pageTitle, pageHeading, isPageHeading, form, schemeName, submitCall
      )(request, messages)

      when(mockCommonReasonService.get(any(), any(), any(), any(), any(), any(), any())(any()))

        .thenReturn(Future.successful(Ok(view)))

      val result = route(application, httpGETRequest(httpPathGET)).value

      status(result) mustBe OK

      compareResultAndView(result, view)
    }

    "populate the view correctly on a GET when the question has previously been answered" in {
      val ua = userAnswers.set(DirectorNoNINOReasonId(0,0), formData).success.value

      mutableFakeDataRetrievalAction.setDataToReturn(Some(ua))

      val filledForm = form.bind(Map("form" -> formData))

      val view = application.injector.instanceOf[ReasonView].apply(
        pageTitle, pageHeading, isPageHeading, filledForm, schemeName, submitCall
      )(request, messages)

      when(mockCommonReasonService.get(any(), any(), any(), any(), any(), any(), any())(any()))

        .thenReturn(Future.successful(Ok(view)))

      val result = route(application, httpGETRequest(httpPathGET)).value

      status(result) mustBe OK

      compareResultAndView(result, view)
    }
    "redirect to the next page when valid data is submitted" in {
      val ua = userAnswers.set(DirectorNoNINOReasonId(0,0), formData).success.value

      mutableFakeDataRetrievalAction.setDataToReturn(Some(ua))

      val filledForm = form.bind(Map("form" -> formData))

      val view = application.injector.instanceOf[ReasonView].apply(
        pageTitle, pageHeading, isPageHeading, filledForm, schemeName, submitCall
      )(request, messages)

      when(mockCommonReasonService.get(any(), any(), any(), any(), any(), any(), any())(any()))

        .thenReturn(Future.successful(Ok(view)))
      when(mockCommonReasonService.post(any(), any(), any(), any(), any(), any(), any(), any(), any())(any(), any()))
        .thenReturn(Future.successful(Redirect(onwardCall.url)))

      val result = route(application, httpPOSTRequest(httpPathPOST, validValues)).value

      status(result) mustBe SEE_OTHER

      redirectLocation(result) mustBe Some(onwardCall.url)
    }
    "return a Bad Request and errors when invalid data is submitted" in {
      when(mockCommonReasonService.post(any(), any(), any(), any(), any(), any(), any(), any(), any())(any(), any()))
        .thenReturn(Future.successful(BadRequest))

      val result = route(application, httpPOSTRequest(httpPathPOST, invalidValues)).value

      status(result) mustBe BAD_REQUEST

      verify(mockUserAnswersCacheConnector, times(0)).save(any(), any())(any(), any())
    }
  }
}
