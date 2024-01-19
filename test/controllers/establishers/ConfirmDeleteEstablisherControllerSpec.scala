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

package controllers.establishers

import controllers.ControllerSpecBase
import controllers.actions.MutableFakeDataRetrievalAction
import forms.establishers.ConfirmDeleteEstablisherFormProvider
import identifiers.establishers.ConfirmDeleteEstablisherId
import identifiers.establishers.company.CompanyDetailsId
import identifiers.establishers.individual.EstablisherNameId
import identifiers.establishers.partnership.PartnershipDetailsId
import matchers.JsonMatchers
import models._
import models.establishers.EstablisherKind
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
class ConfirmDeleteEstablisherControllerSpec extends ControllerSpecBase with NunjucksSupport with JsonMatchers with Enumerable.Implicits {
  private val individualName: String = "Jane Doe"
  private val companyName: String = "test company"
  private val partnershipName: String = "test partnership"
  private val index: Index = Index(0)
  private val userAnswersIndividual: Option[UserAnswers] = ua.set(EstablisherNameId(0), PersonName("Jane", "Doe")).toOption
  private val userAnswersCompany: Option[UserAnswers] = ua.set(CompanyDetailsId(0), CompanyDetails(companyName)).toOption
  private val userAnswersPartnership: Option[UserAnswers] = ua.set(PartnershipDetailsId(0), PartnershipDetails(partnershipName)).toOption
  private val templateToBeRendered = "delete.njk"
  private val form: Form[Boolean] = new ConfirmDeleteEstablisherFormProvider()(individualName)

  private val mutableFakeDataRetrievalAction: MutableFakeDataRetrievalAction = new MutableFakeDataRetrievalAction()

  private val application: Application = applicationBuilderMutableRetrievalAction(mutableFakeDataRetrievalAction).build()

  private def httpPathGET(kind: EstablisherKind): String = controllers.establishers.routes.ConfirmDeleteEstablisherController.onPageLoad(index, kind).url
  private def httpPathPOST(kind: EstablisherKind): String = controllers.establishers.routes.ConfirmDeleteEstablisherController.onSubmit(index, kind).url

  private val valuesValid: Map[String, Seq[String]] = Map(
    "value" -> Seq("true")
  )

  private val valuesInvalid: Map[String, Seq[String]] = Map(
    "value" -> Seq.empty
  )

  private def submitUrl(kind: EstablisherKind) = routes.ConfirmDeleteEstablisherController.onSubmit(index, kind).url

  private def jsonToPassToTemplate(kind: EstablisherKind, estName: String): Form[Boolean] => JsObject = form =>
  Json.obj(
    "form" -> form,
    "titleMessage" -> messages("messages__confirmDeleteEstablisher__title"),
    "name" -> estName,
    "radios" -> Radios.yesNo(form("value")),
    "submitUrl" -> submitUrl(kind),
    "schemeName" -> schemeName
  )

  override def beforeEach(): Unit = {
    super.beforeEach()
    when(mockRenderer.render(any(), any())(any())).thenReturn(Future.successful(Html("")))
  }

  private val templateCaptor : ArgumentCaptor[String] = ArgumentCaptor.forClass(classOf[String])
  private val jsonCaptor: ArgumentCaptor[JsObject] = ArgumentCaptor.forClass(classOf[JsObject])

  "ConfirmDeleteEstablisherController" must {

    "return OK and the correct view for a GET when establisher is an individual" in {
      mutableFakeDataRetrievalAction.setDataToReturn(userAnswersIndividual)
      val result = route(application, httpGETRequest(httpPathGET(EstablisherKind.Individual))).value

      status(result) mustEqual OK

      verify(mockRenderer, times(1)).render(templateCaptor.capture(), jsonCaptor.capture())(any())

      templateCaptor.getValue mustEqual templateToBeRendered
      jsonCaptor.getValue must containJson(jsonToPassToTemplate(EstablisherKind.Individual, individualName)(form))
    }

    "return OK and the correct view for a GET when establisher is a company" in {
      val form = new ConfirmDeleteEstablisherFormProvider()(companyName)
      mutableFakeDataRetrievalAction.setDataToReturn(userAnswersCompany)

      val result = route(application, httpGETRequest(httpPathGET(EstablisherKind.Company))).value

      status(result) mustEqual OK

      verify(mockRenderer, times(1)).render(templateCaptor.capture(), jsonCaptor.capture())(any())

      templateCaptor.getValue mustEqual templateToBeRendered
      jsonCaptor.getValue must containJson(jsonToPassToTemplate(EstablisherKind.Company, companyName)(form))
    }

    "return OK and the correct view for a GET when establisher is a partnership" in {
      val form = new ConfirmDeleteEstablisherFormProvider()(partnershipName)
      mutableFakeDataRetrievalAction.setDataToReturn(userAnswersPartnership)

      val result = route(application, httpGETRequest(httpPathGET(EstablisherKind.Partnership))).value

      status(result) mustEqual OK

      verify(mockRenderer, times(1)).render(templateCaptor.capture(), jsonCaptor.capture())(any())

      templateCaptor.getValue mustEqual templateToBeRendered
      jsonCaptor.getValue must containJson(jsonToPassToTemplate(EstablisherKind.Partnership, partnershipName)(form))
    }

    "redirect to Session Expired page for a GET when there is no data" in {
      mutableFakeDataRetrievalAction.setDataToReturn(None)

      val result = route(application, httpGETRequest(httpPathGET(EstablisherKind.Individual))).value

      status(result) mustEqual SEE_OTHER

      redirectLocation(result).value mustBe controllers.preMigration.routes.ListOfSchemesController.onPageLoad(Scheme).url
    }

    "Save data to user answers and redirect to next page when valid data is submitted for Individual" in {
      val expectedJson = Json.obj()

      when(mockCompoundNavigator.nextPage(ArgumentMatchers.eq(ConfirmDeleteEstablisherId), any(), any())(any()))
        .thenReturn(routes.AddEstablisherController.onPageLoad)
      when(mockUserAnswersCacheConnector.save(any(), any())(any(), any()))
        .thenReturn(Future.successful(Json.obj()))

      mutableFakeDataRetrievalAction.setDataToReturn(userAnswersIndividual)

      val result = route(application, httpPOSTRequest(httpPathPOST(EstablisherKind.Individual), valuesValid)).value

      status(result) mustEqual SEE_OTHER

      verify(mockUserAnswersCacheConnector, times(1)).save(any(), jsonCaptor.capture)(any(), any())
      jsonCaptor.getValue must containJson(expectedJson)
      redirectLocation(result) mustBe Some(routes.AddEstablisherController.onPageLoad.url)
    }

    "Save data to user answers and redirect to next page when valid data is submitted for Company" in {
      val expectedJson = Json.obj()

      when(mockCompoundNavigator.nextPage(ArgumentMatchers.eq(ConfirmDeleteEstablisherId), any(), any())(any()))
        .thenReturn(routes.AddEstablisherController.onPageLoad)
      when(mockUserAnswersCacheConnector.save(any(), any())(any(), any()))
        .thenReturn(Future.successful(Json.obj()))

      mutableFakeDataRetrievalAction.setDataToReturn(userAnswersCompany)

      val result = route(application, httpPOSTRequest(httpPathPOST(EstablisherKind.Company), valuesValid)).value

      status(result) mustEqual SEE_OTHER

      verify(mockUserAnswersCacheConnector, times(1)).save(any(), jsonCaptor.capture)(any(), any())
      jsonCaptor.getValue must containJson(expectedJson)
      redirectLocation(result) mustBe Some(routes.AddEstablisherController.onPageLoad.url)
    }

    "Save data to user answers and redirect to next page when valid data is submitted for Partnership" in {
      val expectedJson = Json.obj()

      when(mockCompoundNavigator.nextPage(ArgumentMatchers.eq(ConfirmDeleteEstablisherId), any(), any())(any()))
        .thenReturn(routes.AddEstablisherController.onPageLoad)
      when(mockUserAnswersCacheConnector.save(any(), any())(any(), any()))
        .thenReturn(Future.successful(Json.obj()))

      mutableFakeDataRetrievalAction.setDataToReturn(userAnswersPartnership)

      val result = route(application, httpPOSTRequest(httpPathPOST(EstablisherKind.Partnership), valuesValid)).value

      status(result) mustEqual SEE_OTHER

      verify(mockUserAnswersCacheConnector, times(1)).save(any(), jsonCaptor.capture)(any(), any())
      jsonCaptor.getValue must containJson(expectedJson)
      redirectLocation(result) mustBe Some(routes.AddEstablisherController.onPageLoad.url)
    }

    "return a BAD REQUEST when invalid data is submitted" in {
      mutableFakeDataRetrievalAction.setDataToReturn(userAnswersIndividual)

      val result = route(application, httpPOSTRequest(httpPathPOST(EstablisherKind.Individual), valuesInvalid)).value

      status(result) mustEqual BAD_REQUEST

      verify(mockUserAnswersCacheConnector, times(0)).save(any(), any())(any(), any())
    }

    "redirect back to list of schemes for a POST when there is no data" in {
      mutableFakeDataRetrievalAction.setDataToReturn(None)

      val result = route(application, httpPOSTRequest(httpPathPOST(EstablisherKind.Individual), valuesValid)).value

      status(result) mustEqual SEE_OTHER

      redirectLocation(result).value mustBe controllers.preMigration.routes.ListOfSchemesController.onPageLoad(Scheme).url
    }
  }
}
