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

package controllers.trustees.company

import controllers.ControllerSpecBase
import controllers.actions._
import helpers.SpokeCreationService
import identifiers.trustees.company.CompanyDetailsId
import matchers.JsonMatchers
import models.CompanyDetails
import org.mockito.ArgumentMatchers.any
import org.mockito.{ArgumentMatchers, Mockito}
import org.scalatest.TryValues
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.Result
import play.api.test.Helpers.{status, _}
import play.twirl.api.Html
import renderer.Renderer
import uk.gov.hmrc.viewmodels.NunjucksSupport
import utils.Data.{schemeName, ua}
import utils.UserAnswers
import viewmodels.Message

import scala.concurrent.Future
class SpokeTaskListControllerSpec
  extends ControllerSpecBase
    with NunjucksSupport
    with JsonMatchers
    with TryValues {

  private def userAnswers(companyName: CompanyDetails): UserAnswers = ua.set(CompanyDetailsId(0), companyName).success.value
  private val mockSpokeCreationService = mock[SpokeCreationService]
  private val templateToBeRendered: String = "spokeTaskList.njk"

  private def json(company: CompanyDetails, submitUrl: String): JsObject =
    Json.obj(
      "taskSections" -> Nil,
      "entityName" -> company.companyName,
      "schemeName" -> schemeName,
      "totalSpokes" -> 0,
      "completedCount" -> 0,
      "entityType" -> Message("messages__tasklist__trustee"),
      "submitUrl" -> submitUrl,
      "config" -> Json.obj(
        "betaFeedbackUnauthenticatedUrl" -> "",
        "reportAProblemPartialUrl" -> "",
        "reportAProblemNonJSUrl" -> "",
        "psaOverviewUrl" -> "",
        "timeout" -> "",
        "countdown" -> "",
        "gtmContainerId" -> "",
        "trackingSnippetUrl" -> "",
        "nonce" -> ""
      )
    )

  private def controller(
                          dataRetrievalAction: DataRetrievalAction
                        ): SpokeTaskListController =
    new SpokeTaskListController(
      messagesApi          = messagesApi,
      authenticate         = new FakeAuthAction(),
      getData              = dataRetrievalAction,
      requireData          = new DataRequiredActionImpl,
      controllerComponents = controllerComponents,
      spokeCreationService = mockSpokeCreationService,
      renderer             = new Renderer(mockAppConfig, mockRenderer)
    )

  override def beforeEach(): Unit = {
    Mockito.reset(mockRenderer)
  }

  "Task List Controller" must {
    "return OK and the correct view for a GET if company name has not been entered" in {
      when(mockSpokeCreationService.getTrusteeCompanySpokes(any(), any(), any())(any())).thenReturn(Nil)

      val company: CompanyDetails = CompanyDetails("the company")

      val getData = new FakeDataRetrievalAction(Some(userAnswers(company)))
      val result: Future[Result] = controller(getData).onPageLoad(0)(fakeDataRequest(userAnswers(company)))
      val json = Json.parse(
          """{
            |"taskSections":[],
            |"entityName":"the company",
            |"schemeName":"Test scheme name",
            |"totalSpokes":0,
            |"completedCount":0,
            |"entityType":"Trustee",
            |"submitUrl":"/add-pension-scheme/trustee/1/company/name",
            |"config":
            |{"betaFeedbackUnauthenticatedUrl":"",
            |"reportAProblemPartialUrl":"",
            |"reportAProblemNonJSUrl":"",
            |"psaOverviewUrl":"",
            |"timeout":"",
            |"countdown":"",
            |"gtmContainerId":"",
            |"trackingSnippetUrl":"",
            |"nonce":""}}""".stripMargin)
        .as[JsObject]

      when(mockRenderer.render(ArgumentMatchers.eq(templateToBeRendered), ArgumentMatchers.eq(json))(any()))
        .thenReturn(Future.successful(Html("")))

      status(result) mustBe OK
    }
    "return OK and the correct view for a GET if company name has been entered" in {
      when(mockSpokeCreationService.getTrusteeCompanySpokes(any(), any(), any())(any())).thenReturn(Nil)
      when(mockRenderer.render(any(), any())(any())).thenReturn(Future.successful(Html("")))

      val company: CompanyDetails = CompanyDetails("test company name")

      val getData = new FakeDataRetrievalAction(Some(userAnswers(company)))
      val result: Future[Result] = controller(getData).onPageLoad(0)(fakeDataRequest(userAnswers(company)))

      val submitUrl = controllers.trustees.routes.AddTrusteeController.onPageLoad.url

      val expectedJson = json(company, submitUrl)

      when(mockRenderer.render(ArgumentMatchers.eq(templateToBeRendered), ArgumentMatchers.eq(expectedJson))(any()))
        .thenReturn(Future.successful(Html("")))

      status(result) mustBe OK
    }
  }
}
