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

package controllers.trustees.company

import controllers.ControllerSpecBase
import controllers.actions.MutableFakeDataRetrievalAction
import forms.CompanyDetailsFormProvider
import identifiers.trustees.company.CompanyDetailsId
import matchers.JsonMatchers
import models.CompanyDetails
import org.mockito.ArgumentMatchers.any
import org.scalatest.{BeforeAndAfterEach, TryValues}
import play.api.Application
import play.api.libs.json.Json
import play.api.test.Helpers._
import utils.Data
import utils.Data.ua
import views.html.CompanyDetailsView

import scala.concurrent.Future
class CompanyDetailsControllerSpec extends ControllerSpecBase

  with JsonMatchers
  with TryValues
  with BeforeAndAfterEach {

  private val companyName = "test company"
  private val index = 0
  private val formProvider: CompanyDetailsFormProvider = new CompanyDetailsFormProvider()
  private val form = formProvider()

  private val formData: CompanyDetails = CompanyDetails(companyName)

  private val mutableFakeDataRetrievalAction: MutableFakeDataRetrievalAction = new MutableFakeDataRetrievalAction()

  private val application: Application = applicationBuilderMutableRetrievalAction(mutableFakeDataRetrievalAction).build()

  private val validValues = Map("companyName" -> Seq(companyName))
  private val invalidValues = Map("companyName" -> Seq(""))


  override def beforeEach(): Unit = {
    super.beforeEach()
    mutableFakeDataRetrievalAction.setDataToReturn(Some(ua))
    when(mockUserAnswersCacheConnector.save(any(), any())(any(), any())).thenReturn(Future.successful(Json.obj()))

  }

  private val httpPathGET: String = routes.CompanyDetailsController.onPageLoad(index).url

  private val submitCall = routes.CompanyDetailsController.onSubmit(index)
  private val submitUrl = submitCall.url

  private val request = httpGETRequest(httpPathGET)

  "trustees CompanyDetailsController" must {
    "return OK and the correct view for a GET" in {
      val result = route(application, httpGETRequest(httpPathGET)).value

      status(result) mustBe OK

      val view = application.injector.instanceOf[CompanyDetailsView].apply(
        form,
        Data.schemeName,
        submitCall
      )(request, messages)

      compareResultAndView(result, view)
    }

    "populate the view correctly on a GET when the question has previously been answered" in {
      val answers = ua.set(CompanyDetailsId(0), formData).success.value
      mutableFakeDataRetrievalAction.setDataToReturn(Some(answers))

      val result = route(application, httpGETRequest(httpPathGET)).value

      status(result) mustBe OK

      val filledForm = form.bind(Map("companyName" -> companyName))

      val view = application.injector.instanceOf[CompanyDetailsView].apply(
        filledForm,
        Data.schemeName,
        submitCall
      )(request, messages)

      compareResultAndView(result, view)
    }

    "redirect to the next page when valid data is submitted" in {
      val result = route(application, httpPOSTRequest(submitUrl, validValues)).value

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(onwardCall.url)
    }

    "return a Bad Request and errors when invalid data is submitted" in {
      val result = route(application, httpPOSTRequest(submitUrl, invalidValues)).value

      status(result) mustBe BAD_REQUEST

      verify(mockUserAnswersCacheConnector, times(0)).save(any(), any())(any(), any())
    }
  }
}
