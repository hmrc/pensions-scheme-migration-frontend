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

package controllers.aboutMembership

import controllers.ControllerSpecBase
import controllers.actions.MutableFakeDataRetrievalAction
import forms.aboutMembership.MembersFormProvider
import identifiers.aboutMembership.FutureMembersId
import matchers.JsonMatchers
import models.{Members, Scheme}
import org.mockito.ArgumentMatchers.any
import org.mockito.{ArgumentCaptor, ArgumentMatchers}
import org.mockito.Mockito._
import play.api.Application
import play.api.data.Form
import play.api.i18n.Messages
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers._
import utils.Data.{schemeName, ua}
import utils.{Enumerable, UserAnswers}
import views.html.aboutMembership.MembersView

import scala.concurrent.Future

class FutureMembersControllerSpec extends ControllerSpecBase with JsonMatchers with Enumerable.Implicits {
  private val userAnswers: Option[UserAnswers] = Some(ua)
  private val form: Form[Members] = new MembersFormProvider()(messages("futureMembers.error.required", schemeName))

  private val mutableFakeDataRetrievalAction: MutableFakeDataRetrievalAction = new MutableFakeDataRetrievalAction()

  private val application: Application = applicationBuilderMutableRetrievalAction(mutableFakeDataRetrievalAction).build()

  private def httpPathGET: String = controllers.aboutMembership.routes.FutureMembersController.onPageLoad.url
  private def httpPathPOST: String = controllers.aboutMembership.routes.FutureMembersController.onSubmit.url

  private val valuesValid: Map[String, Seq[String]] = Map(
    "value" -> Seq(Members.One.toString)
  )

  private val valuesInvalid: Map[String, Seq[String]] = Map(
    "value" -> Seq.empty
  )

  override def beforeEach(): Unit = {
    super.beforeEach()
  }


  "FutureMembers Controller" must {

    "return OK and the correct view for a GET" in {
      mutableFakeDataRetrievalAction.setDataToReturn(userAnswers)
      val onwardCall: Call = Call("POST", httpPathPOST)
      val request = FakeRequest(GET, httpPathGET)

      val result = route(application, httpGETRequest(httpPathGET)).value

      status(result) mustEqual OK

      val view = application.injector.instanceOf[MembersView].apply(
        form,
        schemeName,
        messages("futureMembers.title", Messages("messages__the_scheme")),
        messages("futureMembers.title", schemeName),
        Members.radios(form),
        onwardCall
      )(request, messages)

      compareResultAndView(result, view)

    }

    "return OK and the correct view for a GET when the question has previously been answered" in {
      val ua = userAnswers.map(_.set(FutureMembersId, Members.One)).get.toOption.get
      val onwardCall: Call = Call("POST", httpPathPOST)
      val request = FakeRequest(GET, httpPathGET)
      mutableFakeDataRetrievalAction.setDataToReturn(Option(ua))

      val result = route(application, httpGETRequest(httpPathGET)).value

      status(result) mustEqual OK

      val view = application.injector.instanceOf[MembersView].apply(
        form.fill(Members.One),
        schemeName,
        messages("futureMembers.title", Messages("messages__the_scheme")),
        messages("futureMembers.title", schemeName),
        Members.radios(form),
        onwardCall
      )(request, messages)

      compareResultAndView(result, view)

    }

    "redirect to Session Expired page for a GET when there is no data" in {
      mutableFakeDataRetrievalAction.setDataToReturn(None)

      val result = route(application, httpGETRequest(httpPathGET)).value

      status(result) mustEqual SEE_OTHER

      redirectLocation(result).value mustBe controllers.preMigration.routes.ListOfSchemesController.onPageLoad(Scheme).url
    }

    "Save data to user answers and redirect to next page when valid data is submitted" in {

      val expectedJson = Json.obj()

      when(mockCompoundNavigator.nextPage(ArgumentMatchers.eq(FutureMembersId), any(), any())(any()))
        .thenReturn(routes.CheckYourAnswersController.onPageLoad)
      when(mockUserAnswersCacheConnector.save(any(), any())(any(), any()))
        .thenReturn(Future.successful(Json.obj()))

      mutableFakeDataRetrievalAction.setDataToReturn(userAnswers)

      val jsonCaptor: ArgumentCaptor[JsObject] = ArgumentCaptor.forClass(classOf[JsObject])

      val result = route(application, httpPOSTRequest(httpPathPOST, valuesValid)).value

      status(result) mustEqual SEE_OTHER

      verify(mockUserAnswersCacheConnector, times(1)).save(any(), jsonCaptor.capture)(any(), any())

      jsonCaptor.getValue must containJson(expectedJson)

      redirectLocation(result) mustBe Some(routes.CheckYourAnswersController.onPageLoad.url)
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
