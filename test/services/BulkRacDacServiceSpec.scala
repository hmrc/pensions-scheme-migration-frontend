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
import forms.racdac.RacDacBulkListFormProvider
import matchers.JsonMatchers
import models._
import models.requests.{AuthenticatedRequest, BulkDataRequest}
import org.mockito.ArgumentMatchers.any
import org.mockito.{ArgumentCaptor, MockitoSugar}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import play.api.data.Form
import play.api.http.Status._
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.AnyContent
import play.api.test.Helpers.{defaultAwaitTimeout, redirectLocation, status}
import play.twirl.api.Html
import renderer.Renderer
import uk.gov.hmrc.domain.PsaId
import uk.gov.hmrc.nunjucks.NunjucksSupport
import uk.gov.hmrc.viewmodels.Table.Cell
import uk.gov.hmrc.viewmodels.Text.Literal
import uk.gov.hmrc.viewmodels.{MessageInterpolators, Table}
import utils.Data._

import scala.concurrent.{ExecutionContext, Future}

class BulkRacDacServiceSpec extends SpecBase
  with BeforeAndAfterEach
  with ScalaFutures
  with NunjucksSupport
  with MockitoSugar
  with JsonMatchers {

  import BulkRacDacServiceSpec._

  private val templateToBeRendered: String = "racdac/racDacsBulkList.njk"
  private val paginationService = new PaginationService(mockAppConfig)
  private val pagination: Int = 10
  private val formProvider: RacDacBulkListFormProvider = new RacDacBulkListFormProvider()

  private def form: Form[Boolean] = formProvider()
  private val dummyUrl = "dummyurl"

  private def schemeJson(numberOfSchemes: Int, pagination: Int, pageNumber: Int, numberOfPages: Int, paginationText:Option[String],
                         schemeTable: Option[Table]): JsObject = Json.obj(
    "form" -> form,
    "psaName" -> "test company",
    "numberOfSchemes" -> numberOfSchemes,
    "pagination" -> pagination,
    "pageNumber" -> pageNumber,
    "pageNumberLinks" -> paginationService.pageNumberLinks(
      pageNumber,
      numberOfSchemes,
      pagination,
      numberOfPages
    ),
    "numberOfPages" -> numberOfPages,
    "returnUrl" -> dummyUrl,
    "paginationText" -> paginationService.paginationText(pageNumber, pagination, numberOfSchemes, numberOfPages),
  ) ++  (if (schemeTable.isDefined) Json.obj("schemes" -> schemeTable.get) else Json.obj()) ++
    (if (paginationText.isDefined) Json.obj("paginationText" -> paginationText.get) else Json.obj())

  private val service = new BulkRacDacService(mockAppConfig, paginationService, new Renderer(mockAppConfig, mockRenderer))
  private val minPSA = MinPSA("test@test.com", false, Some("test company"), None, false, false)
  private val listOfSchemes = List(Items(pstr1, "2020-10-10", true, "scheme-1", "2000-10-12", Some("12345678")))

  private val authReq: AuthenticatedRequest[AnyContent] = AuthenticatedRequest(
    request = fakeRequest,
    externalId = "id",
    psaId = PsaId(psaId)
  )

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockAppConfig, mockRenderer)
    when(mockAppConfig.psaOverviewUrl) thenReturn dummyUrl
    when(mockAppConfig.psaUpdateContactDetailsUrl).thenReturn(dummyUrl)
    when(mockAppConfig.deceasedContactHmrcUrl).thenReturn(dummyUrl)
    when(mockAppConfig.listSchemePagination) thenReturn pagination
    when(mockRenderer.render(any(), any())(any())).thenReturn(Future.successful(Html("")))
  }

  "mapToTable" must {

    "return correct table of scheme details" in {
      val head = Seq(
        Cell(msg"messages__listSchemes__column_racDacName"),
        Cell(msg"messages__listSchemes__column_pstr"),
        Cell(msg"messages__listSchemes__column_regDate")
      )

      val rows = List(Seq(
        Cell(Literal("scheme-2"), Seq("govuk-!-width-one-half")),
        Cell(Literal(pstr2), Seq("govuk-!-width-one-quarter")),
        Cell(Literal("12 October 2000"), Seq("govuk-!-width-one-quarter"))))

      val table = Table(head, rows, attributes = Map("role" -> "table"))

      service.mapToTable(fullSchemes.tail) mustBe table

    }
  }

  "renderRacDacBulkView" must {
    "return OK and the correct view when there are no schemes" in {
      val numberOfPages = paginationService.divide(0, pagination)
      val bulkDataRequest: BulkDataRequest[AnyContent] = BulkDataRequest(authReq, minPSA, List.empty)
      val templateCaptor: ArgumentCaptor[String] = ArgumentCaptor.forClass(classOf[String])
      val jsonCaptor: ArgumentCaptor[JsObject] = ArgumentCaptor.forClass(classOf[JsObject])

      val result = service.renderRacDacBulkView(form, 1)(bulkDataRequest, implicitly, implicitly)

      status(result) mustBe OK

      verify(mockRenderer, times(1)).render(templateCaptor.capture(), jsonCaptor.capture())(any())

      templateCaptor.getValue mustEqual templateToBeRendered

      jsonCaptor.getValue must containJson(schemeJson(
        0, pagination, pageNumber = 1, numberOfPages,
        Some(messages("messages__listSchemes__pagination__text", 1, 10, 0)), None))
    }

    "rlsFlag is true and deceasedFlag is false return redirect to update contact page" in {
      val bulkDataRequest: BulkDataRequest[AnyContent] = BulkDataRequest(authReq, minPSA.copy(rlsFlag = true), listOfSchemes)
      val result = service.renderRacDacBulkView(form, 1)(bulkDataRequest, implicitly, implicitly)

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(dummyUrl)
    }

    "rlsFlag is true and deceasedFlag is true return redirect to contact hmrc page" in {
      val bulkDataRequest: BulkDataRequest[AnyContent] = BulkDataRequest(authReq,
        minPSA.copy(rlsFlag = true, deceasedFlag = true), listOfSchemes)
      val result = service.renderRacDacBulkView(form, 1)(bulkDataRequest, implicitly, implicitly)
      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(dummyUrl)
    }

    "return OK and the correct view when there are schemes without pagination" in {
      val bulkDataRequest: BulkDataRequest[AnyContent] = BulkDataRequest(authReq, minPSA, listOfSchemes)
      val numberOfPages = paginationService.divide(fullSchemes.length, pagination)

      val templateCaptor: ArgumentCaptor[String] = ArgumentCaptor.forClass(classOf[String])
      val jsonCaptor: ArgumentCaptor[JsObject] = ArgumentCaptor.forClass(classOf[JsObject])

      val result = service.renderRacDacBulkView(form, 1)(bulkDataRequest, implicitly, implicitly)
      status(result) mustBe OK
      verify(mockRenderer, times(1)).render(templateCaptor.capture(), jsonCaptor.capture())(any())

      jsonCaptor.getValue must containJson(schemeJson(
        1, pagination, pageNumber = 1, numberOfPages, None, Some(tableForScheme)))
    }

    "return OK and the correct view when there are schemes with pagination" in {
      val bulkDataRequest: BulkDataRequest[AnyContent] = BulkDataRequest(authReq, minPSA, twoRacDacs)
      val pageNumber: Int = 1
      val pagination: Int = 1
      val numberOfSchemes: Int = fullSchemes.length

      val numberOfPages = paginationService.divide(numberOfSchemes, pagination)

      when(mockAppConfig.listSchemePagination) thenReturn pagination

      val templateCaptor: ArgumentCaptor[String] = ArgumentCaptor.forClass(classOf[String])
      val jsonCaptor: ArgumentCaptor[JsObject] = ArgumentCaptor.forClass(classOf[JsObject])
      val result = service.renderRacDacBulkView(form, 1)(bulkDataRequest, implicitly, implicitly)

      status(result) mustBe OK
      verify(mockRenderer, times(1)).render(templateCaptor.capture(), jsonCaptor.capture())(any())
      jsonCaptor.getValue must containJson(schemeJson(
        twoRacDacs.length, pagination, pageNumber, numberOfPages,
        Some("Showing 1 to 1 of 2 schemes"), Some(tableForRacDac.copy(rows = List(racDacRows.head)))))
    }

    "return OK and the correct view when using page number" in {
      val pageNumber: Int = 2

      val pagination: Int = 1

      val numberOfSchemes: Int = twoRacDacs.length

      val numberOfPages = paginationService.divide(numberOfSchemes, pagination)

      when(mockAppConfig.listSchemePagination) thenReturn pagination

      val templateCaptor: ArgumentCaptor[String] = ArgumentCaptor.forClass(classOf[String])
      val jsonCaptor: ArgumentCaptor[JsObject] = ArgumentCaptor.forClass(classOf[JsObject])

      val bulkDataRequest: BulkDataRequest[AnyContent] = BulkDataRequest(authReq, minPSA, twoRacDacs)
      val result = service.renderRacDacBulkView(form, pageNumber)(bulkDataRequest, implicitly, implicitly)
      status(result) mustBe OK
      verify(mockRenderer, times(1)).render(templateCaptor.capture(), jsonCaptor.capture())(any())
      jsonCaptor.getValue must containJson(schemeJson(
        twoRacDacs.length, pagination, pageNumber, numberOfPages,
        Some("Showing 2 to 2 of 2 schemes"), Some(tableForRacDac.copy(rows = racDacRows.tail))))
    }
  }
}

object BulkRacDacServiceSpec extends SpecBase  with BeforeAndAfterEach {
  implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global
  private val pstr1: String = "10000678RE"
  private val pstr2: String = "10000678RD"

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
    Cell(msg"messages__listSchemes__column_racDacName"),
    Cell(msg"messages__listSchemes__column_pstr"),
    Cell(msg"messages__listSchemes__column_regDate")
  )

  private val tableForScheme = Table(head,
    List(Seq(
      Cell(Literal("scheme-1"), Seq("govuk-!-width-one-half")),
      Cell(Literal(pstr1), Seq("govuk-!-width-one-quarter")),
      Cell(Literal("12 October 2000"), Seq("govuk-!-width-one-quarter")))
    ),
    attributes = Map("role" -> "table"))

  val racDacRows = List(Seq(
    Cell(Literal("scheme-1"), Seq("govuk-!-width-one-half")),
    Cell(Literal(pstr1), Seq("govuk-!-width-one-quarter")),
    Cell(Literal("12 December 1989"), Seq("govuk-!-width-one-quarter"))),
    Seq(
    Cell(Literal("scheme-2"), Seq("govuk-!-width-one-half")),
      Cell(Literal(pstr2), Seq("govuk-!-width-one-quarter")),
      Cell(Literal("12 October 2000"), Seq("govuk-!-width-one-quarter")))
  )

  val tableForRacDac: Table = Table(head, racDacRows, attributes = Map("role" -> "table"))
}

