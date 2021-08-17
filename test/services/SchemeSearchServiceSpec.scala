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
import connectors.ListOfSchemesConnector
import models.{Items, ListOfLegacySchemes}
import org.mockito.Matchers
import org.mockito.Matchers.any
import org.mockito.Mockito.when
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.viewmodels.{MessageInterpolators, Table}
import uk.gov.hmrc.viewmodels.Table.Cell
import uk.gov.hmrc.viewmodels.Text.Literal
import utils.Data._
import utils.SchemeFuzzyMatcher

import scala.concurrent.{ExecutionContext, Future}

class SchemeSearchServiceSpec extends SpecBase with MockitoSugar with ScalaFutures {

  import SchemeSearchServiceSpec._

  private val mockFuzzyMatching = mock[SchemeFuzzyMatcher]
  private val mockListOfSchemesConnector = mock[ListOfSchemesConnector]
  val schemeSearchService = new SchemeSearchService(mockFuzzyMatching, mockListOfSchemesConnector)

  "search" must {

    "return correct list of scheme details with search on correct pstr" in {

      when(mockListOfSchemesConnector.getListOfSchemes(Matchers.eq(psaId))(any(), any()))
        .thenReturn(Future.successful(Right(listOfSchemes)))

      whenReady(schemeSearchService.search(psaId, Some(pstr1), isRacDac = isRacDacFalse)) { result =>
        result mustBe fullSchemes.filter(_.racDac == isRacDacFalse).filter(_.pstr equalsIgnoreCase(pstr1))
      }
    }
    "return correct list of scheme details with search on correct pstr with rac dac" in {

      when(mockListOfSchemesConnector.getListOfSchemes(Matchers.eq(psaId))(any(), any()))
        .thenReturn(Future.successful(Right(listOfSchemes)))

      whenReady(schemeSearchService.search(psaId, Some(pstr2), isRacDac = isRacDacTrue)) { result =>
        result mustBe fullSchemes.filter(_.racDac == isRacDacTrue).filter(_.pstr equalsIgnoreCase(pstr2))
      }
    }

    "return empty list for correct format pstr but no match" in {
      val emptyList = ListOfLegacySchemes(0, None)
      when(mockListOfSchemesConnector.getListOfSchemes(Matchers.eq(psaId))(any(), any()))
        .thenReturn(Future.successful(Right(emptyList)))

      whenReady(schemeSearchService.search(psaId, Some("S2400000016"), isRacDac = isRacDacFalse)) { result =>
        result mustBe Nil
      }
    }

    "return empty list for correct format pstr but no match with rac dac" in {
      val emptyList = ListOfLegacySchemes(0, None)
      when(mockListOfSchemesConnector.getListOfSchemes(Matchers.eq(psaId))(any(), any()))
        .thenReturn(Future.successful(Right(emptyList)))

      whenReady(schemeSearchService.search(psaId, Some("S2400000016"), isRacDac = isRacDacTrue)) { result =>
        result mustBe Nil
      }
    }

    "return correct list of scheme details with search on scheme name" in {
      when(mockFuzzyMatching.doFuzzyMatching(any(), any())).thenReturn(true).thenReturn(false)
      when(mockListOfSchemesConnector.getListOfSchemes(Matchers.eq(psaId))(any(), any()))
        .thenReturn(Future.successful(Right(listOfSchemes)))

      whenReady(schemeSearchService.search(psaId, Some("scheme-1"),isRacDac = isRacDacFalse)) { result =>
        result mustBe fullSchemes.filter(_.racDac == isRacDacFalse).filter(_.schemeName == "scheme-1")
      }
    }

    "return correct list of scheme details with search on scheme name with rac dac" in {
      when(mockFuzzyMatching.doFuzzyMatching(any(), any())).thenReturn(true).thenReturn(false)
      when(mockListOfSchemesConnector.getListOfSchemes(Matchers.eq(psaId))(any(), any()))
        .thenReturn(Future.successful(Right(listOfSchemes)))

      whenReady(schemeSearchService.search(psaId, Some("scheme-1"),isRacDac = isRacDacTrue)) { result =>
        result mustBe fullSchemes.filter(_.racDac == isRacDacTrue).filter(_.schemeName == "scheme-2")
      }
    }

    "return empty list when fuzzy matching fails" in {
      when(mockFuzzyMatching.doFuzzyMatching(any(), any())).thenReturn(false)
      val emptyList = ListOfLegacySchemes(0, None)
      when(mockListOfSchemesConnector.getListOfSchemes(Matchers.eq(psaId))(any(), any()))
        .thenReturn(Future.successful(Right(emptyList)))

      whenReady(schemeSearchService.search(psaId, Some("no matching"),isRacDac = isRacDacFalse)) { result =>
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

      val t=Table(head, rows,  attributes = Map("role" -> "table"))

      schemeSearchService.mapToTable(List(fullSchemes.head), isRacDac = isRacDacFalse)  mustBe t

    }
    "return correct table of scheme details with rac dac" in {
      val head=Seq(
        Cell(msg"messages__listSchemes__column_racDacName"),
        Cell(msg"messages__listSchemes__column_pstr")
      )

      val rows= List(Seq(
        Cell(Literal("scheme-2"), Seq("govuk-!-width-one-quarter")),
        Cell(Literal(pstr2), Seq("govuk-!-width-one-quarter"))))

      val t=Table(head, rows,  attributes = Map("role" -> "table"))

      schemeSearchService.mapToTable(fullSchemes.tail, isRacDac = isRacDacTrue)  mustBe t

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

  def fullSchemes: List[Items] =
    List(
      Items(pstr1, "2020-10-10", racDac = false, "scheme-1", "1989-12-12", None),
      Items(pstr2, "2020-10-10", racDac = true, "scheme-2", "2000-10-12", Some("12345678"))
    )
}

