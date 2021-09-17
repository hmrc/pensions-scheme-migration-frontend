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

package services

import base.SpecBase
import config.AppConfig
import connectors.cache.FeatureToggleConnector
import connectors.{ListOfSchemesConnector, MinimalDetailsConnector}
import controllers.preMigration.routes
import forms.ListSchemesFormProvider
import matchers.JsonMatchers
import models.MigrationType.isRacDac
import models.requests.AuthenticatedRequest
import models.{Items, ListOfLegacySchemes, MigrationType, MinPSA, Scheme}
import org.mockito.ArgumentMatchers.any
import org.mockito.{ArgumentCaptor, ArgumentMatchers, MockitoSugar}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import play.api.data.Form
import play.api.i18n.Messages
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.AnyContent
import play.api.test.Helpers.{defaultAwaitTimeout, redirectLocation, status}
import renderer.Renderer
import uk.gov.hmrc.domain.PsaId
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.nunjucks.{NunjucksRenderer, NunjucksSupport}
import uk.gov.hmrc.viewmodels.Table.Cell
import uk.gov.hmrc.viewmodels.Text.Literal
import uk.gov.hmrc.viewmodels.{MessageInterpolators, Table}
import utils.Data._
import utils.SchemeFuzzyMatcher

import scala.concurrent.{ExecutionContext, Future}
import play.api.http.Status._

class SchemeSearchServiceSpec extends SpecBase with BeforeAndAfterEach with MockitoSugar with ScalaFutures with NunjucksSupport with JsonMatchers {

  import SchemeSearchServiceSpec._

  private val templateToBeRendered: String = "preMigration/listOfSchemes.njk"
  private val mockAppConfig = mock[AppConfig]
  private val mockFuzzyMatching = mock[SchemeFuzzyMatcher]
  private val mockListOfSchemesConnector = mock[ListOfSchemesConnector]
  private val mockMinimalDetailsConnector: MinimalDetailsConnector = mock[MinimalDetailsConnector]
  private val paginationService = new PaginationService
  private val mockFeatureToggleConnector: FeatureToggleConnector = mock[FeatureToggleConnector]
  private val mockRenderer: NunjucksRenderer = mock[NunjucksRenderer]
  private val typeOfList:List[String]=List("pension scheme","RAC/DAC")
  private val psaName: String = "Nigel"
  private val pagination: Int = 10
  private def minimalPSA(rlsFlag: Boolean = false, deceasedFlag: Boolean = false) =
    MinPSA("", isPsaSuspended = false, Some(psaName), None, rlsFlag = rlsFlag, deceasedFlag = deceasedFlag)
  private val formProvider: ListSchemesFormProvider = new ListSchemesFormProvider()
  private def form(implicit messages: Messages): Form[String] = formProvider(messages("messages__listSchemes__search_required", "pension scheme"))
  private val dummyUrl="dummyurl"
  implicit val request: AuthenticatedRequest[AnyContent] = AuthenticatedRequest(fakeRequest, "", PsaId(psaId))

  private def schemeJson(numberOfSchemes:Int, pagination:Int, pageNumber:Int, numberOfPages:Int, migrationType:MigrationType,
                         noResultsMessageKey:Option[String], paginationText:String, typeOfList:String,
                         schemeTable:Option[Table], form:Form[_]=form): JsObject = Json.obj(

    "form" -> form,
    "psaName" -> psaName,
    "numberOfSchemes" -> numberOfSchemes,
    "pagination" -> pagination,
    "pageNumber" -> pageNumber,
    "pageNumberLinks" -> paginationService.pageNumberLinks(
      pageNumber,
      numberOfSchemes,
      pagination,
      numberOfPages
    ),
    "racDac" -> isRacDac(migrationType),
    "numberOfPages" -> numberOfPages,
    "noResultsMessageKey" -> noResultsMessageKey,
    "clearLinkUrl" -> routes.ListOfSchemesController.onPageLoad(migrationType).url,
    "returnUrl" -> dummyUrl,
    "paginationText" -> paginationText,
    "typeOfList" -> typeOfList,
  ) ++  (if (schemeTable.isDefined) Json.obj("schemes" -> schemeTable.get) else Json.obj())

  val service = new SchemeSearchService(mockAppConfig, mockFuzzyMatching, mockListOfSchemesConnector,
    mockMinimalDetailsConnector, paginationService, mockFeatureToggleConnector, new Renderer(mockAppConfig, mockRenderer))

  override def beforeEach: Unit = {
    super.beforeEach
    when(mockAppConfig.psaOverviewUrl) thenReturn dummyUrl
    when(mockAppConfig.psaUpdateContactDetailsUrl).thenReturn(dummyUrl)
    when(mockAppConfig.deceasedContactHmrcUrl).thenReturn(dummyUrl)
    when(mockFeatureToggleConnector.get(any())(any(), any()))
  }

  "search" must {

    "return correct list of scheme details with search on correct pstr" in {

      when(mockListOfSchemesConnector.getListOfSchemes(ArgumentMatchers.eq(psaId))(any(), any()))
        .thenReturn(Future.successful(Right(listOfSchemes)))

      whenReady(service.search(psaId, Some(pstr1), isRacDac = isRacDacFalse)) { result =>
        result mustBe fullSchemes.filter(_.racDac == isRacDacFalse).filter(_.pstr equalsIgnoreCase pstr1)
      }
    }
    "return correct list of scheme details with search on correct pstr with rac dac" in {

      when(mockListOfSchemesConnector.getListOfSchemes(ArgumentMatchers.eq(psaId))(any(), any()))
        .thenReturn(Future.successful(Right(listOfSchemes)))

      whenReady(service.search(psaId, Some(pstr2), isRacDac = isRacDacTrue)) { result =>
        result mustBe fullSchemes.filter(_.racDac == isRacDacTrue).filter(_.pstr equalsIgnoreCase pstr2)
      }
    }

    "return empty list for correct format pstr but no match" in {
      val emptyList = ListOfLegacySchemes(0, None)
      when(mockListOfSchemesConnector.getListOfSchemes(ArgumentMatchers.eq(psaId))(any(), any()))
        .thenReturn(Future.successful(Right(emptyList)))

      whenReady(service.search(psaId, Some("S2400000016"), isRacDac = isRacDacFalse)) { result =>
        result mustBe Nil
      }
    }

    "return empty list for correct format pstr but no match with rac dac" in {
      val emptyList = ListOfLegacySchemes(0, None)
      when(mockListOfSchemesConnector.getListOfSchemes(ArgumentMatchers.eq(psaId))(any(), any()))
        .thenReturn(Future.successful(Right(emptyList)))

      whenReady(service.search(psaId, Some("S2400000016"), isRacDac = isRacDacTrue)) { result =>
        result mustBe Nil
      }
    }

    "return correct list of scheme details with search on scheme name" in {
      when(mockFuzzyMatching.doFuzzyMatching(any(), any())).thenReturn(true)
      when(mockListOfSchemesConnector.getListOfSchemes(ArgumentMatchers.eq(psaId))(any(), any()))
        .thenReturn(Future.successful(Right(listOfSchemes)))

      whenReady(service.search(psaId, Some("scheme-1"),isRacDac = isRacDacFalse)) { result =>
        result mustBe fullSchemes.filter(_.racDac == isRacDacFalse).filter(_.schemeName == "scheme-1")
      }
    }

    "return correct list of scheme details with search on scheme name with rac dac" in {
      when(mockFuzzyMatching.doFuzzyMatching(any(), any())).thenReturn(true)
      when(mockListOfSchemesConnector.getListOfSchemes(ArgumentMatchers.eq(psaId))(any(), any()))
        .thenReturn(Future.successful(Right(listOfSchemes)))

      whenReady(service.search(psaId, Some("scheme-1"),isRacDac = isRacDacTrue)) { result =>
        result mustBe fullSchemes.filter(_.racDac == isRacDacTrue).filter(_.schemeName == "scheme-2")
      }
    }

    "return empty list when fuzzy matching fails" in {
      when(mockFuzzyMatching.doFuzzyMatching(any(), any())).thenReturn(false)
      val emptyList = ListOfLegacySchemes(0, None)
      when(mockListOfSchemesConnector.getListOfSchemes(ArgumentMatchers.eq(psaId))(any(), any()))
        .thenReturn(Future.successful(Right(emptyList)))

      whenReady(service.search(psaId, Some("no matching"),isRacDac = isRacDacFalse)) { result =>
        result mustBe Nil
      }
    }

  }

  "mapToTable" must {

    "return correct table of scheme details" in {
      val head=Seq(
        Cell(msg"messages__listSchemes__column_schemeName"),
        Cell(msg"messages__listSchemes__column_pstr"),
        Cell(msg"messages__listSchemes__column_regDate")
      )

      val rows= List(Seq(
        Cell(Literal("scheme-1"), Seq("govuk-!-width-one-quarter")),
        Cell(Literal(pstr1), Seq("govuk-!-width-one-quarter")),
        Cell(Literal("12 December 1989"), Seq("govuk-!-width-one-half"))))

      service.mapToTable(List(fullSchemes.head), isRacDac = isRacDacFalse, viewOnly = true)  mustBe
        Table(head, rows,  attributes = Map("role" -> "table"))

    }
    "return correct table of scheme details with rac dac" in {
      val head=Seq(
        Cell(msg"messages__listSchemes__column_racDacName"),
        Cell(msg"messages__listSchemes__column_pstr")
      )

      val rows= List(Seq(
        Cell(Literal("scheme-2"), Seq("govuk-!-width-one-quarter")),
        Cell(Literal(pstr2), Seq("govuk-!-width-one-quarter"))))

      val table =Table(head, rows,  attributes = Map("role" -> "table"))

      service.mapToTable(fullSchemes.tail, isRacDac = isRacDacTrue, viewOnly = true)  mustBe table

    }

  }

  "searchAndRenderView" must {
    "return OK and the correct view when there are no schemes" in {
      when(mockMinimalDetailsConnector.getPSADetails(any())(any(), any())).thenReturn(Future.successful(minimalPSA()))
      when(mockListOfSchemesConnector.getListOfSchemes(any())(any(), any())).thenReturn(Future.successful(Right(emptySchemes)))
      val numberOfPages = paginationService.divide(0, pagination)

      val templateCaptor : ArgumentCaptor[String] = ArgumentCaptor.forClass(classOf[String])
      val jsonCaptor: ArgumentCaptor[JsObject] = ArgumentCaptor.forClass(classOf[JsObject])

      val result = service.searchAndRenderView(form, 1, None, Scheme)

      status(result) mustBe OK

      verify(mockRenderer, times(1)).render(templateCaptor.capture(), jsonCaptor.capture())(any())

      templateCaptor.getValue mustEqual templateToBeRendered

      jsonCaptor.getValue must containJson(schemeJson(
        0,pagination,pageNumber = 1,numberOfPages,Scheme, Some(messages("messages__listSchemes__noSchemes",typeOfList.head)),
        "Showing 1 to 10 of 0 schemes",typeOfList.head,None))

    }

    "rlsFlag is true and deceasedFlag is false return redirect to update contact page" in {
      when(mockMinimalDetailsConnector.getPSADetails(any())(any(), any())).thenReturn(Future.successful(minimalPSA(rlsFlag = true)))

      val result = service.searchAndRenderView(form, 1, None, Scheme)

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(dummyUrl)
    }

    "rlsFlag is true and deceasedFlag is true return redirect to contact hmrc page" in {
      when(mockMinimalDetailsConnector.getPSADetails(any())(any(), any())).thenReturn(Future.successful(minimalPSA(rlsFlag = true,deceasedFlag = true)))

      val result = service.searchAndRenderView(form, 1, None, Scheme)

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(dummyUrl)
    }

    "return OK and the correct view when there are schemes without pagination" in {
      when(mockMinimalDetailsConnector.getPSADetails(any())(any(), any())).thenReturn(Future.successful(minimalPSA()))
      when(mockListOfSchemesConnector.getListOfSchemes(any())(any(), any())).thenReturn(Future.successful(Right(listOfSchemes)))

      val numberOfPages = paginationService.divide(fullSchemes.length, pagination)

      val templateCaptor : ArgumentCaptor[String] = ArgumentCaptor.forClass(classOf[String])
      val jsonCaptor: ArgumentCaptor[JsObject] = ArgumentCaptor.forClass(classOf[JsObject])

      val result = service.searchAndRenderView(form, 1, None, Scheme)

      status(result) mustBe OK
      verify(mockRenderer, times(1)).render(templateCaptor.capture(), jsonCaptor.capture())(any())

      jsonCaptor.getValue must containJson(schemeJson(
        fullSchemes.length,pagination,pageNumber = 1,numberOfPages,Scheme, None,
        "Showing 1 to 2 of 2 schemes", typeOfList.head, Some(table)))
    }

    "return OK and the correct view when there are schemes with pagination" in {
      when(mockMinimalDetailsConnector.getPSADetails(any())(any(), any())).thenReturn(Future.successful(minimalPSA()))
      when(mockListOfSchemesConnector.getListOfSchemes(any())(any(), any())).thenReturn(Future.successful(Right(listOfSchemes)))

      val pageNumber: Int = 1
      val pagination: Int = 1
      val numberOfSchemes: Int = fullSchemes.length

      val numberOfPages = paginationService.divide(numberOfSchemes, pagination)

      when(mockAppConfig.listSchemePagination) thenReturn pagination

      val templateCaptor : ArgumentCaptor[String] = ArgumentCaptor.forClass(classOf[String])
      val jsonCaptor: ArgumentCaptor[JsObject] = ArgumentCaptor.forClass(classOf[JsObject])

      val result = service.searchAndRenderView(form, 1, None, Scheme)

      status(result) mustBe OK
      verify(mockRenderer, times(1)).render(templateCaptor.capture(), jsonCaptor.capture())(any())
      jsonCaptor.getValue must containJson(schemeJson(
        fullSchemes.length,pagination,pageNumber,numberOfPages,Scheme, None,
        "Showing 1 to 1 of 2 schemes",typeOfList.head,Some(table)))

    }

    "return OK and the correct view when using page number" in {
      when(mockMinimalDetailsConnector.getPSADetails(any())(any(), any())).thenReturn(Future.successful(minimalPSA()))
      when(mockListOfSchemesConnector.getListOfSchemes(any())(any(), any())).thenReturn(Future.successful(Right(listOfSchemes)))

      val pageNumber: Int = 2

      val pagination: Int = 1

      val numberOfSchemes: Int = fullSchemes.length

      val numberOfPages = paginationService.divide(numberOfSchemes, pagination)

      when(mockAppConfig.listSchemePagination) thenReturn pagination

      val templateCaptor : ArgumentCaptor[String] = ArgumentCaptor.forClass(classOf[String])
      val jsonCaptor: ArgumentCaptor[JsObject] = ArgumentCaptor.forClass(classOf[JsObject])

      val result = service.searchAndRenderView(form, pageNumber, None, Scheme)

      status(result) mustBe OK
      verify(mockRenderer, times(1)).render(templateCaptor.capture(), jsonCaptor.capture())(any())
      jsonCaptor.getValue must containJson(schemeJson(
        fullSchemes.length,pagination,pageNumber,numberOfPages,Scheme, None,
        "Showing 2 to 2 of 2 schemes",typeOfList.head,Some(table)))

    }
  }

  "onSearch" when {
    "return OK and the correct view when there are schemes without pagination and search on non empty string" in {
      when(mockMinimalDetailsConnector.getPSADetails(any())(any(), any())).thenReturn(Future.successful(minimalPSA()))
      val searchText = "pstr1"
      when(mockListOfSchemesConnector.getListOfSchemes(any())(any(), any())).thenReturn(Future.successful(Right(listOfSchemes)))

      val numberOfPages =
        paginationService.divide(fullSchemes.length, pagination)

      val jsonCaptor: ArgumentCaptor[JsObject] = ArgumentCaptor.forClass(classOf[JsObject])
      val templateCaptor : ArgumentCaptor[String] = ArgumentCaptor.forClass(classOf[String])

      val postRequest = fakeRequest.withFormUrlEncodedBody(("value", searchText))
      implicit val request: AuthenticatedRequest[AnyContent] = AuthenticatedRequest(postRequest, "", PsaId(psaId))
      val result = service.searchAndRenderView(form, 1, Some(searchText), Scheme)

      val boundForm = form.bind(Map("value" -> "pstr1"))
      status(result) mustBe OK
      verify(mockRenderer, times(1)).render(templateCaptor.capture(), jsonCaptor.capture())(any())
      jsonCaptor.getValue must containJson(schemeJson(
        fullSchemes.length,pagination,1, numberOfPages, Scheme, None,
        "Showing 1 to 2 of 2 schemes", typeOfList.head, Some(table), boundForm))

    }

    "return BADREQUEST and error when no value is entered into search" in {
      when(mockMinimalDetailsConnector.getPSADetails(any())(any(), any())).thenReturn(Future.successful(minimalPSA()))
      when(mockListOfSchemesConnector.getListOfSchemes(any())(any(), any())).thenReturn(Future.successful(Right(listOfSchemes)))

      val numberOfPages = paginationService.divide(fullSchemes.length, pagination)

      val jsonCaptor: ArgumentCaptor[JsObject] = ArgumentCaptor.forClass(classOf[JsObject])
      val templateCaptor : ArgumentCaptor[String] = ArgumentCaptor.forClass(classOf[String])

      val postRequest = fakeRequest.withFormUrlEncodedBody(("value", ""))
      implicit val request: AuthenticatedRequest[AnyContent] = AuthenticatedRequest(postRequest, "", PsaId(psaId))
      val result = service.searchAndRenderView(form, 1, None, Scheme)
      val boundForm = form.bind(Map("value" -> ""))
      status(result) mustBe BAD_REQUEST

      verify(mockRenderer, times(1)).render(templateCaptor.capture(), jsonCaptor.capture())(any())
      jsonCaptor.getValue must containJson(schemeJson(
        fullSchemes.length,pagination,1, numberOfPages, Scheme, None,
        "Showing 1 to 2 of 2 schemes", typeOfList.head, Some(table), boundForm))

    }

    "return OK and the correct view with correct no matches message when unrecognised format is entered into search" in {
      when(mockMinimalDetailsConnector.getPSADetails(any())(any(), any())).thenReturn(Future.successful(minimalPSA()))
      val incorrectSearchText = "24000001IN"

      when(mockListOfSchemesConnector.getListOfSchemes(any())(any(), any())).thenReturn(Future.successful(Right(listOfSchemes)))

      val jsonCaptor: ArgumentCaptor[JsObject] = ArgumentCaptor.forClass(classOf[JsObject])
      val templateCaptor : ArgumentCaptor[String] = ArgumentCaptor.forClass(classOf[String])

      val postRequest = fakeRequest.withFormUrlEncodedBody(("value", incorrectSearchText))
      implicit val request: AuthenticatedRequest[AnyContent] = AuthenticatedRequest(postRequest, "", PsaId(psaId))
      val result = service.searchAndRenderView(form, 1, None, Scheme)
      val boundForm = form.bind(Map("value" -> incorrectSearchText))
      status(result) mustBe OK

      verify(mockRenderer, times(1)).render(templateCaptor.capture(), jsonCaptor.capture())(any())
      jsonCaptor.getValue must containJson(schemeJson(
        0,pagination,1,0,Scheme, Some(Messages("messages__listSchemes__search_noMatches",typeOfList.head)),
        "Showing 1 to 10 of 0 schemes",typeOfList.head,None,boundForm))

    }
  }
}


object SchemeSearchServiceSpec extends SpecBase with MockitoSugar with BeforeAndAfterEach {

  private implicit lazy val hc: HeaderCarrier = HeaderCarrier()
  implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global
  private val pstr1: String = "10000678RE"
  private val pstr2: String = "10000678RD"
  private val isRacDacFalse: Boolean = false
  private val isRacDacTrue: Boolean = true
  def listOfSchemes: ListOfLegacySchemes = ListOfLegacySchemes(2, Some(fullSchemes))
  def emptySchemes: ListOfLegacySchemes = ListOfLegacySchemes(0, None)

  def fullSchemes: List[Items] =
    List(
      Items(pstr1, "2020-10-10", racDac = false, "scheme-1", "1989-12-12", None),
      Items(pstr2, "2020-10-10", racDac = true, "scheme-2", "2000-10-12", Some("12345678"))
    )

  private val head=Seq(
    Cell(msg"messages__listSchemes__column_racDacName"),
    Cell(msg"messages__listSchemes__column_pstr"),
    Cell(msg"messages__listSchemes__column_regDate")
  )

  val rows= List(Seq(
    Cell(Literal("scheme-1"), Seq("govuk-!-width-one-quarter")),
    Cell(Literal(pstr1), Seq("govuk-!-width-one-quarter")),
    Cell(Literal("12 December 1989"), Seq("govuk-!-width-one-half"))),
    Seq(
      Cell(Literal("scheme-2"), Seq("govuk-!-width-one-quarter")),
      Cell(Literal(pstr2), Seq("govuk-!-width-one-quarter")),
      Cell(Literal("12 December 1989"), Seq("govuk-!-width-one-half")))
  )

  val table: Table = Table(head, rows,  attributes = Map("role" -> "table"))
}

