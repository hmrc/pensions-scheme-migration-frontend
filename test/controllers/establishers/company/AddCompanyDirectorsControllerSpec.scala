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

package controllers.establishers.company

import controllers.ControllerSpecBase
import controllers.actions.MutableFakeDataRetrievalAction
import forms.establishers.company.director.AddCompanyDirectorsFormProvider
import helpers.AddToListHelper
import identifiers.establishers.EstablisherKindId
import identifiers.establishers.company.director.{DirectorNameId, IsNewDirectorId}
import identifiers.establishers.company.{AddCompanyDirectorsId, CompanyDetailsId}
import matchers.JsonMatchers
import models.establishers.EstablisherKind
import models.{CompanyDetails, NormalMode, PersonName}
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
import uk.gov.hmrc.nunjucks.NunjucksSupport
import uk.gov.hmrc.viewmodels.{Radios, Table}
import utils.Data.{schemeName, ua}
import utils.{Enumerable, UserAnswers}

import scala.concurrent.Future

class AddCompanyDirectorsControllerSpec extends ControllerSpecBase with NunjucksSupport with JsonMatchers with Enumerable.Implicits {
  private val directorName: PersonName =
    PersonName("Jane", "Doe")
  private val userAnswers: Option[UserAnswers] =
   ua.set(EstablisherKindId(0), EstablisherKind.Company).flatMap(
     _.set(CompanyDetailsId(0), CompanyDetails("ABC")).flatMap(
        _.set(DirectorNameId(0,0), directorName).flatMap(
          _.set(IsNewDirectorId(0,0), true)
     ))).toOption

  private val templateToBeRendered = "establishers/company/addDirector.njk"

  //private val form: Form[Boolean] = new ConfirmDeleteDirectorFormProvider()(directorName.fullName)
  private val formProvider = new AddCompanyDirectorsFormProvider()
  private val form         = formProvider()
  val table: Table = Table(head = Nil, rows = Nil)
  private val mockHelper: AddToListHelper = mock[AddToListHelper]

  private val mutableFakeDataRetrievalAction: MutableFakeDataRetrievalAction = new MutableFakeDataRetrievalAction()
  val extraModules: Seq[GuiceableModule] = Seq(
    bind[AddToListHelper].toInstance(mockHelper)
  )
  private val application: Application = applicationBuilderMutableRetrievalAction(mutableFakeDataRetrievalAction, extraModules).build()

  private def httpPathGET: String = controllers.establishers.company.routes.AddCompanyDirectorsController.onPageLoad(0,NormalMode).url
  private def httpPathPOST: String = controllers.establishers.company.routes.AddCompanyDirectorsController.onSubmit(0,NormalMode).url

  private val maxDirectors = appConfig.maxDirectors
  private val valuesValid: Map[String, Seq[String]] = Map(
    "value" -> Seq("true")
  )

  private val valuesInvalid: Map[String, Seq[String]] = Map(
    "value" -> Seq("invalid")
  )

  private val jsonToPassToTemplate: Form[Boolean] => JsObject = form =>
    Json.obj(
      "form" -> form,
      "table" -> table,
      "radios" -> Radios.yesNo(form("value")),
      "schemeName" -> schemeName,
      "directorSize" -> 1,
      "maxDirectors" -> 10
    )

  override def beforeEach: Unit = {
    super.beforeEach
    when(mockRenderer.render(any(), any())(any())).thenReturn(Future.successful(Html("")))
    when(mockHelper.mapDirectorToTable(any())(any())).thenReturn(table)
  }


  "AddCompanyDirectorsController" must {

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

    "redirect to Session Expired page for a GET when there is no data" in {
      mutableFakeDataRetrievalAction.setDataToReturn(None)

      val result = route(application, httpGETRequest(httpPathGET)).value

      status(result) mustEqual SEE_OTHER

      redirectLocation(result).value mustBe controllers.routes.IndexController.onPageLoad().url
    }

    "Save data to user answers and redirect to next page when valid data is submitted" in {

      when(mockCompoundNavigator.nextPage(Matchers.eq(AddCompanyDirectorsId(0)), any(), any())(any()))
        .thenReturn(routes.AddCompanyDirectorsController.onPageLoad(0,NormalMode))

      mutableFakeDataRetrievalAction.setDataToReturn(userAnswers)

      val result = route(application, httpPOSTRequest(httpPathPOST, valuesValid)).value

      status(result) mustEqual SEE_OTHER

      redirectLocation(result) mustBe Some(routes.AddCompanyDirectorsController.onPageLoad(0,NormalMode).url)
    }

    "return a BAD REQUEST when invalid data is submitted" in {

      mutableFakeDataRetrievalAction.setDataToReturn(userAnswers)
      val boundForm = form.bind(Map("value" -> "invalid value"))
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
