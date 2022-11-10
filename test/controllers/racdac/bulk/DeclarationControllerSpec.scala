/*
 * Copyright 2022 HM Revenue & Customs
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
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.{GuiceApplicationBuilder, GuiceableModule}
import play.api.libs.json.{JsObject, Json}
import play.api.test.Helpers._
import play.twirl.api.Html
import uk.gov.hmrc.http.HttpException
import uk.gov.hmrc.nunjucks.NunjucksSupport
import utils.Enumerable

import scala.concurrent.Future

class DeclarationControllerSpec extends ControllerSpecBase with NunjucksSupport with JsonMatchers with Enumerable.Implicits {

  private val templateToBeRendered = "racdac/declaration.njk"
  private val mockBulkMigrationConnector = mock[BulkMigrationQueueConnector]
  private val mockCurrentPstrCacheConnector = mock[CurrentPstrCacheConnector]

  private val mutableFakeBulkDataAction: MutableFakeBulkDataAction = new MutableFakeBulkDataAction(false)
  val extraModules: Seq[GuiceableModule] = Seq(
    bind[BulkMigrationQueueConnector].to(mockBulkMigrationConnector),
    bind[EmailConnector].toInstance(mockEmailConnector),
    bind[CurrentPstrCacheConnector].toInstance(mockCurrentPstrCacheConnector)
  )
  private val application: Application = new GuiceApplicationBuilder()
    .configure(
      "metrics.jvm" -> false,
      "metrics.enabled" -> false
    )
    .overrides(
      modules ++ extraModules ++ Seq[GuiceableModule](
        bind[BulkDataAction].toInstance(mutableFakeBulkDataAction)
      ): _*
    ).build()
  private val dummyUrl = "/dummyurl"

  private def jsonToPassToTemplate: JsObject =
    Json.obj(
      "psaName" -> "test company",
      "submitUrl" -> routes.DeclarationController.onSubmit.url,
      "returnUrl" -> dummyUrl
    )

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockCurrentPstrCacheConnector)
    when(mockAppConfig.psaOverviewUrl) thenReturn dummyUrl
    when(mockRenderer.render(any(), any())(any())).thenReturn(Future.successful(Html("")))
  }

  private def httpPathGET: String = controllers.racdac.bulk.routes.DeclarationController.onPageLoad.url

  private def httpPathPOST: String = controllers.racdac.bulk.routes.DeclarationController.onSubmit.url

  "onPageLoad" must {

    "return OK and the correct view for a GET" in {
      val templateCaptor: ArgumentCaptor[String] = ArgumentCaptor.forClass(classOf[String])
      val jsonCaptor: ArgumentCaptor[JsObject] = ArgumentCaptor.forClass(classOf[JsObject])
      val result = route(application, httpGETRequest(httpPathGET)).value
      status(result) mustEqual OK

      verify(mockRenderer, times(1)).render(templateCaptor.capture(), jsonCaptor.capture())(any())
      templateCaptor.getValue mustEqual templateToBeRendered

      jsonCaptor.getValue must containJson(jsonToPassToTemplate)
    }
  }

  "onSubmit" must {
    "redirect to next page when rac dac schemes exist" in {
      when(mockBulkMigrationConnector.pushAll(any(), any())(any(), any())).thenReturn(Future(Json.obj()))
      when(mockCurrentPstrCacheConnector.save(any())(any(), any())).thenReturn(Future.successful(Json.obj()))
      val result = route(application, httpPOSTRequest(httpPathPOST, Map("value" -> Seq("false")))).value

      status(result) mustEqual SEE_OTHER
      redirectLocation(result) mustBe Some(controllers.racdac.bulk.routes.ProcessingRequestController.onPageLoad.url)
    }

    "redirect to Request not process page when error while push" in {
      when(mockBulkMigrationConnector.pushAll(any(), any())(any(), any())).thenReturn(Future.failed(new HttpException("No Service", SERVICE_UNAVAILABLE)))

      val result = route(application, httpPOSTRequest(httpPathPOST, Map("value" -> Seq("false")))).value

      status(result) mustEqual SEE_OTHER
      redirectLocation(result) mustBe Some(controllers.racdac.bulk.routes.RequestNotProcessedController.onPageLoad.url)
    }
  }
}
