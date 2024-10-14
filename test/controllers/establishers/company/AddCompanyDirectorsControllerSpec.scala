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
import forms.establishers.company.director.AddCompanyDirectorsFormProvider
import identifiers.establishers.EstablisherKindId
import identifiers.establishers.company.director.{DirectorNameId, IsNewDirectorId}
import identifiers.establishers.company.{AddCompanyDirectorsId, CompanyDetailsId}
import matchers.JsonMatchers
import models._
import models.establishers.EstablisherKind
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import play.api.Application
import play.api.test.Helpers._
import uk.gov.hmrc.viewmodels.Radios
import utils.Data.{companyDetails, schemeName, ua}
import utils.{Enumerable, TwirlMigration, UserAnswers}
import views.html.establishers.company.AddDirectorView

class AddCompanyDirectorsControllerSpec extends ControllerSpecBase with JsonMatchers with Enumerable.Implicits {
  private val directorName: PersonName =
    PersonName("Jane", "Doe")
  private val userAnswers: Option[UserAnswers] =
   ua.set(EstablisherKindId(0), EstablisherKind.Company).flatMap(
     _.set(CompanyDetailsId(0), CompanyDetails(companyDetails.companyName)).flatMap(
        _.set(DirectorNameId(0,0), directorName).flatMap(
          _.set(IsNewDirectorId(0,0), true)
     ))).toOption

  private def validData() = {
    ua.set(EstablisherKindId(1), EstablisherKind.Company).flatMap(
      _.set(CompanyDetailsId(1), CompanyDetails(companyDetails.companyName)).flatMap(
        _.set(DirectorNameId(1,1), directorName).flatMap(
        _.set(DirectorNameId(1,2), directorName).flatMap(
        _.set(DirectorNameId(1,3), directorName).flatMap(
        _.set(DirectorNameId(1,4), directorName).flatMap(
        _.set(DirectorNameId(1,5), directorName).flatMap(
        _.set(DirectorNameId(1,6), directorName).flatMap(
        _.set(DirectorNameId(1,7), directorName).flatMap(
        _.set(DirectorNameId(1,8), directorName).flatMap(
        _.set(DirectorNameId(1,9), directorName).flatMap(
          _.set(DirectorNameId(1,10), directorName).flatMap(
          _.set(IsNewDirectorId(1,1), false)
        )))))))))))).toOption
  }

  private val formProvider = new AddCompanyDirectorsFormProvider()
  private val form         = formProvider()
  private val mutableFakeDataRetrievalAction: MutableFakeDataRetrievalAction = new MutableFakeDataRetrievalAction()

  private val application: Application = applicationBuilderMutableRetrievalAction(mutableFakeDataRetrievalAction).build()

  private def httpPathGET: String = controllers.establishers.company.routes.AddCompanyDirectorsController.onPageLoad(0,NormalMode).url
  private def httpPathPOST: String = controllers.establishers.company.routes.AddCompanyDirectorsController.onSubmit(0,NormalMode).url

  private val valuesValid: Map[String, Seq[String]] = Map(
    "value" -> Seq("true")
  )

  private val valuesInvalid: Map[String, Seq[String]] = Map(
    "value" -> Seq("invalid")
  )

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockAppConfig)
    when(mockAppConfig.maxDirectors).thenReturn(10)
  }


  "AddCompanyDirectorsController" must {

    "return OK and the correct view for a GET" in {
      mutableFakeDataRetrievalAction.setDataToReturn(userAnswers)

      val request = httpGETRequest(httpPathGET)
      val result = route(application, request).value

      status(result) mustEqual OK

      val view = application.injector.instanceOf[AddDirectorView].apply(
        form,
        schemeName,
        1,
        mockAppConfig.maxDirectors,
        Seq(DirectorEntity(DirectorNameId(0,0), directorName.fullName, false, false, true, 1)),
        utils.Radios.yesNo(form("value")),
        controllers.establishers.company.routes.AddCompanyDirectorsController.onSubmit(0,NormalMode)
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

      when(mockCompoundNavigator.nextPage(ArgumentMatchers.eq(AddCompanyDirectorsId(0)), any(), any())(any()))
        .thenReturn(routes.AddCompanyDirectorsController.onPageLoad(0,NormalMode))

      mutableFakeDataRetrievalAction.setDataToReturn(userAnswers)

      val result = route(application, httpPOSTRequest(httpPathPOST, valuesValid)).value

      status(result) mustEqual SEE_OTHER

      redirectLocation(result) mustBe Some(routes.AddCompanyDirectorsController.onPageLoad(0,NormalMode).url)
    }

    "redirect to the next page when maximum directors exist and the user submits" in {
      mutableFakeDataRetrievalAction.setDataToReturn(validData())
      val result = route(application, httpPOSTRequest(httpPathPOST, valuesValid)).value

      status(result) mustEqual SEE_OTHER

      redirectLocation(result).value mustBe controllers.preMigration.routes.ListOfSchemesController.onPageLoad(Scheme).url
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
