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

package controllers.benefitsAndInsurance

import controllers.ControllerSpecBase
import controllers.actions.MutableFakeDataRetrievalAction
import forms.benefitsAndInsurance.HowProvideBenefitsFormProvider
import identifiers.beforeYouStart.SchemeNameId
import identifiers.benefitsAndInsurance.HowProvideBenefitsId
import matchers.JsonMatchers
import models.Scheme
import models.benefitsAndInsurance.BenefitsProvisionType
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import play.api.Application
import play.api.data.Form
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.{Call, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import utils.Data.{schemeName, ua}
import utils.{Data, Enumerable, UserAnswers}
import views.html.benefitsAndInsurance.HowProvideBenefitsView

import scala.concurrent.Future

class HowProvideBenefitsControllerSpec extends ControllerSpecBase with JsonMatchers with Enumerable.Implicits {

  private val userAnswers: Option[UserAnswers] = Some(ua)
  private val mutableFakeDataRetrievalAction: MutableFakeDataRetrievalAction = new MutableFakeDataRetrievalAction()
  private val application: Application = applicationBuilderMutableRetrievalAction(mutableFakeDataRetrievalAction).build()
  private val httpPathGET: String = controllers.benefitsAndInsurance.routes.HowProvideBenefitsController.onPageLoad.url
  private val httpPathPOST: String = controllers.benefitsAndInsurance.routes.HowProvideBenefitsController.onSubmit.url
  private val form: Form[BenefitsProvisionType] = new HowProvideBenefitsFormProvider()()

  private val valuesValid: Map[String, Seq[String]] = Map(
    "value" -> Seq("moneyPurchaseOnly")
  )

  private val valuesInvalid: Map[String, Seq[String]] = Map(
    "value" -> Seq.empty
  )

  val request = FakeRequest(GET, httpPathGET)
  override val onwardCall: Call = Call("POST", httpPathPOST)

  override def beforeEach(): Unit = {
    super.beforeEach()

  }

  "HowProvideBEnefits Controller" must {

    "Return OK and the correct view for a GET" in {
      val ua: UserAnswers = UserAnswers().setOrException(SchemeNameId, Data.schemeName)
      mutableFakeDataRetrievalAction.setDataToReturn(Some(ua))

      val result: Future[Result] = route(application, httpGETRequest(httpPathGET)).value

      status(result) mustEqual OK

      val view = application.injector.instanceOf[HowProvideBenefitsView].apply(
        form,
        schemeName,
        BenefitsProvisionType.radios(form),
        onwardCall
      )(request, messages)

      compareResultAndView(result, view)

    }

    "return OK and the correct view for a GET when the question has previously been answered" in {
      val ua: UserAnswers = UserAnswers()
        .setOrException(SchemeNameId, Data.schemeName)
        .setOrException(HowProvideBenefitsId, BenefitsProvisionType.MoneyPurchaseOnly)

      mutableFakeDataRetrievalAction.setDataToReturn(Some(ua))

      val filledFrom = form.fill(BenefitsProvisionType.MoneyPurchaseOnly)

      val result: Future[Result] = route(application, httpGETRequest(httpPathGET)).value

      status(result) mustEqual OK

      val view = application.injector.instanceOf[HowProvideBenefitsView].apply(
        filledFrom,
        schemeName,
        BenefitsProvisionType.radios(filledFrom),
        onwardCall
      )(request, messages)

      compareResultAndView(result, view)

    }

    "redirect to Session Expired page for a GET when there is no data" in {
      val ua: UserAnswers = UserAnswers()

      mutableFakeDataRetrievalAction.setDataToReturn(Some(ua))

      val req = httpGETRequest(httpPathGET)
      val result: Future[Result] = route(application, req).value

      status(result) mustEqual SEE_OTHER
      redirectLocation(result).value mustBe controllers.routes.SessionExpiredController.onPageLoad().absoluteURL()(req)
    }

    "Save data to user answers and redirect to next page when valid data is submitted" in {

      val expectedJson = Json.obj(
        HowProvideBenefitsId.toString -> BenefitsProvisionType.MoneyPurchaseOnly.toString
      )

      when(mockUserAnswersCacheConnector.save(any(), any())(any(), any()))
        .thenReturn(Future.successful(Json.obj()))

      mutableFakeDataRetrievalAction.setDataToReturn(userAnswers)

      val jsonCaptor: ArgumentCaptor[JsObject] = ArgumentCaptor.forClass(classOf[JsObject])

      val result = route(application, httpPOSTRequest(httpPathPOST, valuesValid)).value

      status(result) mustEqual SEE_OTHER

      verify(mockUserAnswersCacheConnector, times(1)).save(any(), jsonCaptor.capture)(any(), any())

      jsonCaptor.getValue must containJson(expectedJson)

      redirectLocation(result) mustBe Some(onwardCall.url)
    }

    "return a BAD REQUEST when invalid data is submitted" in {
      mutableFakeDataRetrievalAction.setDataToReturn(userAnswers)

      val result = route(application, httpPOSTRequest(httpPathPOST, valuesInvalid)).value

      status(result) mustEqual BAD_REQUEST

      verify(mockUserAnswersCacheConnector, times(0)).save(any(), any())(any(), any())
    }

    "redirect back to list of schemes for a POST when there is no data" in {
      mutableFakeDataRetrievalAction.setDataToReturn(None)

      val result = route(application, httpPOSTRequest(httpPathPOST, valuesValid)).value

      status(result) mustEqual SEE_OTHER

      redirectLocation(result).value mustBe controllers.preMigration.routes.ListOfSchemesController.onPageLoad(Scheme).url
    }
  }

}
