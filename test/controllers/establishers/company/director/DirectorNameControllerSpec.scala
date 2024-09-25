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

package controllers.establishers.company.director

import controllers.ControllerSpecBase
import controllers.actions.MutableFakeDataRetrievalAction
import forms.PersonNameFormProvider
import identifiers.establishers.company.director.DirectorNameId
import matchers.JsonMatchers
import models.{Index, NormalMode, PersonName, Scheme}
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import play.api.Application
import play.api.data.Form
import play.api.i18n.Messages
import play.api.libs.json.Json
import play.api.test.Helpers._
import uk.gov.hmrc.nunjucks.NunjucksSupport
import utils.Data.{schemeName, ua}
import utils.{Enumerable, UserAnswers}
import views.html.PersonNameView

import scala.concurrent.Future

class DirectorNameControllerSpec extends ControllerSpecBase with NunjucksSupport with JsonMatchers with Enumerable.Implicits {

  private val index: Index = Index(0)
  private val directorIndex: Index = Index(0)
  private val personName: PersonName = PersonName("Jane", "Doe")
  private val userAnswers: Option[UserAnswers] = ua.set(DirectorNameId(0,0), personName).toOption
  private val form: Form[PersonName] = new PersonNameFormProvider()("messages__error__director")

  private val mutableFakeDataRetrievalAction: MutableFakeDataRetrievalAction = new MutableFakeDataRetrievalAction()

  private val application: Application = applicationBuilderMutableRetrievalAction(mutableFakeDataRetrievalAction).build()

  private def httpPathGET: String = controllers.establishers.company.director.routes.DirectorNameController.onPageLoad(index,directorIndex,NormalMode).url
  private def httpPathPOST: String = controllers.establishers.company.director.routes.DirectorNameController.onSubmit(index,directorIndex,NormalMode).url

  private val valuesValid: Map[String, Seq[String]] = Map(
    "firstName" -> Seq("Jane"),
    "lastName" -> Seq("Doe")
  )

  private val valuesInvalid: Map[String, Seq[String]] = Map(
    "value" -> Seq.empty
  )

  override def beforeEach(): Unit = {
    super.beforeEach()
  }


  "DirectorNameController" must {

    "return OK and the correct view for a GET" in {
      mutableFakeDataRetrievalAction.setDataToReturn(Some(ua))
      val request = httpGETRequest(httpPathGET)
      val result = route(application, request).value

      status(result) mustEqual OK
      val view = application.injector.instanceOf[PersonNameView].apply(
        form,
        schemeName,
        Messages("messages__director"),
        routes.DirectorNameController.onSubmit(index,directorIndex,NormalMode)
      )(request, messages)
      compareResultAndView(result, view)
    }

    "return OK and the correct view for a GET when the question has previously been answered" in {
      mutableFakeDataRetrievalAction.setDataToReturn(userAnswers)

      val result = route(application, httpGETRequest(httpPathGET)).value
      status(result) mustEqual OK

      contentAsString(result) must include(messages("messages__name_title", "the director"))
      contentAsString(result) must include(personName.firstName)
      contentAsString(result) must include(personName.lastName)
    }

    "redirect to Session Expired page for a GET when there is no data" in {
      mutableFakeDataRetrievalAction.setDataToReturn(None)

      val result = route(application, httpGETRequest(httpPathGET)).value

      status(result) mustEqual SEE_OTHER

      redirectLocation(result).value mustBe controllers.preMigration.routes.ListOfSchemesController.onPageLoad(Scheme).url
    }

    "Save data to user answers and redirect to next page when valid data is submitted" in {
      when(mockCompoundNavigator.nextPage(ArgumentMatchers.eq(DirectorNameId(index,directorIndex)), any(), any())(any()))
        .thenReturn(controllers.establishers.company.director.details.routes.DirectorDOBController.onPageLoad(index,directorIndex,NormalMode))
      when(mockUserAnswersCacheConnector.save(any(), any())(any(), any()))
        .thenReturn(Future.successful(Json.obj()))

      mutableFakeDataRetrievalAction.setDataToReturn(userAnswers)

      val result = route(application, httpPOSTRequest(httpPathPOST, valuesValid)).value

      status(result) mustEqual SEE_OTHER

      verify(mockUserAnswersCacheConnector, times(1)).save(any(), any())(any(), any())
      redirectLocation(result) mustBe Some(controllers.establishers.company.director.details.routes.DirectorDOBController.onPageLoad(index,directorIndex,NormalMode).url)
    }

    "return a BAD REQUEST when invalid data is submitted" in {
      mutableFakeDataRetrievalAction.setDataToReturn(userAnswers)

      val result = route(application, httpPOSTRequest(httpPathPOST, valuesInvalid)).value

      status(result) mustEqual BAD_REQUEST

      contentAsString(result) must include(messages("messages__name_title", "the director"))
      contentAsString(result) must include(messages("messages__error__first_name", "director’s"))
      contentAsString(result) must include(messages("messages__error__last_name", "director’s"))
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
