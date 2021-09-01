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

package controllers.establishers.company.director

import controllers.ControllerSpecBase
import controllers.actions.MutableFakeDataRetrievalAction
import forms.establishers.ConfirmDeleteEstablisherFormProvider
import identifiers.establishers.company.director.{ConfirmDeleteDirectorId, DirectorNameId}
import matchers.JsonMatchers
import models.{Index, NormalMode, PersonName}
import org.mockito.ArgumentMatchers.any
import org.mockito.{ArgumentCaptor, ArgumentMatchers}
import play.api.Application
import play.api.data.Form
import play.api.libs.json.{JsObject, Json}
import play.api.test.Helpers._
import play.twirl.api.Html
import uk.gov.hmrc.nunjucks.NunjucksSupport
import uk.gov.hmrc.viewmodels.Radios
import utils.Data.{schemeName, ua}
import utils.{Enumerable, UserAnswers}

import scala.concurrent.Future

class ConfirmDeleteDirectorControllerSpec extends ControllerSpecBase with NunjucksSupport with JsonMatchers with Enumerable.Implicits {
  private val directorName: String = "Jane Doe"
  private val establisherIndex: Index = Index(0)
  private val dirIndex: Index = Index(0)
  private val userAnswersDirector: Option[UserAnswers] = ua.set(DirectorNameId(establisherIndex,dirIndex), PersonName("Jane", "Doe")).toOption
  private val templateToBeRendered = "delete.njk"
  private val form: Form[Boolean] = new ConfirmDeleteEstablisherFormProvider()(directorName)

  private val mutableFakeDataRetrievalAction: MutableFakeDataRetrievalAction = new MutableFakeDataRetrievalAction()

  private val application: Application = applicationBuilderMutableRetrievalAction(mutableFakeDataRetrievalAction).build()

  private def httpPathGET(directorIndex: Index): String = controllers.establishers.company.director.routes.ConfirmDeleteDirectorController.onPageLoad(establisherIndex,directorIndex).url
  private def httpPathPOST(directorIndex: Index): String =controllers.establishers.company.director.routes.ConfirmDeleteDirectorController.onSubmit(establisherIndex,directorIndex).url

  private val valuesValid: Map[String, Seq[String]] = Map(
    "value" -> Seq("true")
  )

  private val valuesInvalid: Map[String, Seq[String]] = Map(
    "value" -> Seq.empty
  )

  private def submitUrl(directorIndex:Index) = routes.ConfirmDeleteDirectorController.onSubmit(establisherIndex, directorIndex).url

  private def jsonToPassToTemplate(directorIndex:Index, directorName: String): Form[Boolean] => JsObject = form =>
  Json.obj(
    "form" -> form,
    "titleMessage" -> messages("messages__confirmDeleteDirectors__title"),
    "name" -> directorName,
    "radios" -> Radios.yesNo(form("value")),
    "submitUrl" -> submitUrl(directorIndex),
    "schemeName" -> schemeName
  )

  override def beforeEach: Unit = {
    super.beforeEach
    when(mockRenderer.render(any(), any())(any())).thenReturn(Future.successful(Html("")))
  }

  private val templateCaptor = ArgumentCaptor.forClass(classOf[String])
  private val jsonCaptor = ArgumentCaptor.forClass(classOf[JsObject])

  "ConfirmDeleteDirectorController" must {

    "return OK and the correct view for a GET director" in {
      mutableFakeDataRetrievalAction.setDataToReturn(userAnswersDirector)
      val result = route(application, httpGETRequest(httpPathGET(dirIndex))).value

      status(result) mustEqual OK

      verify(mockRenderer, times(1)).render(templateCaptor.capture(), jsonCaptor.capture())(any())

      templateCaptor.getValue mustEqual templateToBeRendered
      jsonCaptor.getValue must containJson(jsonToPassToTemplate(dirIndex, directorName)(form))
    }

    "Save data to user answers and redirect to next page when valid data is submitted for director" in {
      val expectedJson = Json.obj()

      when(mockCompoundNavigator.nextPage(ArgumentMatchers.eq(ConfirmDeleteDirectorId(dirIndex)), any(), any())(any()))
        .thenReturn(controllers.establishers.company.routes.AddCompanyDirectorsController.onPageLoad(establisherIndex,NormalMode))
      when(mockUserAnswersCacheConnector.save(any(), any())(any(), any()))
        .thenReturn(Future.successful(Json.obj()))

      mutableFakeDataRetrievalAction.setDataToReturn(userAnswersDirector)

      val result = route(application, httpPOSTRequest(httpPathPOST(dirIndex), valuesValid)).value

      status(result) mustEqual SEE_OTHER
      verify(mockUserAnswersCacheConnector, times(1)).save(any(), jsonCaptor.capture)(any(), any())
      jsonCaptor.getValue must containJson(expectedJson)
      redirectLocation(result) mustBe Some(controllers.establishers.company.routes.AddCompanyDirectorsController.onPageLoad(establisherIndex,NormalMode).url)
    }


    "return a BAD REQUEST when invalid data is submitted" in {
      mutableFakeDataRetrievalAction.setDataToReturn(userAnswersDirector)

      val result = route(application, httpPOSTRequest(httpPathPOST(dirIndex), valuesInvalid)).value

      status(result) mustEqual BAD_REQUEST

      verify(mockUserAnswersCacheConnector, times(0)).save(any(), any())(any(), any())
    }

    "redirect to Session Expired page for a POST when there is no data" in {
      mutableFakeDataRetrievalAction.setDataToReturn(None)

      val result = route(application, httpPOSTRequest(httpPathPOST(dirIndex), valuesValid)).value

      status(result) mustEqual SEE_OTHER

      redirectLocation(result).value mustBe controllers.routes.IndexController.onPageLoad().url
    }
  }
}
