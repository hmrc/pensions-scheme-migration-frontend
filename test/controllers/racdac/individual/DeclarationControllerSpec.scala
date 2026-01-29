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

package controllers.racdac.individual

import connectors.{EmailConnector, EmailSent, MinimalDetailsConnector, PensionsSchemeConnector}
import controllers.ControllerSpecBase
import controllers.actions.MutableFakeDataRetrievalAction
import matchers.JsonMatchers
import models.MinPSA
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.*
import play.api.Application
import play.api.http.Status
import play.api.inject.bind
import play.api.inject.guice.GuiceableModule
import play.api.libs.json.JsString
import play.api.mvc.Request
import play.api.test.Helpers.*
import uk.gov.hmrc.http.HttpErrorFunctions.upstreamResponseMessage
import uk.gov.hmrc.http.UpstreamErrorResponse
import utils.Data.{psaName, pstr, ua}
import utils.Enumerable

import scala.concurrent.Future
class DeclarationControllerSpec extends ControllerSpecBase with JsonMatchers with Enumerable.Implicits {
  

  private val mutableFakeDataRetrievalAction: MutableFakeDataRetrievalAction = new MutableFakeDataRetrievalAction()
  private val mockPensionsSchemeConnector:PensionsSchemeConnector = mock[PensionsSchemeConnector]

  val extraModules: Seq[GuiceableModule] = Seq(
    bind[EmailConnector].toInstance(mockEmailConnector),
    bind[MinimalDetailsConnector].toInstance(mockMinimalDetailsConnector),
    bind[PensionsSchemeConnector].toInstance(mockPensionsSchemeConnector)
  )

  override def fakeApplication(): Application = applicationBuilderMutableRetrievalAction(mutableFakeDataRetrievalAction, extraModules).build()

  private def httpPathGET: String = controllers.racdac.individual.routes.DeclarationController.onPageLoad.url
  private def httpPathPOST: String = controllers.racdac.individual.routes.DeclarationController.onSubmit.url

  private def getView(request: Request[?]) = app.injector.instanceOf[views.html.racdac.DeclarationView].apply(
    routes.DeclarationController.onSubmit,
    controllers.routes.PensionSchemeRedirectController.onPageLoad.url,
    psaName
  )(request, implicitly)

  private val minPSA = MinPSA("test@test.com", false, Some("test company"), None, false, false)

  override def beforeEach(): Unit = {
    super.beforeEach()
    mutableFakeDataRetrievalAction.setDataToReturn(Some(ua))
    when(mockMinimalDetailsConnector.getPSADetails(any())(any(), any())).thenReturn(Future.successful(minPSA))
  }


  "DeclarationController" must {

    "RacDac Individual DeclarationController" must {

      "return OK and the correct view for a GET" in {

        when(mockMinimalDetailsConnector.getPSAName(any(), any())).thenReturn(Future.successful(psaName))
        val req = httpGETRequest(httpPathGET)
        val result = route(app, req).value
        status(result) mustEqual OK
        compareResultAndView(
          result,
          getView(req)
        )
      }

      "redirect to next page when rac dac schemes exist" in {
        when(mockPensionsSchemeConnector.registerScheme(any(),any(), any())(any(),any())).thenReturn(Future.successful(Right(JsString(pstr))))
        when(mockEmailConnector.sendEmail(any(), any(), any(), any())(any(), any())).thenReturn(Future(EmailSent))
        val result = route(app, httpPOSTRequest(httpPathPOST, Map("value" -> Seq("false")))).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.racdac.individual.routes.ConfirmationController.onPageLoad.url)
      }
    }
    "redirect to your action was not processed page when backend returns 5XX" in {
      when(mockPensionsSchemeConnector.registerScheme(any(),any(), any())(any(),any())).thenReturn(Future.failed(
        UpstreamErrorResponse(upstreamResponseMessage("POST", "url",
          Status.INTERNAL_SERVER_ERROR, "response.body"), Status.INTERNAL_SERVER_ERROR, Status.INTERNAL_SERVER_ERROR)))
      when(mockEmailConnector.sendEmail(any(), any(), any(), any())(any(), any())).thenReturn(Future(EmailSent))
      val result = route(app, httpPOSTRequest(httpPathPOST, Map("value" -> Seq("false")))).value

      status(result) mustEqual SEE_OTHER
      redirectLocation(result) mustBe Some(controllers.routes.YourActionWasNotProcessedController.onPageLoadRacDac.url)
    }
    "direct to adding rac/dac page when backend returns 422" in {
      when(mockPensionsSchemeConnector.registerScheme(any(),any(), any())(any(),any())).thenReturn(Future.failed(
        UpstreamErrorResponse(upstreamResponseMessage("POST", "url",
          Status.UNPROCESSABLE_ENTITY, "response.body"), Status.UNPROCESSABLE_ENTITY, Status.UNPROCESSABLE_ENTITY)))
      when(mockEmailConnector.sendEmail(any(), any(), any(), any())(any(), any())).thenReturn(Future(EmailSent))
      val result = route(app, httpPOSTRequest(httpPathPOST, Map("value" -> Seq("false")))).value

      status(result) mustEqual SEE_OTHER
      redirectLocation(result) mustBe Some(controllers.racdac.individual.routes.AddingRacDacController.onPageLoad.url)
    }
  }
}
