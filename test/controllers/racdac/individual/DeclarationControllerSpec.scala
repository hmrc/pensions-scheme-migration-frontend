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

import config.AppConfig
import connectors.{EmailConnector, EmailSent, MinimalDetailsConnector, PensionsSchemeConnector}
import controllers.ControllerSpecBase
import controllers.actions.MutableFakeDataRetrievalAction
import matchers.JsonMatchers
import models.MinPSA
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.*
import play.api.Application
import play.api.http.Status
import play.api.inject.bind
import play.api.inject.guice.GuiceableModule
import play.api.libs.json.JsString
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
    bind[PensionsSchemeConnector].toInstance(mockPensionsSchemeConnector),
    bind[AppConfig].toInstance(mockAppConfig)
  )

  override def fakeApplication(): Application = applicationBuilderMutableRetrievalAction(mutableFakeDataRetrievalAction, extraModules).build()

  private def httpPathGET: String = controllers.racdac.individual.routes.DeclarationController.onPageLoad.url
  private def httpPathPOST: String = controllers.racdac.individual.routes.DeclarationController.onSubmit.url

  private val minPSA = MinPSA("test@test.com", false, Some("test company"), None, false, false)

  override def beforeEach(): Unit = {
    super.beforeEach()
    mutableFakeDataRetrievalAction.setDataToReturn(Some(ua))
    when(mockMinimalDetailsConnector.getPSADetails(any())(any(), any())).thenReturn(Future.successful(minPSA))
    when(mockAppConfig.podsUkResidency).thenReturn(false)
  }

  "DeclarationController" must {

    "RacDac Individual DeclarationController" must {

      "return OK and render the correct content" in {
        when(mockAppConfig.podsUkResidency).thenReturn(false)
        mutableFakeDataRetrievalAction.setDataToReturn(Some(ua))
        when(mockMinimalDetailsConnector.getPSAName(any(), any())).thenReturn(Future.successful(psaName))

        val request = httpGETRequest(httpPathGET)
        val result = route(app, request).value

        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))
        doc.select("h1.govuk-heading-l").text() mustBe "Declaration"
        val bullets = doc.select("ul.govuk-list li").eachText()

        bullets.get(0) mustBe "you understand that as the scheme administrator you are responsible for discharging the functions conferred or imposed on the scheme administrator of the pension scheme by the Finance Act 2004 and you intend to discharge those functions at all times, whether resident in the United Kingdom, or another EU member state or non-member EEA state"
        bullets.get(1) mustBe "you will comply with all information notices issued to the scheme administrator under the Finance Act 2004 or the Finance Act 2008. You understand that you may be liable to a penalty and the pension scheme may be de-registered if you fail to properly discharge those functions"
        bullets.get(2) mustBe "you understand that you may be liable to a penalty and the pension scheme may be de-registered if a false statement is made in any information you provide and that false statements may also lead to prosecution."
      }

      "return OK and render the correct content when toggle is enabled" in {
        when(mockAppConfig.podsUkResidency).thenReturn(true)
        mutableFakeDataRetrievalAction.setDataToReturn(Some(ua))
        when(mockMinimalDetailsConnector.getPSAName(any(), any())).thenReturn(Future.successful(psaName))

        val request = httpGETRequest(httpPathGET)
        val result = route(app, request).value

        status(result) mustBe OK

        val doc = Jsoup.parse(contentAsString(result))
        doc.select("h1.govuk-heading-l").text() mustBe "Declaration"

        val bullets = doc.select("ul.govuk-list li").eachText()

        bullets.get(0) mustBe "you understand that as the scheme administrator you are responsible for discharging the functions conferred or imposed on the scheme administrator of the pension scheme by the Finance Act 2004 and you intend to discharge those functions at all times"
        bullets.get(1) mustBe "you will comply with all information notices issued to the scheme administrator under the Finance Act 2004 or the Finance Act 2008 — you understand that you may be liable to a penalty and the pension scheme may be de-registered if you fail to properly discharge those functions"
        bullets.get(2) mustBe "you understand that you may be liable to a penalty and the pension scheme may be de-registered if a false statement is made in any information you provide and that false statements may also lead to prosecution"
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
