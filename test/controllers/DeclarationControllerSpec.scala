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

package controllers

import connectors.{EmailConnector, EmailSent, MinimalDetailsConnector, PensionsSchemeConnector}
import controllers.actions.MutableFakeDataRetrievalAction
import identifiers.beforeYouStart.{SchemeNameId, WorkingKnowledgeId}
import matchers.JsonMatchers
import models.MinPSA
import org.apache.commons.lang3.StringUtils
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import play.api.Application
import play.api.http.Status
import play.api.inject.bind
import play.api.inject.guice.GuiceableModule
import play.api.test.Helpers._
import uk.gov.hmrc.http.HttpErrorFunctions.upstreamResponseMessage
import uk.gov.hmrc.http.UpstreamErrorResponse
import utils.Data.{psaName, pstr, schemeName, ua}
import utils.{Enumerable, UserAnswers}
import views.html.DeclarationView

import scala.concurrent.Future

class DeclarationControllerSpec extends ControllerSpecBase with JsonMatchers with Enumerable.Implicits {

  private val mutableFakeDataRetrievalAction: MutableFakeDataRetrievalAction = new MutableFakeDataRetrievalAction()
  private val mockPensionsSchemeConnector: PensionsSchemeConnector = mock[PensionsSchemeConnector]
  val extraModules: Seq[GuiceableModule] = Seq(
    bind[EmailConnector].toInstance(mockEmailConnector),
    bind[MinimalDetailsConnector].toInstance(mockMinimalDetailsConnector),
    bind[PensionsSchemeConnector].toInstance(mockPensionsSchemeConnector)
  )

  override def fakeApplication(): Application = applicationBuilderMutableRetrievalAction(mutableFakeDataRetrievalAction, extraModules).build()

  private def httpPathGET: String = controllers.routes.DeclarationController.onPageLoad.url

  private def httpPathPOST: String = controllers.routes.DeclarationController.onSubmit.url

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockEmailConnector)
    reset(mockMinimalDetailsConnector)
    reset(mockPensionsSchemeConnector)
    reset(mockAppConfig)
  }

  "DeclarationController" must {

    "return OK with WorkingKnowledgeId true and the correct view for a GET" in {
      val ua: UserAnswers = UserAnswers()
        .setOrException(SchemeNameId, schemeName)
        .setOrException(WorkingKnowledgeId, true)
      mutableFakeDataRetrievalAction.setDataToReturn(Some(ua))
      val request = httpGETRequest(httpPathGET)
      val result = route(app, request).value
      status(result) mustEqual OK

      val view = app.injector.instanceOf[DeclarationView].apply(
        schemeName,
        true,
        true,
        routes.DeclarationController.onSubmit
      )(request, messages)
      compareResultAndView(result, view)
    }

    "return OK with WorkingKnowledgeId false and the correct view for a GET" in {
      val ua: UserAnswers = UserAnswers()
        .setOrException(SchemeNameId, schemeName)
        .setOrException(WorkingKnowledgeId, false)

      mutableFakeDataRetrievalAction.setDataToReturn(Some(ua))
      val request = httpGETRequest(httpPathGET)
      val result = route(app, request).value

      status(result) mustEqual OK

      val view = app.injector.instanceOf[DeclarationView].apply(
        schemeName,
        true,
        false,
        routes.DeclarationController.onSubmit
      )(request, messages)
      compareResultAndView(result, view)
    }

    "redirect to next page when button is clicked" in {

      mutableFakeDataRetrievalAction.setDataToReturn(Some(ua))
      when(mockAppConfig.schemeConfirmationEmailTemplateId).thenReturn("test template name")
      when(mockMinimalDetailsConnector.getPSADetails(any())(any(), any()))
        .thenReturn(Future.successful(MinPSA("test@test.com", isPsaSuspended = false, Some(psaName), None, rlsFlag = false, deceasedFlag = false)))
      when(mockPensionsSchemeConnector.registerScheme(any(), any(), any())(any(), any())).thenReturn(Future.successful(pstr))
      when(mockEmailConnector.sendEmail(any(), any(), any(), any())(any(), any())).thenReturn(Future.successful(EmailSent))

      val result = route(app, httpPOSTRequest(httpPathPOST, Map("value" -> Seq("false")))).value

      status(result) mustEqual SEE_OTHER

      verify(mockEmailConnector, times(1)).sendEmail(
        ArgumentMatchers.eq("test@test.com"),
        ArgumentMatchers.eq("pods_scheme_migration_confirmation"),
        ArgumentMatchers.eq(Map("psaName" -> psaName, "schemeName" -> schemeName)),
        any())(any(), any())

      redirectLocation(result) mustBe Some(controllers.routes.SchemeSuccessController.onPageLoad.url)
    }

    "redirect to next page when button is clicked more than one (declaration is already submitted)" in {

      mutableFakeDataRetrievalAction.setDataToReturn(Some(ua))
      when(mockPensionsSchemeConnector.registerScheme(any(), any(), any())(any(), any())).thenReturn(Future.successful(StringUtils.EMPTY))

      val result = route(app, httpPOSTRequest(httpPathPOST, Map("value" -> Seq("false")))).value

      status(result) mustEqual SEE_OTHER

      verify(mockEmailConnector, never).sendEmail(any(), any(), any(), any())(any(), any())
      verify(mockMinimalDetailsConnector, never).getPSADetails(any())(any(), any())
      verify(mockAppConfig, never).schemeConfirmationEmailTemplateId

      redirectLocation(result) mustBe Some(controllers.routes.SchemeSuccessController.onPageLoad.url)
    }

    "redirect to your action was not processed page when backend returns 5XX" in {

      mutableFakeDataRetrievalAction.setDataToReturn(Some(ua))
      when(mockAppConfig.schemeConfirmationEmailTemplateId).thenReturn("test template name")
      when(mockMinimalDetailsConnector.getPSADetails(any())(any(), any()))
        .thenReturn(Future.successful(MinPSA("test@test.com", isPsaSuspended = false, Some(psaName), None, rlsFlag = false, deceasedFlag = false)))
      when(mockPensionsSchemeConnector.registerScheme(any(), any(), any())(any(), any())).thenReturn(Future.failed(
        UpstreamErrorResponse(upstreamResponseMessage("POST", "url",
          Status.INTERNAL_SERVER_ERROR, "response.body"), Status.INTERNAL_SERVER_ERROR, Status.INTERNAL_SERVER_ERROR)))

      val result = route(app, httpPOSTRequest(httpPathPOST, Map("value" -> Seq("false")))).value

      status(result) mustEqual SEE_OTHER
      redirectLocation(result) mustBe Some(controllers.routes.YourActionWasNotProcessedController.onPageLoadScheme.url)
    }
    "redirect to task list page when backend returns Error" in {

      mutableFakeDataRetrievalAction.setDataToReturn(Some(ua))
      when(mockAppConfig.schemeConfirmationEmailTemplateId).thenReturn("test template name")
      when(mockMinimalDetailsConnector.getPSADetails(any())(any(), any()))
        .thenReturn(Future.successful(MinPSA("test@test.com", isPsaSuspended = false, Some(psaName), None, rlsFlag = false, deceasedFlag = false)))
      when(mockPensionsSchemeConnector.registerScheme(any(), any(), any())(any(), any())).thenReturn(Future.failed(
        UpstreamErrorResponse(upstreamResponseMessage("POST", "url",
          Status.BAD_REQUEST, "response.body"), Status.BAD_REQUEST, Status.BAD_REQUEST)))

      val result = route(app, httpPOSTRequest(httpPathPOST, Map("value" -> Seq("false")))).value

      status(result) mustEqual SEE_OTHER
      redirectLocation(result) mustBe Some(controllers.routes.YourActionWasNotProcessedController.onPageLoadScheme.url)
    }

    "directs to correct page if 422 response is returned" in {

      mutableFakeDataRetrievalAction.setDataToReturn(Some(ua))
      when(mockAppConfig.schemeConfirmationEmailTemplateId).thenReturn("test template name")
      when(mockMinimalDetailsConnector.getPSADetails(any())(any(), any()))
        .thenReturn(Future.successful(MinPSA("test@test.com", isPsaSuspended = false, Some(psaName), None, rlsFlag = false, deceasedFlag = false)))
      when(mockPensionsSchemeConnector.registerScheme(any(), any(), any())(any(), any())).thenReturn(Future.failed(
        UpstreamErrorResponse(upstreamResponseMessage("POST", "url",
          Status.UNPROCESSABLE_ENTITY, "response.body"), Status.UNPROCESSABLE_ENTITY, Status.UNPROCESSABLE_ENTITY)))

      val result = route(app, httpPOSTRequest(httpPathPOST, Map("value" -> Seq("false")))).value

      status(result) mustEqual SEE_OTHER
      redirectLocation(result) mustBe Some(controllers.routes.AddingSchemeController.onPageLoad.url)
    }

  }
}
