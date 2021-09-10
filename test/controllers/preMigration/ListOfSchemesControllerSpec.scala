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

package controllers.preMigration

import connectors.MinimalDetailsConnector
import controllers.ControllerSpecBase
import controllers.actions._
import forms.ListSchemesFormProvider
import matchers.JsonMatchers
import models.MigrationType.isRacDac
import models.{Items, MigrationType, MinPSA, Scheme}
import org.mockito.ArgumentMatchers.any
import org.mockito.{ArgumentCaptor, ArgumentMatchers, MockitoSugar}
import org.scalatest.TryValues
import play.api.data.Form
import play.api.i18n.Messages
import play.api.libs.json.{JsObject, Json}
import play.api.test.Helpers.{status, _}
import play.twirl.api.Html
import renderer.Renderer
import services.{PaginationService, SchemeSearchService}
import uk.gov.hmrc.nunjucks.NunjucksSupport
import uk.gov.hmrc.viewmodels.Table.Cell
import uk.gov.hmrc.viewmodels.Text.Literal
import uk.gov.hmrc.viewmodels.{MessageInterpolators, Table}

import scala.concurrent.Future

class ListOfSchemesControllerSpec extends ControllerSpecBase with NunjucksSupport with JsonMatchers with TryValues with MockitoSugar {

  private val templateToBeRendered: String = "preMigration/listOfSchemes.njk"
  private val psaName: String = "Nigel"
  private val mockMinimalDetailsConnector: MinimalDetailsConnector = mock[MinimalDetailsConnector]
  private val mockSchemeSearchService: SchemeSearchService = mock[SchemeSearchService]
  private val paginationService = new PaginationService
  private val typeOfList:List[String]=List("pension scheme","RAC/DAC")
  private val formProvider: ListSchemesFormProvider =
    new ListSchemesFormProvider()
  private def form(implicit messages: Messages): Form[String] = formProvider(messages("messages__listSchemes__search_required", typeOfList.head))
  private val emptySchemes: List[Items] = List.empty[Items]
  private def controller: ListOfSchemesController =
    new ListOfSchemesController(mockAppConfig, messagesApi, new FakeAuthAction(),
      mockMinimalDetailsConnector,controllerComponents,paginationService,formProvider
      ,mockSchemeSearchService, new Renderer(mockAppConfig, mockRenderer))
  private val dummyUrl="dummyurl"
  private val pagination: Int = 10
  private def minimalPSA(rlsFlag: Boolean = false, deceasedFlag: Boolean = false) = MinPSA(
    email = "",
    isPsaSuspended = false,
    organisationName = Some(psaName),
    individualDetails = None,
    rlsFlag = rlsFlag,
    deceasedFlag = deceasedFlag
  )

  private val schemeDetail = Items("pstr1", "2020-10-10", racDac = false, "scheme-1", "2020-12-12", None)
  private val racDacDetail = Items("pstr2", "2020-10-10", racDac = true, "scheme-2", "2020-12-12", Some("12345678"))
  private val fullSchemes = List(schemeDetail, racDacDetail)
  private val head=Seq(
    Cell(msg"messages__listSchemes__column_racDacName"),
    Cell(msg"messages__listSchemes__column_pstr"),
    Cell(msg"messages__listSchemes__column_regDate")
  )

  val rows= List(Seq(
    Cell(Literal("scheme-1"), Seq("govuk-!-width-one-quarter")),
    Cell(Literal("pstr1"), Seq("govuk-!-width-one-quarter")),
    Cell(Literal("12 December 1989"), Seq("govuk-!-width-one-half"))),
    Seq(
      Cell(Literal("scheme-2"), Seq("govuk-!-width-one-quarter")),
      Cell(Literal("pstr2"), Seq("govuk-!-width-one-quarter")),
      Cell(Literal("12 December 1989"), Seq("govuk-!-width-one-half")))
    )

  val table=Table(head, rows,  attributes = Map("role" -> "table"))

  private def schemeJson(numberOfSchemes:Int,pagination:Int,pageNumber:Int,numberOfPages:Int,migrationType:MigrationType,
                         noResultsMessageKey:Option[String],paginationText:String,typeOfList:String,schemeTable:Option[Table],form:Form[_]=form ): JsObject = Json.obj(

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

  override def beforeEach: Unit = {
    super.beforeEach
    when(mockRenderer.render(any(), any())(any())).thenReturn(Future.successful(Html("")))
    when(mockAppConfig.listSchemePagination) thenReturn pagination
    when(mockAppConfig.psaOverviewUrl) thenReturn dummyUrl
    when(mockAppConfig.psaUpdateContactDetailsUrl).thenReturn(dummyUrl)
    when(mockAppConfig.deceasedContactHmrcUrl).thenReturn(dummyUrl)
  }

  "onPageLoad" must {
    "return OK and the correct view when there are no schemes" in {
      when(mockMinimalDetailsConnector.getPSADetails(any())(any(), any())).thenReturn(Future.successful(minimalPSA()))
      when(mockSchemeSearchService.search(any(), any(),any())(any(), any())).thenReturn(Future.successful(Nil))
      val numberOfPages = paginationService.divide(emptySchemes.length, pagination)

      val templateCaptor : ArgumentCaptor[String] = ArgumentCaptor.forClass(classOf[String])
      val jsonCaptor: ArgumentCaptor[JsObject] = ArgumentCaptor.forClass(classOf[JsObject])

      val result = controller.onPageLoad(Scheme)(fakeRequest)

      status(result) mustBe OK

      verify(mockRenderer, times(1)).render(templateCaptor.capture(), jsonCaptor.capture())(any())

      templateCaptor.getValue mustEqual templateToBeRendered

      jsonCaptor.getValue must containJson(schemeJson(
        0,pagination,pageNumber = 1,numberOfPages,Scheme, Some(messages("messages__listSchemes__noSchemes",typeOfList.head)),
        "Showing 1 to 10 of 0 schemes",typeOfList.head,None))

    }

    "rlsFlag is true and deceasedFlag is false return redirect to update contact page" in {
      when(mockMinimalDetailsConnector.getPSADetails(any())(any(), any())).thenReturn(Future.successful(minimalPSA(rlsFlag = true)))
      when(mockSchemeSearchService.search(any(), any(),any())(any(), any())).thenReturn(Future.successful(Nil))

      val result = controller.onPageLoad(Scheme)(fakeRequest)

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(dummyUrl)
    }

    "rlsFlag is true and deceasedFlag is true return redirect to contact hmrc page" in {
      when(mockMinimalDetailsConnector.getPSADetails(any())(any(), any())).thenReturn(Future.successful(minimalPSA(rlsFlag = true,deceasedFlag = true)))
      when(mockSchemeSearchService.search(any(), any(),any())(any(), any())).thenReturn(Future.successful(Nil))

      val result = controller.onPageLoad(Scheme)(fakeRequest)

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(dummyUrl)
    }

    "return OK and the correct view when there are schemes without pagination" in {
      when(mockMinimalDetailsConnector.getPSADetails(any())(any(), any())).thenReturn(Future.successful(minimalPSA()))
      when(mockSchemeSearchService.search(any(), any(),any())(any(), any())).thenReturn(Future.successful(fullSchemes))
      when(mockSchemeSearchService.mapToTable(any(),any())).thenReturn(table)

      val numberOfPages = paginationService.divide(fullSchemes.length, pagination)

      val templateCaptor : ArgumentCaptor[String] = ArgumentCaptor.forClass(classOf[String])
      val jsonCaptor: ArgumentCaptor[JsObject] = ArgumentCaptor.forClass(classOf[JsObject])

      val result = controller.onPageLoad(Scheme)(fakeRequest)

      status(result) mustBe OK
      verify(mockRenderer, times(1)).render(templateCaptor.capture(), jsonCaptor.capture())(any())

      jsonCaptor.getValue must containJson(schemeJson(
        fullSchemes.length,pagination,pageNumber = 1,numberOfPages,Scheme, None,
        "Showing 1 to 2 of 2 schemes",typeOfList.head,Some(table)))
    }

    "return OK and the correct view when there are schemes with pagination" in {
      when(mockMinimalDetailsConnector.getPSADetails(any())(any(), any())).thenReturn(Future.successful(minimalPSA()))
      when(mockSchemeSearchService.search(any(), any(),any())(any(), any())).thenReturn(Future.successful(fullSchemes))
      when(mockSchemeSearchService.mapToTable(any(),any())).thenReturn(table)
      val pageNumber: Int = 1
      val pagination: Int = 1
      val numberOfSchemes: Int = fullSchemes.length

      val numberOfPages = paginationService.divide(numberOfSchemes, pagination)

      when(mockAppConfig.listSchemePagination) thenReturn pagination

      val templateCaptor : ArgumentCaptor[String] = ArgumentCaptor.forClass(classOf[String])
      val jsonCaptor: ArgumentCaptor[JsObject] = ArgumentCaptor.forClass(classOf[JsObject])

      val result = controller.onPageLoad(Scheme)(fakeRequest)

      status(result) mustBe OK
      verify(mockRenderer, times(1)).render(templateCaptor.capture(), jsonCaptor.capture())(any())
      jsonCaptor.getValue must containJson(schemeJson(
        fullSchemes.length,pagination,pageNumber,numberOfPages,Scheme, None,
        "Showing 1 to 1 of 2 schemes",typeOfList.head,Some(table)))

    }

    "return OK and the correct view when using page number" in {
      when(mockMinimalDetailsConnector.getPSADetails(any())(any(), any())).thenReturn(Future.successful(minimalPSA()))
      when(mockSchemeSearchService.search(any(), any(),any())(any(), any())).thenReturn(Future.successful(fullSchemes))
      when(mockSchemeSearchService.mapToTable(any(),any())).thenReturn(table)
      val pageNumber: Int = 2

      val pagination: Int = 1

      val numberOfSchemes: Int = fullSchemes.length

      val numberOfPages = paginationService.divide(numberOfSchemes, pagination)

      when(mockAppConfig.listSchemePagination) thenReturn pagination

      val templateCaptor : ArgumentCaptor[String] = ArgumentCaptor.forClass(classOf[String])
      val jsonCaptor: ArgumentCaptor[JsObject] = ArgumentCaptor.forClass(classOf[JsObject])

      val result = controller.onPageLoadWithPageNumber(pageNumber = pageNumber,Scheme)(fakeRequest)

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
      when(mockSchemeSearchService.search(any(), ArgumentMatchers.eq(Some(searchText)),any())(any(), any())).thenReturn(Future.successful(fullSchemes))

      val numberOfPages =
        paginationService.divide(fullSchemes.length, pagination)

      val jsonCaptor: ArgumentCaptor[JsObject] = ArgumentCaptor.forClass(classOf[JsObject])
      val templateCaptor : ArgumentCaptor[String] = ArgumentCaptor.forClass(classOf[String])

      val postRequest = fakeRequest.withFormUrlEncodedBody(("value", searchText))
      val result = controller.onSearch(Scheme)(postRequest)
      val boundForm = form.bind(Map("value" -> "pstr1"))
      status(result) mustBe OK
      verify(mockRenderer, times(1)).render(templateCaptor.capture(), jsonCaptor.capture())(any())
      jsonCaptor.getValue must containJson(schemeJson(
        fullSchemes.length,pagination,1,numberOfPages,Scheme, None,
        "Showing 1 to 2 of 2 schemes",typeOfList.head,Some(table),boundForm))

    }

    "return BADREQUEST and error when no value is entered into search" in {
      when(mockMinimalDetailsConnector.getPSADetails(any())(any(), any())).thenReturn(Future.successful(minimalPSA()))
      when(mockSchemeSearchService.search(any(), ArgumentMatchers.eq(None),any())(any(), any())).thenReturn(Future.successful(fullSchemes))

      val numberOfPages = paginationService.divide(fullSchemes.length, pagination)

      val jsonCaptor: ArgumentCaptor[JsObject] = ArgumentCaptor.forClass(classOf[JsObject])
      val templateCaptor : ArgumentCaptor[String] = ArgumentCaptor.forClass(classOf[String])

      val postRequest = fakeRequest.withFormUrlEncodedBody(("value", ""))
      val result = controller.onSearch(Scheme)(postRequest)
      val boundForm = form.bind(Map("value" -> ""))
      status(result) mustBe BAD_REQUEST

      verify(mockRenderer, times(1)).render(templateCaptor.capture(), jsonCaptor.capture())(any())
      jsonCaptor.getValue must containJson(schemeJson(
        fullSchemes.length,pagination,1,numberOfPages,Scheme, None,
        "Showing 1 to 2 of 2 schemes",typeOfList.head,Some(table),boundForm))

    }

    "return OK and the correct view with correct no matches message when unrecognised format is entered into search" in {
      when(mockMinimalDetailsConnector.getPSADetails(any())(any(), any())).thenReturn(Future.successful(minimalPSA()))
      val incorrectSearchText = "24000001IN"
      when(mockSchemeSearchService.search(any(), ArgumentMatchers.eq(Some(incorrectSearchText)),any())(any(), any())).thenReturn(Future.successful(Nil))

      val jsonCaptor: ArgumentCaptor[JsObject] = ArgumentCaptor.forClass(classOf[JsObject])
      val templateCaptor : ArgumentCaptor[String] = ArgumentCaptor.forClass(classOf[String])

      val postRequest = fakeRequest.withFormUrlEncodedBody(("value", incorrectSearchText))
      val result = controller.onSearch(Scheme)(postRequest)
      val boundForm = form.bind(Map("value" -> incorrectSearchText))
      status(result) mustBe OK

      verify(mockRenderer, times(1)).render(templateCaptor.capture(), jsonCaptor.capture())(any())
      jsonCaptor.getValue must containJson(schemeJson(
        0,pagination,1,0,Scheme, Some(Messages("messages__listSchemes__search_noMatches",typeOfList.head)),
        "Showing 1 to 10 of 0 schemes",typeOfList.head,None,boundForm))

    }
  }


}
