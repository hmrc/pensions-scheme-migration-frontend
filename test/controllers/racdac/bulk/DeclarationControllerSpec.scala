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

package controllers.racdac.bulk

import connectors.EmailConnector
import connectors.cache.{BulkMigrationQueueConnector, CurrentPstrCacheConnector}
import controllers.ControllerSpecBase
import controllers.actions.{BulkDataAction, MutableFakeBulkDataAction}
import matchers.JsonMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.*
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.{GuiceApplicationBuilder, GuiceableModule}
import play.api.libs.json.Json
import play.api.test.Helpers.*
import uk.gov.hmrc.http.HttpException
import utils.Data.psaName
import utils.Enumerable
import views.html.racdac.{DeclarationView, UKResidencyDeclarationView}

import scala.concurrent.Future

class DeclarationControllerSpec extends ControllerSpecBase with JsonMatchers with Enumerable.Implicits {

  private val mockBulkMigrationConnector = mock[BulkMigrationQueueConnector]
  private val mockCurrentPstrCacheConnector = mock[CurrentPstrCacheConnector]

  private val mutableFakeBulkDataAction: MutableFakeBulkDataAction = new MutableFakeBulkDataAction(false)

  val extraModules: Seq[GuiceableModule] = Seq(
    bind[BulkMigrationQueueConnector].to(mockBulkMigrationConnector),
    bind[EmailConnector].toInstance(mockEmailConnector),
    bind[CurrentPstrCacheConnector].toInstance(mockCurrentPstrCacheConnector)
  )

  override def fakeApplication(): Application = new GuiceApplicationBuilder()
    .configure(
      "metrics.jvm" -> false,
      "metrics.enabled" -> false
    )
    .overrides(
      modules ++ extraModules ++ Seq[GuiceableModule](
        bind[BulkDataAction].toInstance(mutableFakeBulkDataAction)
      ) *
    ).build()

  private val dummyUrl = "/dummyurl"

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockAppConfig)
    reset(mockCurrentPstrCacheConnector)
    when(mockAppConfig.psaOverviewUrl).thenReturn(dummyUrl)
    when(mockMinimalDetailsConnector.getPSADetails(any())(any(), any())).thenReturn(Future.successful(psaName))
    when(mockAppConfig.podsUkResidency).thenReturn(false)
  }

  private def httpPathGET: String = controllers.racdac.bulk.routes.DeclarationController.onPageLoad.url

  private def httpPathPOST: String = controllers.racdac.bulk.routes.DeclarationController.onSubmit.url

  "onPageLoad" must {

    "return OK and the correct view for a GET" in {
      when(mockAppConfig.psaOverviewUrl).thenReturn(dummyUrl)
      val req = httpGETRequest(httpPathGET)
      val result = route(app, req).value
      status(result) mustEqual OK

      val view = app.injector.instanceOf[DeclarationView].apply(
        routes.DeclarationController.onSubmit,
        dummyUrl,
        psaName
      )(req, messages)
      compareResultAndView(result, view

      //changed to test the entire view juust checking key elements of the view
      contentAsString(result) must include("test company")
      contentAsString(result) must include("""class="govuk-link"""")
      contentAsString(result) must include("""manage-pension-schemes/overview""")
    }
    "return OK and the correct view for a GET when toggle is enabled" in {
      when(mockAppConfig.podsUkResidency).thenReturn(true)
      when(mockMinimalDetailsConnector.getPSAName(any(), any())).thenReturn(Future.successful(psaName))

      val req = httpGETRequest(httpPathGET)
      val result = route(app, req).value
      status(result) mustEqual OK

      val view = app.injector.instanceOf[UKResidencyDeclarationView].apply(
        routes.DeclarationController.onSubmit,
        dummyUrl,
        psaName
      )(req, messages)
      compareResultAndView(result, view)

      //changed to test the entire view juust checking key elements of the view
      contentAsString(result) must include("test company")
      contentAsString(result) must include("""class="govuk-link"""")
      contentAsString(result) must include("""manage-pension-schemes/overview""")
    }
  }

  "onSubmit" must {
    "redirect to next page when rac dac schemes exist" in {
      when(mockBulkMigrationConnector.pushAll(any())(any(), any())).thenReturn(Future(Json.obj()))
      when(mockCurrentPstrCacheConnector.save(any())(any(), any())).thenReturn(Future.successful(Json.obj()))
      val result = route(app, httpPOSTRequest(httpPathPOST, Map("value" -> Seq("false")))).value

      status(result) mustEqual SEE_OTHER
      redirectLocation(result) mustBe Some(controllers.racdac.bulk.routes.ProcessingRequestController.onPageLoad.url)
    }

    "redirect to Request not process page when error while push" in {
      when(mockBulkMigrationConnector.pushAll(any())(any(), any())).thenReturn(Future.failed(new HttpException("No Service", SERVICE_UNAVAILABLE)))

      val result = route(app, httpPOSTRequest(httpPathPOST, Map("value" -> Seq("false")))).value

      status(result) mustEqual SEE_OTHER
      redirectLocation(result) mustBe Some(controllers.racdac.bulk.routes.RequestNotProcessedController.onPageLoad.url)
    }
  }
}
