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
import forms.establishers.ConfirmDeleteEstablisherFormProvider
import identifiers.establishers.company.OtherDirectorsId
import identifiers.establishers.company.director.{ConfirmDeleteDirectorId, DirectorNameId}
import matchers.JsonMatchers
import models.{Index, NormalMode, PersonName, Scheme}
import org.mockito.ArgumentMatchers.any
import org.mockito.{ArgumentCaptor, ArgumentMatchers}
import org.mockito.Mockito._
import play.api.Application
import play.api.data.Form
import play.api.libs.json.{JsObject, JsValue, Json}
import play.api.test.Helpers._
import utils.Data.{schemeName, ua}
import utils.{Enumerable, UserAnswers}
import views.html.DeleteView

import scala.concurrent.Future

class ConfirmDeleteDirectorControllerSpec extends ControllerSpecBase with JsonMatchers with Enumerable.Implicits {

  private val mutableFakeDataRetrievalAction: MutableFakeDataRetrievalAction = new MutableFakeDataRetrievalAction()
  override def fakeApplication(): Application = applicationBuilderMutableRetrievalAction(mutableFakeDataRetrievalAction).build()

  private val directorName: String = "Jane Doe"
  private val establisherIndex: Index = Index(0)
  private val dirIndex: Index = Index(0)
  private val userAnswersDirector: Option[UserAnswers] = ua
    .set(DirectorNameId(establisherIndex, dirIndex), PersonName("Jane", "Doe")).toOption

  private val form: Form[Boolean] = new ConfirmDeleteEstablisherFormProvider()(directorName)

  private def httpPathGET(directorIndex: Index): String =
    controllers.establishers.company.director.routes.ConfirmDeleteDirectorController.onPageLoad(establisherIndex,directorIndex).url
  private def httpPathPOST(directorIndex: Index): String =
    controllers.establishers.company.director.routes.ConfirmDeleteDirectorController.onSubmit(establisherIndex,directorIndex).url

  private val valuesValid: Map[String, Seq[String]] = Map(
    "value" -> Seq("true")
  )

  private val valuesInvalid: Map[String, Seq[String]] = Map(
    "value" -> Seq.empty
  )

  private def submitUrl(directorIndex:Index) = routes.ConfirmDeleteDirectorController.onSubmit(establisherIndex, directorIndex)

  override def beforeEach(): Unit = {
    super.beforeEach()
  }

  "ConfirmDeleteDirectorController" must {

    "return OK and the correct view for a GET director" in {
      mutableFakeDataRetrievalAction.setDataToReturn(userAnswersDirector)
      val request = httpGETRequest(httpPathGET(dirIndex))

      val result = route(app, request).value

      status(result) mustEqual OK

      val deleteView = app.injector.instanceOf[DeleteView].apply(
        form,
        messages("messages__confirmDeleteDirectors__title"),
        directorName,
        None,
        utils.Radios.yesNo(form("value")),
        schemeName,
        submitUrl(dirIndex)
      )(request, messages)
      compareResultAndView(result, deleteView)
    }

    "Save data to user answers and redirect to next page when valid data is submitted for director" in {

      when(mockCompoundNavigator.nextPage(ArgumentMatchers.eq(ConfirmDeleteDirectorId(dirIndex)), any(), any())(any()))
        .thenReturn(controllers.establishers.company.routes.AddCompanyDirectorsController.onPageLoad(establisherIndex,NormalMode))
      when(mockUserAnswersCacheConnector.save(any(), any())(any(), any()))
        .thenReturn(Future.successful(Json.obj()))

      val userAnswers = userAnswersDirector.map(_.setOrException(OtherDirectorsId(establisherIndex), true))

      mutableFakeDataRetrievalAction.setDataToReturn(userAnswers)

      val result = route(app, httpPOSTRequest(httpPathPOST(dirIndex), valuesValid)).value

      status(result) mustEqual SEE_OTHER
      verify(mockUserAnswersCacheConnector, times(1)).save(any(), any())(any(), any())
      redirectLocation(result) mustBe Some(controllers.establishers.company.routes.AddCompanyDirectorsController.onPageLoad(establisherIndex,NormalMode).url)

      val jsonCaptorUA = ArgumentCaptor.forClass(classOf[JsValue])
      verify(mockUserAnswersCacheConnector, times(1)).save(any(), jsonCaptorUA.capture())(any(), any())
      val actualUA = {
        val jsValue:JsValue = jsonCaptorUA.getValue
        UserAnswers(jsValue.as[JsObject])
      }
      actualUA.get(OtherDirectorsId(establisherIndex)) mustBe None
    }


    "return a BAD REQUEST when invalid data is submitted" in {
      mutableFakeDataRetrievalAction.setDataToReturn(userAnswersDirector)

      val result = route(app, httpPOSTRequest(httpPathPOST(dirIndex), valuesInvalid)).value

      status(result) mustEqual BAD_REQUEST
      contentAsString(result) must include(messages("messages__confirmDeleteDirectors__title"))
      contentAsString(result) must include(messages("messages__confirmDelete__error_required", directorName))

      verify(mockUserAnswersCacheConnector, times(0)).save(any(), any())(any(), any())
    }

    "redirect back to list of schemes for a POST when there is no data" in {
      mutableFakeDataRetrievalAction.setDataToReturn(None)

      val result = route(app, httpPOSTRequest(httpPathPOST(dirIndex), valuesValid)).value

      status(result) mustEqual SEE_OTHER

      redirectLocation(result).value mustBe controllers.preMigration.routes.ListOfSchemesController.onPageLoad(Scheme).url
    }
  }
}
