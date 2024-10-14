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

package controllers.trustees.company.address

import controllers.ControllerSpecBase
import controllers.actions.MutableFakeDataRetrievalAction
import forms.address.PostcodeFormProvider
import identifiers.beforeYouStart.SchemeNameId
import matchers.JsonMatchers
import models.{NormalMode, Scheme}
import org.mockito.ArgumentMatchers.any
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceableModule
import play.api.mvc.{Result, Results}
import play.api.mvc.Results.{BadRequest, Ok}
import play.api.test.Helpers._
import play.twirl.api.Html
import services.common.address.CommonPostcodeService
import utils.Data.ua
import utils.{Data, Enumerable, UserAnswers}
import views.html.address.PostcodeView

import scala.concurrent.Future

class EnterPreviousPostcodeControllerSpec extends ControllerSpecBase with JsonMatchers with Enumerable.Implicits {

  private val mockCommonPostcodeService = mock[CommonPostcodeService]

  val extraModules: Seq[GuiceableModule] = Seq(
    bind[CommonPostcodeService].toInstance(mockCommonPostcodeService)
  )
  private val mutableFakeDataRetrievalAction: MutableFakeDataRetrievalAction = new MutableFakeDataRetrievalAction()
  override def fakeApplication(): Application = applicationBuilderMutableRetrievalAction(mutableFakeDataRetrievalAction, extraModules).build()

  private val formProvider: PostcodeFormProvider = new PostcodeFormProvider()
  private val form = formProvider("companyEnterPreviousPostcode.required", "enterPostcode.invalid")
  private val mode = NormalMode
  private val index = 0

  private val userAnswers: Option[UserAnswers] = Some(ua)
  private val httpPathGET: String = controllers.trustees.company.address.routes.EnterPreviousPostcodeController.onPageLoad(index, mode).url
  private val httpPathPOST: String = controllers.trustees.company.address.routes.EnterPreviousPostcodeController.onSubmit(index, mode).url

  private val valuesValid: Map[String, Seq[String]] = Map(
    "value" -> Seq("ZZ11ZZ")
  )

  private val valuesInvalid: Map[String, Seq[String]] = Map(
    "value" -> Seq.empty
  )
  val request = httpGETRequest(httpPathGET)

  override def beforeEach(): Unit = {
    super.beforeEach()

  }

  "EnterPreviousPostcode Controller" must {

    "Return OK and the correct view for a GET" in {
      val ua: UserAnswers = UserAnswers().setOrException(SchemeNameId, Data.schemeName)
      mutableFakeDataRetrievalAction.setDataToReturn(Some(ua))
      val view = app.injector.instanceOf[PostcodeView]
      val expectedView = view(
        form, "entityType", "entityName",
        routes.EnterPreviousPostcodeController.onSubmit(index, mode),
        routes.ConfirmPreviousAddressController.onPageLoad(index, mode).url,
        Some(Data.schemeName),
        h1MessageKey = "previousPostcode.title"
      )(fakeRequest, messages)
      when(mockCommonPostcodeService.get(any(), any())(any(), any()))
        .thenReturn(Future.successful(Ok(expectedView)))

      val result: Future[Result] = route(app, httpGETRequest(httpPathGET)).value

      status(result) mustEqual OK

      compareResultAndView(result, expectedView)
    }

    "redirect to Session Expired page for a GET when there is no data" in {
      val ua: UserAnswers = UserAnswers()

      mutableFakeDataRetrievalAction.setDataToReturn(Some(ua))

      val result: Future[Result] = route(app, request).value

      status(result) mustEqual SEE_OTHER

      redirectLocation(result).value mustBe controllers.routes.SessionExpiredController.onPageLoad().absoluteURL()(request)
    }

    "Save data to user answers and redirect to next page when valid data is submitted" in {
      when(mockCommonPostcodeService.post(any(), any(), any(), any(), any())(any(), any(), any()))
        .thenReturn(Future.successful(Results.SeeOther(onwardCall.url)))

      mutableFakeDataRetrievalAction.setDataToReturn(userAnswers)

      val result = route(app, httpPOSTRequest(httpPathPOST, valuesValid)).value

      status(result) mustEqual SEE_OTHER
      redirectLocation(result) mustBe Some(onwardCall.url)
    }

    "return a BAD REQUEST when invalid data is submitted" in {
      mutableFakeDataRetrievalAction.setDataToReturn(userAnswers)
      when(mockCommonPostcodeService.post(any(), any(), any(), any(), any())(any(), any(), any()))
        .thenReturn(Future.successful(BadRequest))

      val result = route(app, httpPOSTRequest(httpPathPOST, valuesInvalid)).value

      status(result) mustEqual BAD_REQUEST

      verify(mockUserAnswersCacheConnector, times(0)).save(any(), any())(any(), any())
    }

    "redirect to Session Expired page for a POST when there is no data" in {
      mutableFakeDataRetrievalAction.setDataToReturn(None)

      val result = route(app, httpPOSTRequest(httpPathPOST, valuesValid)).value

      status(result) mustEqual SEE_OTHER

      redirectLocation(result).value mustBe controllers.preMigration.routes.ListOfSchemesController.onPageLoad(Scheme).url
    }
  }
}
