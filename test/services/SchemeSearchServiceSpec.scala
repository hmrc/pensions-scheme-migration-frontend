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

package services

import base.SpecBase
import connectors.{ListOfSchemesConnector, MinimalDetailsConnector}
import controllers.preMigration.routes
import controllers.preMigration.routes.ListOfSchemesController
import forms.ListSchemesFormProvider
import matchers.JsonMatchers
import models.MigrationType.isRacDac
import models._
import models.requests.AuthenticatedRequest
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, when}
import org.mockito.MockitoSugar.mock
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import play.api.data.Form
import play.api.http.Status._
import play.api.i18n.Messages
import play.api.mvc.AnyContent
import play.api.test.Helpers.{defaultAwaitTimeout, redirectLocation, status}
import uk.gov.hmrc.domain.PsaId
import uk.gov.hmrc.govukfrontend.views.html.components
import uk.gov.hmrc.govukfrontend.views.viewmodels.button.Button
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.{HtmlContent, Text}
import uk.gov.hmrc.govukfrontend.views.viewmodels.errormessage.ErrorMessage
import uk.gov.hmrc.govukfrontend.views.viewmodels.table.{HeadCell, TableRow}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.nunjucks.NunjucksSupport
import uk.gov.hmrc.viewmodels.MessageInterpolators
import utils.Data._
import utils.SchemeFuzzyMatcher
import views.html.preMigration.ListOfSchemesView

import scala.concurrent.{ExecutionContext, Future}

class SchemeSearchServiceSpec extends SpecBase with BeforeAndAfterEach with ScalaFutures with NunjucksSupport with JsonMatchers {

  import SchemeSearchServiceSpec._

  private val mockFuzzyMatching = mock[SchemeFuzzyMatcher]
  private val mockListOfSchemesConnector = mock[ListOfSchemesConnector]
  private val mockMinimalDetailsConnector: MinimalDetailsConnector = mock[MinimalDetailsConnector]
  private val paginationService = new PaginationService(mockAppConfig)

  private val typeOfList: List[String] = List("pension scheme", "RAC/DAC")
  private val psaName: String = "Nigel"
  private val pagination: Int = 10

  private def minimalPSA(rlsFlag: Boolean = false, deceasedFlag: Boolean = false) =
    MinPSA("", isPsaSuspended = false, Some(psaName), None, rlsFlag = rlsFlag, deceasedFlag = deceasedFlag)

  private val formProvider: ListSchemesFormProvider = new ListSchemesFormProvider()

  private def form(implicit messages: Messages): Form[String] = formProvider(messages("messages__listSchemes__search_required", "pension scheme"))

  private val dummyUrl = "dummyurl"
  implicit val request: AuthenticatedRequest[AnyContent] = AuthenticatedRequest(fakeRequest, "", PsaId(psaId))

  //scalastyle:off
  private def getView(
                      numberOfSchemes: Int,
                      pagination: Int,
                      pageNumber: Int,
                      numberOfPages: Int,
                      migrationType: MigrationType,
                      noResultsMessageKey: Option[String],
                      paginationText: String,
                      typeOfList: String,
                      schemeTable: components.Table,
                      form: Form[_] = form) = {

    val heading = form.value match {
      case Some(_) => messages("messages__listSchemes__search_result_title", typeOfList)
      case None => messages("messages__listSchemes__add_title", typeOfList)
    }

    val formErrorClass = if (form.hasErrors) {
      "govuk-form-group--error"
    } else {
      ""
    }

    val inputErrorClass = if (form.hasErrors) {
      "govuk-input--error"
    } else {
      ""
    }

    val errorMessages: Option[Seq[ErrorMessage]] = if(form.hasErrors) {
      val messages = form.errors.map((err) => {
        ErrorMessage(
          id = Some(s"value-error-${err.key}"),
          content = Text(err.message)
        )
      })
      Some(messages)
    } else {
      None
    }

    val buttonContent = form.value match {
      case Some(_) => messages("messages__listSchemes__search_again")
      case _ => messages("messages__listSchemes__search_submit")
    }


    val searchButton = Button(
      content = Text(buttonContent),
      attributes = Map("id" -> "search"),
      classes = "govuk-!-margin-bottom-3"
    )

    val listRacDacUrl = "/add-pension-scheme/rac-dac/list-rac-dacs/page/"
    val listUrl = "/add-pension-scheme/list-pension-schemes/page/"

    val listSchemeUrl = if (isRacDac(migrationType)) {
      listRacDacUrl
    } else {
      listUrl
    }

    app.injector.instanceOf[ListOfSchemesView].apply(
      heading = heading,
      form = form,
      submitCall = routes.ListOfSchemesController.onSearch(migrationType),
      formErrorClass = formErrorClass,
      errorMessages = errorMessages,
      inputErrorClass = inputErrorClass,
      searchButton = searchButton,
      schemes = schemeTable,
      clearLinkUrl = routes.ListOfSchemesController.onPageLoad(migrationType).url,
      numberOfSchemes = numberOfSchemes,
      noResultsMessageKey = noResultsMessageKey,
      pagination = pagination,
      paginationText = paginationText,
      pageNumber = pageNumber,
      pageNumberLinks = paginationService.pageNumberLinks(
        pageNumber,
        numberOfSchemes,
        pagination,
        numberOfPages
      ),
      returnUrl = dummyUrl,
      psaName = psaName,
      racDac = isRacDac(migrationType),
      listSchemeUrl = listSchemeUrl,
      numberOfPages = numberOfPages,
    )
  }

  val service = new SchemeSearchService(mockAppConfig, mockFuzzyMatching, mockListOfSchemesConnector,
    mockMinimalDetailsConnector, paginationService, app.injector.instanceOf[ListOfSchemesView])

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockAppConfig)
    when(mockAppConfig.psaOverviewUrl) thenReturn dummyUrl
    when(mockAppConfig.listSchemePagination) thenReturn pagination
    when(mockAppConfig.psaUpdateContactDetailsUrl).thenReturn(dummyUrl)
    when(mockAppConfig.deceasedContactHmrcUrl).thenReturn(dummyUrl)
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

      whenReady(service.search(psaId, Some("scheme-1"), isRacDac = isRacDacFalse)) { result =>
        result mustBe fullSchemes.filter(_.racDac == isRacDacFalse).filter(_.schemeName == "scheme-1")
      }
    }

    "return correct list of scheme details with search on scheme name with rac dac" in {
      when(mockFuzzyMatching.doFuzzyMatching(any(), any())).thenReturn(true)
      when(mockListOfSchemesConnector.getListOfSchemes(ArgumentMatchers.eq(psaId))(any(), any()))
        .thenReturn(Future.successful(Right(listOfSchemes)))

      whenReady(service.search(psaId, Some("scheme-1"), isRacDac = isRacDacTrue)) { result =>
        result mustBe fullSchemes.filter(_.racDac == isRacDacTrue).filter(_.schemeName == "scheme-2")
      }
    }

    "return empty list when fuzzy matching fails" in {
      when(mockFuzzyMatching.doFuzzyMatching(any(), any())).thenReturn(false)
      val emptyList = ListOfLegacySchemes(0, None)
      when(mockListOfSchemesConnector.getListOfSchemes(ArgumentMatchers.eq(psaId))(any(), any()))
        .thenReturn(Future.successful(Right(emptyList)))

      whenReady(service.search(psaId, Some("no matching"), isRacDac = isRacDacFalse)) { result =>
        result mustBe Nil
      }
    }

  }

  "mapToTable" must {

    "return correct table of scheme details" in {
      val head = Seq(
        HeadCell(content = Text(msg"messages__listSchemes__column_schemeName".resolve)),
        HeadCell(content = Text(msg"messages__listSchemes__column_pstr".resolve)),
        HeadCell(content = Text(msg"messages__listSchemes__column_regDate".resolve))
      )

      val rows = List(Seq(
        TableRow(content = HtmlContent(s"""<a class="govuk-link migrate-pstr-$pstr1" href=/add-pension-scheme/list-schemes-on-click/$pstr1/false>scheme-1</a>"""),
          classes = "govuk-!-width-one-half"),
        TableRow(content = Text(pstr1), classes = "govuk-!-width-one-quarter"),
        TableRow(content = Text("12 December 1989"), classes = "govuk-!-width-one-quarter")))

      service.mapToTable(List(fullSchemes.head), isRacDac = isRacDacFalse) mustBe
        components.Table(rows, Some(head), attributes = Map("role" -> "table"))

    }

    "return correct table of scheme details with rac dac" in {
      val head = Seq(
        HeadCell(content = Text(msg"messages__listSchemes__column_racDacName".resolve)),
        HeadCell(content = Text(msg"messages__listSchemes__column_pstr".resolve)),
        HeadCell(content = Text(msg"messages__listSchemes__column_regDate".resolve))
      )

      val rows = List(Seq(
        TableRow(content = HtmlContent(s"""<a class="govuk-link migrate-pstr-$pstr2" href=/add-pension-scheme/list-schemes-on-click/$pstr2/true>scheme-2</a>"""),
          classes = "govuk-!-width-one-half"),
        TableRow(Text(pstr2), classes = "govuk-!-width-one-quarter"),
        TableRow(Text("12 October 2000"), classes = "govuk-!-width-one-quarter")))

      val table = components.Table(rows, Some(head), attributes = Map("role" -> "table"))

      service.mapToTable(fullSchemes.tail, isRacDac = isRacDacTrue) mustBe table

    }
  }

  "searchAndRenderView" must {
    "return OK and the correct view when there are no schemes" in {
      when(mockMinimalDetailsConnector.getPSADetails(any())(any(), any())).thenReturn(Future.successful(minimalPSA()))
      when(mockListOfSchemesConnector.getListOfSchemes(any())(any(), any())).thenReturn(Future.successful(Right(emptySchemes)))
      val numberOfPages = paginationService.divide(0, pagination)


      val result = service.searchAndRenderView(form, 1, None, Scheme)

      status(result) mustBe OK
      compareResultAndView(
        result,
        getView(
          0, pagination, pageNumber = 1, numberOfPages, Scheme, Some(messages("messages__listSchemes__noSchemes",
            typeOfList.head)), "Showing 1 to 1 of 2 schemes", typeOfList.head, components.Table(head = Some(schemeHead), attributes = Map("role" -> "table"))
        )
      )

    }

    "rlsFlag is true and deceasedFlag is false return redirect to update contact page" in {
      when(mockMinimalDetailsConnector.getPSADetails(any())(any(), any())).thenReturn(Future.successful(minimalPSA(rlsFlag = true)))

      val result = service.searchAndRenderView(form, 1, None, Scheme)

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(dummyUrl)
    }

    "rlsFlag is true and deceasedFlag is true return redirect to contact hmrc page" in {
      when(mockMinimalDetailsConnector.getPSADetails(any())(any(), any())).thenReturn(Future.successful(minimalPSA(rlsFlag = true, deceasedFlag = true)))

      val result = service.searchAndRenderView(form, 1, None, Scheme)

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(dummyUrl)
    }

    "return OK and the correct view when there are schemes without pagination" in {
      when(mockMinimalDetailsConnector.getPSADetails(any())(any(), any())).thenReturn(Future.successful(minimalPSA()))
      when(mockListOfSchemesConnector.getListOfSchemes(any())(any(), any())).thenReturn(Future.successful(Right(listOfSchemes)))

      val numberOfPages = paginationService.divide(fullSchemes.length, pagination)

      val result = service.searchAndRenderView(form, 1, None, Scheme)

      status(result) mustBe OK
      compareResultAndView(
        result,
        getView(
          1, pagination, pageNumber = 1, numberOfPages, Scheme, None,
          "Showing 1 to 1 of 2 schemes", typeOfList.head, tableForScheme
        )
      )
    }

    "return OK and the correct view when there are schemes with pagination" in {
      when(mockMinimalDetailsConnector.getPSADetails(any())(any(), any())).thenReturn(Future.successful(minimalPSA()))
      when(mockListOfSchemesConnector.getListOfSchemes(any())(any(), any())).
        thenReturn(Future.successful(Right(listOfSchemes.copy(items = Some(twoRacDacs)))))

      val pageNumber: Int = 1
      val pagination: Int = 1
      val numberOfSchemes: Int = fullSchemes.length

      val numberOfPages = paginationService.divide(numberOfSchemes, pagination)

      when(mockAppConfig.listSchemePagination) thenReturn pagination

      val result = service.searchAndRenderView(form, 1, None, RacDac)

      status(result) mustBe OK
      compareResultAndView(result,
        getView(
          fullSchemes.length, pagination, pageNumber, numberOfPages, RacDac, None,
          "Showing 1 to 1 of 2 schemes", typeOfList(1), tableForRacDac.copy(rows = List(racDacRows.head))
        )
      )
    }

    "return OK and the correct view when using page number" in {
      when(mockMinimalDetailsConnector.getPSADetails(any())(any(), any())).thenReturn(Future.successful(minimalPSA()))
      when(mockListOfSchemesConnector.getListOfSchemes(any())(any(), any()))
        .thenReturn(Future.successful(Right(listOfSchemes.copy(items = Some(twoRacDacs)))))

      val pageNumber: Int = 2

      val pagination: Int = 1

      val numberOfSchemes: Int = fullSchemes.length

      val numberOfPages = paginationService.divide(numberOfSchemes, pagination)

      when(mockAppConfig.listSchemePagination) thenReturn pagination

      val result = service.searchAndRenderView(form, pageNumber, None, RacDac)

      status(result) mustBe OK
      compareResultAndView(result,
        getView(
          fullSchemes.length, pagination, pageNumber, numberOfPages, RacDac, None,
          "Showing 2 to 2 of 2 schemes", typeOfList(1), tableForRacDac.copy(rows = racDacRows.tail)
        )
      )
    }
  }

  "onSearch" when {
    "return OK and the correct view when there are schemes without pagination and search on non empty string" in {
      when(mockMinimalDetailsConnector.getPSADetails(any())(any(), any())).thenReturn(Future.successful(minimalPSA()))
      val searchText = pstr1
      when(mockListOfSchemesConnector.getListOfSchemes(any())(any(), any())).thenReturn(Future.successful(Right(listOfSchemes)))
      when(mockAppConfig.listSchemePagination) thenReturn 2
      val numberOfPages =
        paginationService.divide(fullSchemes.length, 2)

      val postRequest = fakeRequest.withFormUrlEncodedBody(("value", searchText))
      implicit val request: AuthenticatedRequest[AnyContent] = AuthenticatedRequest(postRequest, "", PsaId(psaId))
      val boundForm = form.bind(Map("value" -> searchText))

      val result = service.searchAndRenderView(boundForm, 1, Some(searchText), Scheme)

      status(result) mustBe OK
      compareResultAndView(
        result,
        getView(
          1, 2, 1, numberOfPages, Scheme, None,
          "Showing 1 to 1 of 1 schemes", typeOfList.head, tableForScheme, boundForm
        )
      )

    }

    "return BADREQUEST and error when no value is entered into search" in {
      when(mockMinimalDetailsConnector.getPSADetails(any())(any(), any())).thenReturn(Future.successful(minimalPSA()))
      when(mockListOfSchemesConnector.getListOfSchemes(any())(any(), any())).thenReturn(Future.successful(Right(listOfSchemes)))

      val numberOfPages = paginationService.divide(fullSchemes.length, pagination)

      val postRequest = fakeRequest.withFormUrlEncodedBody(("value", ""))
      implicit val request: AuthenticatedRequest[AnyContent] = AuthenticatedRequest(postRequest, "", PsaId(psaId))
      val boundForm = form.bind(Map("value" -> ""))
      val result = service.searchAndRenderView(boundForm, 1, None, Scheme)
      status(result) mustBe BAD_REQUEST
      compareResultAndView(result,
        getView(1, pagination, 1, numberOfPages, Scheme, None,
          "Showing 1 to 1 of 1 schemes", typeOfList.head, tableForScheme, boundForm
        )
      )

    }

    "return OK and the correct view with correct no matches message when unrecognised format is entered into search" in {
      when(mockMinimalDetailsConnector.getPSADetails(any())(any(), any())).thenReturn(Future.successful(minimalPSA()))
      val incorrectSearchText = "24000001IN"

      when(mockListOfSchemesConnector.getListOfSchemes(any())(any(), any())).thenReturn(Future.successful(Right(listOfSchemes)))

      val boundForm = form.bind(Map("value" -> incorrectSearchText))
      val result = service.searchAndRenderView(boundForm, 1, Some(incorrectSearchText), Scheme)
      status(result) mustBe OK
      compareResultAndView(
        result,
        getView(
          0, pagination, 1, 0, Scheme, Some(Messages("messages__listSchemes__search_noMatches", typeOfList.head)),
          "Showing 1 to 10 of 0 schemes", typeOfList.head, components.Table(head = Some(schemeHead), attributes = Map("role" -> "table")), boundForm
        )
      )

    }
  }
}


object SchemeSearchServiceSpec extends SpecBase  with BeforeAndAfterEach {

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

  def twoRacDacs: List[Items] =
    List(
      Items(pstr1, "2020-10-10", racDac = true, "scheme-1", "1989-12-12", Some("12345678")),
      Items(pstr2, "2020-10-10", racDac = true, "scheme-2", "2000-10-12", Some("12345678"))
    )

  private val head = Seq(
    HeadCell(content = Text(msg"messages__listSchemes__column_racDacName".resolve)),
    HeadCell(content = Text(msg"messages__listSchemes__column_pstr".resolve)),
    HeadCell(content = Text(msg"messages__listSchemes__column_regDate".resolve))
  )

  private val schemeHead = Seq(
    HeadCell(content = Text(msg"messages__listSchemes__column_schemeName".resolve)),
    HeadCell(content = Text(msg"messages__listSchemes__column_pstr".resolve)),
    HeadCell(content = Text(msg"messages__listSchemes__column_regDate".resolve))
  )

  private val tableForScheme = components.Table(
    List(Seq(
      TableRow(HtmlContent(
        s"""<a class="govuk-link migrate-pstr-$pstr1" href=${ListOfSchemesController.clickSchemeLink(pstr1, false)}>scheme-1</a>""".stripMargin),
        classes = "govuk-!-width-one-half"),
      TableRow(Text(pstr1), classes = "govuk-!-width-one-quarter"),
      TableRow(Text("12 December 1989"), classes = "govuk-!-width-one-quarter"))
    ),
    head = Some(schemeHead),
    attributes = Map("role" -> "table"))

  val racDacRows = List(
    Seq(
      TableRow(HtmlContent(
        s"""<a class="govuk-link migrate-pstr-$pstr1" href=${ListOfSchemesController.clickSchemeLink(pstr1, true)}>scheme-1</a>""".stripMargin),
        classes = "govuk-!-width-one-half"),
      TableRow(Text(pstr1), classes = "govuk-!-width-one-quarter"),
      TableRow(Text("12 December 1989"), classes = "govuk-!-width-one-quarter")
    ),
    Seq(
      TableRow(HtmlContent(
        s"""<a class="govuk-link migrate-pstr-$pstr2" href=${ListOfSchemesController.clickSchemeLink(pstr2, true)}>scheme-2</a>""".stripMargin),
        classes = "govuk-!-width-one-half"),
      TableRow(Text(pstr2), classes = "govuk-!-width-one-quarter"),
      TableRow(Text("12 October 2000"), classes = "govuk-!-width-one-quarter")
    )
  )
  val tableForRacDac: components.Table = components.Table(racDacRows, head = Some(head), attributes = Map("role" -> "table"))
}

