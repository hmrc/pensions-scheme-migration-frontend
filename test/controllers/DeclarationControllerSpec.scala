/*
 * Copyright 2026 HM Revenue & Customs
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
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.*
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceableModule
import play.api.libs.json.JsString
import play.api.test.Helpers.*
import uk.gov.hmrc.http.UnprocessableEntityException
import utils.Data.{psaName, pstr, schemeName, ua}
import utils.UserAnswers

import scala.concurrent.Future

class DeclarationControllerSpec extends ControllerSpecBase with JsonMatchers {

  private val mutableFakeDataRetrievalAction = new MutableFakeDataRetrievalAction()
  private val mockPensionsSchemeConnector = mock[PensionsSchemeConnector]

  val extraModules: Seq[GuiceableModule] = Seq(
    bind[EmailConnector].toInstance(mockEmailConnector),
    bind[MinimalDetailsConnector].toInstance(mockMinimalDetailsConnector),
    bind[PensionsSchemeConnector].toInstance(mockPensionsSchemeConnector)
  )

  override def fakeApplication(): Application =
    applicationBuilderMutableRetrievalAction(mutableFakeDataRetrievalAction, extraModules).build()

  private def httpPathGET = controllers.routes.DeclarationController.onPageLoad.url
  private def httpPathPOST = controllers.routes.DeclarationController.onSubmit.url

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockEmailConnector, mockMinimalDetailsConnector, mockPensionsSchemeConnector, mockAppConfig)
    when(mockAppConfig.podsUkResidency).thenReturn(false)
  }

  "DeclarationController GET" must {

    "render declaration view when toggle is disabled" in {
      when(mockAppConfig.podsUkResidency).thenReturn(false)
      val ua = UserAnswers().setOrException(SchemeNameId, schemeName).setOrException(WorkingKnowledgeId, true)
      mutableFakeDataRetrievalAction.setDataToReturn(Some(ua))

      val request = httpGETRequest(httpPathGET)
      val result = route(app, request).value

      val bulletsToggleFalse = Seq(
        "you understand that as scheme administrator you are responsible for discharging the functions conferred or imposed on the scheme administrator of the pension scheme by Finance Act 2004 and you intend to discharge those functions at all times, whether resident in the United Kingdom or another EU member state or non-member EEA state",
        "you will comply with all information notices issued to the scheme administrator under the Finance Act 2004 or the Finance Act 2008 and you understand that you may be liable to a penalty and the pension scheme may be de-registered if you fail to properly discharge those functions",
        "you understand that you may be liable to a penalty and the pension scheme may be de-registered if a false statement is made on this application, or in any information you provide in connection with this application, and that false statements may also lead to prosecution",
        "you are a fit and proper person to be the scheme administrator, with a working knowledge of pensions and the scheme administrator duties and liabilities",
        "you understand that where HMRC believes that if any of the persons who are the scheme administrator are not a fit and proper person, HMRC may refuse to register the scheme or, if the scheme is already registered, HMRC may de-register the scheme"
      )

      status(result) mustBe OK
      val bulletsList = Jsoup.parse(contentAsString(result))
        .select("ul.govuk-list li")
        .eachText()

      bulletsList must contain allElementsOf bulletsToggleFalse
    }

    "render UK residency declaration view when toggle is enabled" in {
      when(mockAppConfig.podsUkResidency).thenReturn(true)
      val ua = UserAnswers().setOrException(SchemeNameId, schemeName).setOrException(WorkingKnowledgeId, true)
      mutableFakeDataRetrievalAction.setDataToReturn(Some(ua))

      val request = httpGETRequest(httpPathGET)
      val result = route(app, request).value

      val bulletsToggleTrue = Seq(
        "you understand that as the scheme administrator you are responsible for discharging the functions conferred or imposed on the scheme administrator of the pension scheme by the Finance Act 2004 and you intend to discharge those functions at all times",
        "you will comply with all information notices issued to the scheme administrator under the Finance Act 2004 or the Finance Act 2008 — you understand that you may be liable to a penalty and the pension scheme may be de-registered if you fail to properly discharge those functions",
        "you understand that you may be liable to a penalty and the pension scheme may be de-registered if a false statement is made in any information you provide and that false statements may also lead to prosecution",
        "you are a fit and proper person to be the scheme administrator, with a working knowledge of pensions and the scheme administrator duties and liabilities",
        "you understand that where HMRC believes that if any of the persons who are the scheme administrator are not a fit and proper person, HMRC may refuse to register the scheme or, if the scheme is already registered, HMRC may de-register the scheme"
      )

      status(result) mustBe OK
      val bulletsList = Jsoup.parse(contentAsString(result))
        .select("ul.govuk-list li")
        .eachText()

      bulletsList must contain allElementsOf bulletsToggleTrue
    }
  }

  "DeclarationController POST" must {
    "redirect to success page after successful submission and email" in {
      mutableFakeDataRetrievalAction.setDataToReturn(Some(ua))
      when(mockAppConfig.schemeConfirmationEmailTemplateId).thenReturn("test template name")
      when(mockMinimalDetailsConnector.getPSADetails(any())(any(), any()))
        .thenReturn(Future.successful(MinPSA("test@test.com", isPsaSuspended = false, Some(psaName), None, rlsFlag = false, deceasedFlag = false)))
      when(mockPensionsSchemeConnector.registerScheme(any(), any(), any())(any(), any())).thenReturn(Future.successful(Right(JsString(pstr))))
      when(mockEmailConnector.sendEmail(any(), any(), any(), any())(any(), any())).thenReturn(Future.successful(EmailSent))

      val result = route(app, httpPOSTRequest(httpPathPOST, Map("value" -> Seq("false")))).value

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(controllers.routes.SchemeSuccessController.onPageLoad.url)
      verify(mockEmailConnector, times(1)).sendEmail(any(), any(), any(), any())(any(), any())
    }

    "redirect to 'action not processed' page on 5XX or BAD_REQUEST" in {
      mutableFakeDataRetrievalAction.setDataToReturn(Some(ua))
      when(mockPensionsSchemeConnector.registerScheme(any(), any(), any())(any(), any()))
        .thenReturn(Future.failed(new RuntimeException("backend error")))

      val result = route(app, httpPOSTRequest(httpPathPOST, Map("value" -> Seq("false")))).value

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(controllers.routes.YourActionWasNotProcessedController.onPageLoadScheme.url)
    }

    "redirect to AddingScheme page on 422 response" in {
      mutableFakeDataRetrievalAction.setDataToReturn(Some(ua))
      when(mockPensionsSchemeConnector.registerScheme(any(), any(), any())(any(), any()))
        .thenReturn(Future.successful(Left(new UnprocessableEntityException("response.body"))))

      val result = route(app, httpPOSTRequest(httpPathPOST, Map("value" -> Seq("false")))).value

      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(controllers.routes.AddingSchemeController.onPageLoad.url)
    }
  }
}