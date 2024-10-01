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

package controllers.preMigration

import connectors.ListOfSchemesConnector
import controllers.ControllerSpecBase
import controllers.actions._
import matchers.JsonMatchers
import models.{Items, ListOfLegacySchemes, RacDac, Scheme}
import org.mockito.ArgumentMatchers.any
import org.scalatest.TryValues
import play.api.mvc.Result
import play.api.test.Helpers.{status, _}
import uk.gov.hmrc.viewmodels.NunjucksSupport
import views.html.preMigration.CannotAddView

import scala.concurrent.Future
class CannotAddControllerSpec extends ControllerSpecBase with NunjucksSupport with JsonMatchers with TryValues  {

  private val mockListOfSchemesConnector:ListOfSchemesConnector= mock[ListOfSchemesConnector]
  private val schemeDetail = Items("10000678RE", "2020-10-10", racDac = false, "abcdefghi", "2020-12-12", None)
  private val racDacDetail = Items("10000678RF", "2020-10-10", racDac = true, "abcdefghi", "2020-12-12", Some("12345678"))

  private val expectedResponse = ListOfLegacySchemes(1, Some(List(schemeDetail, racDacDetail)))
  private val expectedResponseWithRacOnly = ListOfLegacySchemes(1, Some(List(racDacDetail)))
  private val expectedResponseWithPensionOnly = ListOfLegacySchemes(1, Some(List(schemeDetail)))
  private val expectedResponseWithEmpty = ListOfLegacySchemes(1, None)

  private def controller(): CannotAddController =
    new CannotAddController(
      appConfig,
      messagesApi,
      new FakeAuthAction(),
      controllerComponents,
      mockListOfSchemesConnector,
      app.injector.instanceOf[CannotAddView]
    )

  "CannotAddController" must {
    "return OK and the correct view for a GET for scheme" in {
      when(mockListOfSchemesConnector.getListOfSchemes(any())(any(),any())).thenReturn(Future.successful(Right(expectedResponse)))
      val result: Future[Result] = controller().onPageLoadScheme(fakeDataRequest())

      status(result) mustBe OK
      val view = app.injector.instanceOf[CannotAddView].apply(
        msg"messages__pension_scheme".resolve,
        msg"messages__scheme".resolve,
        appConfig.contactHmrcUrl,
        routes.ListOfSchemesController.onPageLoad(Scheme)
      )(fakeRequest, messages)
      compareResultAndView(result, view)
    }

    "return OK and the correct view for a GET for scheme with no pension Scheme" in {

      when(mockListOfSchemesConnector.getListOfSchemes(any())(any(),any())).thenReturn(Future.successful(Right(expectedResponseWithRacOnly)))
      val result: Future[Result] = controller().onPageLoadScheme(fakeDataRequest())
      status(result) mustEqual SEE_OTHER
      redirectLocation(result) mustBe Some(routes.NotRegisterController.onPageLoadScheme.url)
    }
    "return OK and the correct view for a GET for scheme with Empty pension Scheme" in {

      when(mockListOfSchemesConnector.getListOfSchemes(any())(any(),any())).thenReturn(Future.successful(Right(expectedResponseWithEmpty)))
      val result: Future[Result] = controller().onPageLoadScheme(fakeDataRequest())
      status(result) mustEqual SEE_OTHER
      redirectLocation(result) mustBe Some(routes.NotRegisterController.onPageLoadScheme.url)
    }

    "return OK and the correct view for a GET for rac dac" in {
      when(mockListOfSchemesConnector.getListOfSchemes(any())(any(),any())).thenReturn(Future.successful(Right(expectedResponse)))

      val result: Future[Result] = controller().onPageLoadRacDac(fakeDataRequest())

      status(result) mustBe OK
      val view = app.injector.instanceOf[CannotAddView].apply(
        msg"messages__racdac".resolve,
        msg"messages__racdac".resolve,
        appConfig.contactHmrcUrl,
        routes.ListOfSchemesController.onPageLoad(RacDac)
      )(fakeRequest, messages)
      compareResultAndView(result, view)

    }
    "return OK and the correct view for a GET for rac Dac with no rac dac" in {

      when(mockListOfSchemesConnector.getListOfSchemes(any())(any(),any())).thenReturn(Future.successful(Right(expectedResponseWithPensionOnly)))
      val result: Future[Result] = controller().onPageLoadRacDac(fakeDataRequest())
      status(result) mustEqual SEE_OTHER
      redirectLocation(result) mustBe Some(routes.NotRegisterController.onPageLoadRacDac.url)
    }
    "return OK and the correct view for a GET for rac Dac with empty list" in {

      when(mockListOfSchemesConnector.getListOfSchemes(any())(any(),any())).thenReturn(Future.successful(Right(expectedResponseWithEmpty)))
      val result: Future[Result] = controller().onPageLoadRacDac(fakeDataRequest())
      status(result) mustEqual SEE_OTHER
      redirectLocation(result) mustBe Some(routes.NotRegisterController.onPageLoadRacDac.url)
    }
  }
}
