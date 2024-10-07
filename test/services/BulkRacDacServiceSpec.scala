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
import config.AppConfig
import forms.racdac.RacDacBulkListFormProvider
import matchers.JsonMatchers
import models._
import models.requests.{AuthenticatedRequest, BulkDataRequest}
import org.mockito.MockitoSugar
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import play.api.data.Form
import play.api.http.Status._
import play.api.mvc.{AnyContent, Request}
import play.api.test.Helpers.{defaultAwaitTimeout, redirectLocation, status}
import renderer.Renderer
import uk.gov.hmrc.domain.PsaId
import uk.gov.hmrc.govukfrontend.views.Aliases.{Table, Text}
import uk.gov.hmrc.govukfrontend.views.viewmodels.table.{HeadCell, TableRow}
import uk.gov.hmrc.nunjucks.NunjucksSupport
import uk.gov.hmrc.viewmodels.{MessageInterpolators, Radios}
import utils.Data._
import utils.TwirlMigration
import views.html.racdac.RacDacsBulkListView

import scala.concurrent.{ExecutionContext, Future}

class BulkRacDacServiceSpec extends SpecBase
  with BeforeAndAfterEach
  with ScalaFutures
  with NunjucksSupport
  with MockitoSugar
  with JsonMatchers {

  import BulkRacDacServiceSpec._

  private val paginationService = new PaginationService(mockAppConfig)
  private val pagination: Int = 10
  private val formProvider: RacDacBulkListFormProvider = new RacDacBulkListFormProvider()

  private def form: Form[Boolean] = formProvider()
  private val dummyUrl = "dummyurl"

  private def getView(req: Request[_], numberOfSchemes: Int, pagination: Int, pageNumber: Int, numberOfPages: Int, paginationText:String,
                      schemeTable: Table) = {
    app.injector.instanceOf[RacDacsBulkListView].apply(
      form,
      controllers.racdac.bulk.routes.BulkListController.onSubmit,
      schemeTable,
      numberOfSchemes,
      pagination,
      paginationText,
      pageNumber,
      numberOfPages,
      paginationService.pageNumberLinks(
        pageNumber,
        numberOfSchemes,
        pagination,
        numberOfPages
      ),
      "test company",
      dummyUrl,
      TwirlMigration.toTwirlRadios(Radios.yesNo(form("value")))
    )(req, implicitly)
  }

  private val service = new BulkRacDacService(mockAppConfig, paginationService, app.injector.instanceOf[RacDacsBulkListView])
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
  }

  "mapToTable" must {

    "return correct table of scheme details" in {
      val head = Seq(
        HeadCell(Text(msg"messages__listSchemes__column_racDacName".resolve)),
        HeadCell(Text(msg"messages__listSchemes__column_pstr".resolve)),
        HeadCell(Text(msg"messages__listSchemes__column_regDate".resolve))
      )

      val rows = List(Seq(
        TableRow(Text("scheme-2"), classes = "govuk-!-width-one-half"),
        TableRow(Text(pstr2), classes = "govuk-!-width-one-quarter"),
        TableRow(Text("12 October 2000"), classes = "govuk-!-width-one-quarter")))

      val table = Table(rows, Some(head), attributes = Map("role" -> "table"))

      service.mapToTable(fullSchemes.tail) mustBe table

    }
  }

  "renderRacDacBulkView" must {
    "return OK and the correct view when there are no schemes" in {
      val numberOfPages = paginationService.divide(0, pagination)
      val bulkDataRequest: BulkDataRequest[AnyContent] = BulkDataRequest(authReq, minPSA, List.empty)

      val result = service.renderRacDacBulkView(form, 1)(bulkDataRequest, implicitly)

      result.header.status mustBe OK

      compareResultAndView(Future.successful(result),
        getView(
          bulkDataRequest,
          0, pagination, pageNumber = 1, numberOfPages,
          messages("messages__listSchemes__pagination__text", 1, 10, 0), Table(head = Some(head), attributes = Map("role" -> "table"))
        )
      )
    }

    "rlsFlag is true and deceasedFlag is false return redirect to update contact page" in {
      val bulkDataRequest: BulkDataRequest[AnyContent] = BulkDataRequest(authReq, minPSA.copy(rlsFlag = true), listOfSchemes)
      val result = Future.successful(service.renderRacDacBulkView(form, 1)(bulkDataRequest, implicitly))

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(dummyUrl)
    }

    "rlsFlag is true and deceasedFlag is true return redirect to contact hmrc page" in {
      val bulkDataRequest: BulkDataRequest[AnyContent] = BulkDataRequest(authReq,
        minPSA.copy(rlsFlag = true, deceasedFlag = true), listOfSchemes)
      val result = Future.successful(service.renderRacDacBulkView(form, 1)(bulkDataRequest, implicitly))
      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(dummyUrl)
    }

    "return OK and the correct view when there are schemes with pagination" in {
      val bulkDataRequest: BulkDataRequest[AnyContent] = BulkDataRequest(authReq, minPSA, twoRacDacs)
      val pageNumber: Int = 1
      val pagination: Int = 1
      val numberOfSchemes: Int = fullSchemes.length

      val numberOfPages = paginationService.divide(numberOfSchemes, pagination)

      when(mockAppConfig.listSchemePagination) thenReturn pagination

      val result = service.renderRacDacBulkView(form, 1)(bulkDataRequest, implicitly)

      result.header.status mustBe OK
      compareResultAndView(
        Future.successful(result),
        getView(
          bulkDataRequest,
          twoRacDacs.length, pagination, pageNumber, numberOfPages,
          "Showing 1 to 1 of 2 schemes", tableForRacDac.copy(rows = List(racDacRows.head))
        )
      )
    }

    "return OK and the correct view when using page number" in {
      val pageNumber: Int = 2

      val pagination: Int = 1

      val numberOfSchemes: Int = twoRacDacs.length

      val numberOfPages = paginationService.divide(numberOfSchemes, pagination)

      when(mockAppConfig.listSchemePagination) thenReturn pagination

      val bulkDataRequest: BulkDataRequest[AnyContent] = BulkDataRequest(authReq, minPSA, twoRacDacs)
      val result = service.renderRacDacBulkView(form, pageNumber)(bulkDataRequest, implicitly)
      result.header.status mustBe OK
      compareResultAndView(
        Future.successful(result),
        getView(
          bulkDataRequest,
          twoRacDacs.length, pagination, pageNumber, numberOfPages,
          "Showing 2 to 2 of 2 schemes", tableForRacDac.copy(rows = racDacRows.tail)
        )
      )
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
    HeadCell(Text(msg"messages__listSchemes__column_racDacName".resolve)),
    HeadCell(Text(msg"messages__listSchemes__column_pstr".resolve)),
    HeadCell(Text(msg"messages__listSchemes__column_regDate".resolve))
  )

  val racDacRows: Seq[Seq[TableRow]] = List(Seq(
    TableRow(Text("scheme-1"), classes = "govuk-!-width-one-half"),
    TableRow(Text(pstr1), classes = "govuk-!-width-one-quarter"),
    TableRow(Text("12 December 1989"), classes = "govuk-!-width-one-quarter")),
    Seq(
    TableRow(Text("scheme-2"), classes = "govuk-!-width-one-half"),
      TableRow(Text(pstr2), classes = "govuk-!-width-one-quarter"),
      TableRow(Text("12 October 2000"), classes = "govuk-!-width-one-quarter"))
  )

  val tableForRacDac: Table = Table(racDacRows, Some(head), attributes = Map("role" -> "table"))
}

