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

package controllers

import connectors.{EmailConnector, EmailSent, MinimalDetailsConnector, PensionsSchemeConnector}
import controllers.actions.MutableFakeDataRetrievalAction
import identifiers.beforeYouStart.{SchemeNameId, WorkingKnowledgeId}
import matchers.JsonMatchers
import models.MinPSA
import org.mockito.ArgumentMatchers.any
import org.mockito.{ArgumentCaptor, ArgumentMatchers}
import play.api.Application
import play.api.http.Status
import play.api.inject.bind
import play.api.inject.guice.GuiceableModule
import play.api.libs.json.{JsObject, Json}
import play.api.test.Helpers._
import play.twirl.api.Html
import uk.gov.hmrc.http.HttpReads.upstreamResponseMessage
import uk.gov.hmrc.http.UpstreamErrorResponse
import uk.gov.hmrc.nunjucks.NunjucksSupport
import utils.Data.{psaName, schemeName, ua}
import utils.{Enumerable, UserAnswers}
import utils.Data.{psaName, pstr, schemeName, ua}
import utils.Enumerable

import scala.concurrent.Future

class DeclarationControllerSpec extends ControllerSpecBase with NunjucksSupport with JsonMatchers with Enumerable.Implicits {

  private val templateToBeRendered = "declaration.njk"

  private val mutableFakeDataRetrievalAction: MutableFakeDataRetrievalAction = new MutableFakeDataRetrievalAction()
  private val mockPensionsSchemeConnector:PensionsSchemeConnector = mock[PensionsSchemeConnector]
  val extraModules: Seq[GuiceableModule] = Seq(
    bind[EmailConnector].toInstance(mockEmailConnector),
    bind[MinimalDetailsConnector].toInstance(mockMinimalDetailsConnector),
    bind[PensionsSchemeConnector].toInstance(mockPensionsSchemeConnector)
  )

  private val application: Application = applicationBuilderMutableRetrievalAction(mutableFakeDataRetrievalAction, extraModules).build()

  private def httpPathGET: String = controllers.routes.DeclarationController.onPageLoad().url
  private def httpPathPOST: String = controllers.routes.DeclarationController.onSubmit().url

  private val jsonToPassToTemplate: JsObject =
    Json.obj(
      "schemeName" -> schemeName,
      "isCompany" -> true,
      "hasWorkingKnowledge" -> true,
      "submitUrl" -> routes.DeclarationController.onSubmit().url
    )

  override def beforeEach: Unit = {
    super.beforeEach
    when(mockRenderer.render(any(), any())(any())).thenReturn(Future.successful(Html("")))
  }


  "DeclarationController" must {

    "return OK with WorkingKnowledgeId true and the correct view for a GET" in {
      val ua: UserAnswers = UserAnswers()
        .setOrException(SchemeNameId, schemeName)
        .setOrException(WorkingKnowledgeId, true)

      mutableFakeDataRetrievalAction.setDataToReturn(Some(ua))

      val templateCaptor:ArgumentCaptor[String] = ArgumentCaptor.forClass(classOf[String])
      val jsonCaptor:ArgumentCaptor[JsObject] = ArgumentCaptor.forClass(classOf[JsObject])

      val result = route(application, httpGETRequest(httpPathGET)).value

      status(result) mustEqual OK

      verify(mockRenderer, times(1)).render(templateCaptor.capture(), jsonCaptor.capture())(any())

      templateCaptor.getValue mustEqual templateToBeRendered

      jsonCaptor.getValue must containJson(jsonToPassToTemplate)
    }

    "return OK with WorkingKnowledgeId false and the correct view for a GET" in {
      val ua: UserAnswers = UserAnswers()
        .setOrException(SchemeNameId, schemeName)
        .setOrException(WorkingKnowledgeId, false)

      val jsonToPassToTemplate: JsObject =
        Json.obj(
          "schemeName" -> schemeName,
          "isCompany" -> true,
          "hasWorkingKnowledge" -> false,
          "submitUrl" -> routes.DeclarationController.onSubmit().url
        )
      mutableFakeDataRetrievalAction.setDataToReturn(Some(ua))

      val templateCaptor:ArgumentCaptor[String] = ArgumentCaptor.forClass(classOf[String])
      val jsonCaptor:ArgumentCaptor[JsObject] = ArgumentCaptor.forClass(classOf[JsObject])

      val result = route(application, httpGETRequest(httpPathGET)).value

      status(result) mustEqual OK

      verify(mockRenderer, times(1)).render(templateCaptor.capture(), jsonCaptor.capture())(any())

      templateCaptor.getValue mustEqual templateToBeRendered

      jsonCaptor.getValue must containJson(jsonToPassToTemplate)
    }

    "redirect to next page when button is clicked" in {

      mutableFakeDataRetrievalAction.setDataToReturn(Some(ua))
      when(mockAppConfig.schemeConfirmationEmailTemplateId).thenReturn("test template name")
      when(mockMinimalDetailsConnector.getPSADetails(any())(any(), any()))
        .thenReturn(Future.successful(MinPSA("test@test.com", isPsaSuspended = false, Some(psaName), None, rlsFlag = false, deceasedFlag = false)))
      when(mockPensionsSchemeConnector.registerScheme(any(),any(), any())(any(),any())).thenReturn(Future.successful(pstr))
      when(mockEmailConnector.sendEmail(any(), any(), any(), any())(any(),any())).thenReturn(Future.successful(EmailSent))

      val result = route(application, httpPOSTRequest(httpPathPOST, Map("value" -> Seq("false")))).value

      status(result) mustEqual SEE_OTHER

      verify(mockEmailConnector, times(1)).sendEmail(
        ArgumentMatchers.eq("test@test.com"),
        ArgumentMatchers.eq("test template name"),
        ArgumentMatchers.eq(Map("psaName" -> psaName.toString, "schemeName"-> schemeName)),
        any())(any(), any())

      redirectLocation(result) mustBe Some(controllers.routes.SchemeSuccessController.onPageLoad().url)
    }

    "redirect to your action was not processed page when backend returns 5XX" in {

      mutableFakeDataRetrievalAction.setDataToReturn(Some(ua))
      when(mockAppConfig.schemeConfirmationEmailTemplateId).thenReturn("test template name")
      when(mockMinimalDetailsConnector.getPSADetails(any())(any(), any()))
        .thenReturn(Future.successful(MinPSA("test@test.com", isPsaSuspended = false, Some(psaName), None, rlsFlag = false, deceasedFlag = false)))
      when(mockPensionsSchemeConnector.registerScheme(any(),any(), any())(any(),any())).thenReturn(Future.failed(
        UpstreamErrorResponse(upstreamResponseMessage("POST", "url",
          Status.INTERNAL_SERVER_ERROR, "response.body"), Status.INTERNAL_SERVER_ERROR, Status.INTERNAL_SERVER_ERROR)))

      val result = route(application, httpPOSTRequest(httpPathPOST, Map("value" -> Seq("false")))).value

      status(result) mustEqual SEE_OTHER
      redirectLocation(result) mustBe Some(controllers.routes.YourActionWasNotProcessedController.onPageLoadScheme.url)
    }
    "redirect to task list page when backend returns Error" in {

      mutableFakeDataRetrievalAction.setDataToReturn(Some(ua))
      when(mockAppConfig.schemeConfirmationEmailTemplateId).thenReturn("test template name")
      when(mockMinimalDetailsConnector.getPSADetails(any())(any(), any()))
        .thenReturn(Future.successful(MinPSA("test@test.com", isPsaSuspended = false, Some(psaName), None, rlsFlag = false, deceasedFlag = false)))
      when(mockPensionsSchemeConnector.registerScheme(any(),any(), any())(any(),any())).thenReturn(Future.failed(
        UpstreamErrorResponse(upstreamResponseMessage("POST", "url",
          Status.BAD_REQUEST, "response.body"), Status.BAD_REQUEST, Status.BAD_REQUEST)))

      val result = route(application, httpPOSTRequest(httpPathPOST, Map("value" -> Seq("false")))).value

      status(result) mustEqual SEE_OTHER
      redirectLocation(result) mustBe Some(controllers.routes.TaskListController.onPageLoad.url)
    }

  }
}
