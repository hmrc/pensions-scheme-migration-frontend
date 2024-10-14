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

package controllers.establishers.company

import controllers.ControllerSpecBase
import controllers.actions.MutableFakeDataRetrievalAction
import forms.CompanyDetailsFormProvider
import identifiers.establishers.company.CompanyDetailsId
import matchers.JsonMatchers
import models.CompanyDetails
import org.mockito.ArgumentMatchers.any
import org.scalatest.{BeforeAndAfterEach, TryValues}
import play.api.Application
import play.api.libs.json.Json
import play.api.test.Helpers._
import play.twirl.api.Html
import uk.gov.hmrc.nunjucks.NunjucksSupport
import utils.Data.ua
import utils.{Data, UserAnswers}

import scala.concurrent.Future

class CompanyDetailsControllerSpec extends ControllerSpecBase
  with NunjucksSupport
  with JsonMatchers
  with TryValues
  with BeforeAndAfterEach {

  private val companyName = "test company"

  private val formData: CompanyDetails = CompanyDetails(companyName)
  override def beforeEach(): Unit = {
    super.beforeEach()
    mutableFakeDataRetrievalAction.setDataToReturn(Some(ua))
    when(mockUserAnswersCacheConnector.save(any(), any())(any(), any())).thenReturn(Future.successful(Json.obj()))

  }

  private val mutableFakeDataRetrievalAction: MutableFakeDataRetrievalAction = new MutableFakeDataRetrievalAction()

  private val application: Application =
    applicationBuilderMutableRetrievalAction(mutableFakeDataRetrievalAction).build()

  private val formProvider = new CompanyDetailsFormProvider()
  private val form = formProvider()
  private val request = httpGETRequest(routes.CompanyDetailsController.onPageLoad(0).url)

  private val validValues = Map("companyName" -> Seq(companyName))
  private val invalidValues = Map("companyName" -> Seq(""))

  private val submitCall = routes.CompanyDetailsController.onSubmit(0)
  private val submitUrl = submitCall.url

  "establiser CompanyDetailsController" must {
    "return OK and the correct view for a GET" in {
      val result = route(application, request).value

      status(result) mustBe OK

      val view = application.injector.instanceOf[views.html.CompanyDetailsView].apply(form, Data.schemeName, submitCall)(request, messages)

      compareResultAndView(result, view)
    }

    "populate the view correctly on a GET when the question has previously been answered" in {
      val prepopUA: UserAnswers = ua.set(CompanyDetailsId(0), formData).toOption.value
      mutableFakeDataRetrievalAction.setDataToReturn(Some(prepopUA))

      val result = route(application, request).value

      status(result) mustBe OK

      val filledForm = form.bind(Map("companyName" -> companyName))

      val view = application.injector.instanceOf[views.html.CompanyDetailsView].apply(filledForm, Data.schemeName, submitCall)(request, messages)

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
