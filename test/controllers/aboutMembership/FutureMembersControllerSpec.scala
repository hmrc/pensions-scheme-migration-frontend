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

package controllers.aboutMembership

import connectors.cache.UserAnswersCacheConnector
import controllers.ControllerSpecBase
import controllers.actions.MutableFakeDataRetrievalAction
import forms.aboutMembership.MembersFormProvider
import identifiers.aboutMembership.FutureMembersId
import matchers.JsonMatchers
import models.Members
import org.mockito.Matchers.any
import org.mockito.Mockito.{times, verify, when}
import org.mockito.{ArgumentCaptor, Matchers}
import play.api.Application
import play.api.data.Form
import play.api.inject.bind
import play.api.inject.guice.GuiceableModule
import play.api.libs.json.{JsObject, Json}
import play.api.test.Helpers._
import play.twirl.api.Html
import uk.gov.hmrc.nunjucks.NunjucksRenderer
import uk.gov.hmrc.viewmodels.NunjucksSupport
import utils.Data.{schemeName, ua}
import utils.{Enumerable, UserAnswers}

import scala.concurrent.Future

class FutureMembersControllerSpec extends ControllerSpecBase with NunjucksSupport with JsonMatchers with Enumerable.Implicits {
  private val userAnswers: Option[UserAnswers] = Some(ua)
  private val templateToBeRendered = "aboutMembership/members.njk"
  private val form: Form[Members] = new MembersFormProvider()(messages("futureMembers.error.required", schemeName))

  private val mutableFakeDataRetrievalAction: MutableFakeDataRetrievalAction = new MutableFakeDataRetrievalAction()
  val extraModules: Seq[GuiceableModule] = Seq(
    bind[NunjucksRenderer].toInstance(mockRenderer),
    bind[UserAnswersCacheConnector].to(mockUserAnswersCacheConnector)
  )
  private val application: Application = applicationBuilder(mutableFakeDataRetrievalAction, extraModules).build()

  private def httpPathGET: String = controllers.aboutMembership.routes.FutureMembersController.onPageLoad.url
  private def httpPathPOST: String = controllers.aboutMembership.routes.FutureMembersController.onSubmit.url

  private val valuesValid: Map[String, Seq[String]] = Map(
    "value" -> Seq(Members.One.toString)
  )

  private val valuesInvalid: Map[String, Seq[String]] = Map(
    "value" -> Seq.empty
  )

  private val jsonToPassToTemplate: Form[Members] => JsObject = form =>
    Json.obj(
      "schemeName" -> schemeName,
      "titleMessage" -> msg"futureMembers.title".withArgs(schemeName),
      "radios" -> Members.radios(form)
    )

  override def beforeEach: Unit = {
    super.beforeEach
    when(mockRenderer.render(any(), any())(any())).thenReturn(Future.successful(Html("")))
  }


  "FutureMembers Controller" must {

    "return OK and the correct view for a GET" in {
      mutableFakeDataRetrievalAction.setDataToReturn(userAnswers)
      val templateCaptor = ArgumentCaptor.forClass(classOf[String])
      val jsonCaptor = ArgumentCaptor.forClass(classOf[JsObject])

      val result = route(application, httpGETRequest(httpPathGET)).value

      status(result) mustEqual OK

      verify(mockRenderer, times(1)).render(templateCaptor.capture(), jsonCaptor.capture())(any())

      templateCaptor.getValue mustEqual templateToBeRendered

      jsonCaptor.getValue must containJson(jsonToPassToTemplate(form))
    }

    "return OK and the correct view for a GET when the question has previously been answered" in {
      val ua = userAnswers.map(_.set(FutureMembersId, Members.One)).get.toOption.get

      mutableFakeDataRetrievalAction.setDataToReturn(Option(ua))

      val templateCaptor = ArgumentCaptor.forClass(classOf[String])

      val jsonCaptor = ArgumentCaptor.forClass(classOf[JsObject])

      val result = route(application, httpGETRequest(httpPathGET)).value

      status(result) mustEqual OK

      verify(mockRenderer, times(1)).render(templateCaptor.capture(), jsonCaptor.capture())(any())

      templateCaptor.getValue mustEqual templateToBeRendered

      jsonCaptor.getValue must containJson(jsonToPassToTemplate(form.fill(Members.One)))
    }

    "redirect to Session Expired page for a GET when there is no data" in {
      mutableFakeDataRetrievalAction.setDataToReturn(None)

      val result = route(application, httpGETRequest(httpPathGET)).value

      status(result) mustEqual SEE_OTHER

      redirectLocation(result).value mustBe controllers.routes.IndexController.onPageLoad().url
    }

    "Save data to user answers and redirect to next page when valid data is submitted" in {

      val expectedJson = Json.obj()

      when(mockCompoundNavigator.nextPage(Matchers.eq(FutureMembersId), any())(any()))
        .thenReturn(routes.CheckYourAnswersController.onPageLoad())
      when(mockUserAnswersCacheConnector.save(any(), any())(any(), any()))
        .thenReturn(Future.successful(Json.obj()))

      mutableFakeDataRetrievalAction.setDataToReturn(userAnswers)

      val jsonCaptor = ArgumentCaptor.forClass(classOf[JsObject])

      val result = route(application, httpPOSTRequest(httpPathPOST, valuesValid)).value

      status(result) mustEqual SEE_OTHER

      verify(mockUserAnswersCacheConnector, times(1)).save(any(), jsonCaptor.capture)(any(), any())

      jsonCaptor.getValue must containJson(expectedJson)

      redirectLocation(result) mustBe Some(routes.CheckYourAnswersController.onPageLoad().url)
    }

    "return a BAD REQUEST when invalid data is submitted" in {
      mutableFakeDataRetrievalAction.setDataToReturn(userAnswers)

      val result = route(application, httpPOSTRequest(httpPathPOST, valuesInvalid)).value

      status(result) mustEqual BAD_REQUEST

      verify(mockUserAnswersCacheConnector, times(0)).save(any(), any())(any(), any())
    }

    "redirect to Session Expired page for a POST when there is no data" in {
      mutableFakeDataRetrievalAction.setDataToReturn(None)

      val result = route(application, httpPOSTRequest(httpPathPOST, valuesValid)).value

      status(result) mustEqual SEE_OTHER

      redirectLocation(result).value mustBe controllers.routes.IndexController.onPageLoad().url
    }
  }
}