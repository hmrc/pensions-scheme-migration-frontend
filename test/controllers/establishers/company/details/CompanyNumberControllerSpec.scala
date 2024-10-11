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

package controllers.establishers.company.details

import controllers.ControllerSpecBase
import controllers.actions.{DataRequiredActionImpl, DataRetrievalAction, FakeAuthAction, FakeDataRetrievalAction}
import forms.CompanyNumberFormProvider
import identifiers.establishers.company.CompanyDetailsId
import identifiers.establishers.company.details.CompanyNumberId
import matchers.JsonMatchers
import models.{Index, NormalMode, ReferenceValue}
import org.mockito.ArgumentMatchers.any
import org.scalatest.{BeforeAndAfterEach, TryValues}
import play.api.data.{Form, FormBinding}
import play.api.libs.json.Json
import play.api.mvc.{AnyContentAsFormUrlEncoded, Request, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers.{status, _}
import services.common.details.CommonEnterReferenceValueService
import uk.gov.hmrc.viewmodels.NunjucksSupport
import utils.Data.{companyDetails, schemeName, ua}
import utils.{FakeNavigator, UserAnswers}
import views.html.{EnterReferenceValueView, EnterReferenceValueWithHintView}

import scala.concurrent.Future

class CompanyNumberControllerSpec extends ControllerSpecBase with NunjucksSupport with JsonMatchers with TryValues with BeforeAndAfterEach {

  private val index: Index = Index(0)
  private val referenceValue: ReferenceValue = ReferenceValue("12345678")
  private val userAnswers: UserAnswers = ua.set(CompanyDetailsId(index), companyDetails).success.value
  private val formProvider: CompanyNumberFormProvider = new CompanyNumberFormProvider()

  private def getView(req: Request[_], form: Form[_]) = {
    app.injector.instanceOf[EnterReferenceValueWithHintView].apply(
      form,
      schemeName,
      messages("messages__companyNumber", messages("messages__company")),
      messages("messages__companyNumber", companyDetails.companyName),
      "govuk-label--xl",
      Seq(),
      Some(messages("messages__companyNumber__hint")),
      routes.CompanyNumberController.onSubmit(index, NormalMode)
    )(req, implicitly)
  }

  private def controller(dataRetrievalAction: DataRetrievalAction): CompanyNumberController =
    new CompanyNumberController(messagesApi, new FakeAuthAction(), dataRetrievalAction,
      new DataRequiredActionImpl, formProvider,
      common = new CommonEnterReferenceValueService(
        controllerComponents = controllerComponents,
        userAnswersCacheConnector = mockUserAnswersCacheConnector,
        navigator = new FakeNavigator(desiredRoute = onwardCall),
        enterReferenceValueView = app.injector.instanceOf[EnterReferenceValueView],
        enterReferenceValueWithHintView = app.injector.instanceOf[EnterReferenceValueWithHintView],
        messagesApi = messagesApi
      ))

  override def beforeEach(): Unit = reset(mockUserAnswersCacheConnector)

  "CompanyNumberController" must {
    "return OK and the correct view for a GET" in {
      val getData = new FakeDataRetrievalAction(Some(userAnswers))
      val req = fakeDataRequest(userAnswers)
      val result: Future[Result] = controller(getData).onPageLoad(0, NormalMode)(req)

      status(result) mustBe OK
      compareResultAndView(result, getView(req, formProvider(companyDetails.companyName)))

    }

    "populate the view correctly on a GET when the question has previously been answered" in {

      val ua = userAnswers.set(CompanyNumberId(0), referenceValue).success.value
      val getData = new FakeDataRetrievalAction(Some(ua))

      val req = fakeDataRequest(userAnswers)
      val result: Future[Result] = controller(getData).onPageLoad(0, NormalMode)(req)

      status(result) mustBe OK
      compareResultAndView(result, getView(req, formProvider(companyDetails.companyName).fill(referenceValue)))
    }

    "redirect to the next page when valid data is submitted" in {
      when(mockUserAnswersCacheConnector.save(any(), any())(any(), any())).thenReturn(Future.successful(Json.obj()))
      val request: FakeRequest[AnyContentAsFormUrlEncoded] = fakeRequest.withFormUrlEncodedBody("value" -> referenceValue.value)
      val getData = new FakeDataRetrievalAction(Some(userAnswers))

      val result: Future[Result] = controller(getData).onSubmit(0, NormalMode)(request)

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(onwardCall.url)
      verify(mockUserAnswersCacheConnector, times(1)).save(any(), any())(any(), any())
    }

    "return a Bad Request and errors when invalid data is submitted" in {
      val request = fakeRequest.withFormUrlEncodedBody(("value", "invalid value"))
      val getData = new FakeDataRetrievalAction(Some(userAnswers))

      val result: Future[Result] = controller(getData).onSubmit(0, NormalMode)(request)
      val testForm = formProvider(companyDetails.companyName).bindFromRequest()(request, FormBinding.Implicits.formBinding)

      status(result) mustBe BAD_REQUEST
      compareResultAndView(result, getView(request, testForm))
      verify(mockUserAnswersCacheConnector, times(0)).save(any(), any())(any(), any())
    }
  }
}
