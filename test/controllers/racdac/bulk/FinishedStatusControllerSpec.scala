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

import connectors.cache.BulkMigrationQueueConnector
import controllers.ControllerSpecBase
import controllers.actions.MutableFakeDataRetrievalAction
import matchers.JsonMatchers
import org.mockito.ArgumentMatchers.any
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceableModule
import play.api.mvc.Request
import play.api.test.Helpers._
import play.twirl.api.Html
import uk.gov.hmrc.nunjucks.NunjucksSupport
import utils.Enumerable

import scala.concurrent.Future
class FinishedStatusControllerSpec extends ControllerSpecBase with NunjucksSupport with JsonMatchers with Enumerable.Implicits {

  private val mockQueueConnector = mock[BulkMigrationQueueConnector]
  private val mutableFakeDataRetrievalAction: MutableFakeDataRetrievalAction = new MutableFakeDataRetrievalAction()
  private val extraModules: Seq[GuiceableModule] = Seq(
    bind[BulkMigrationQueueConnector].to(mockQueueConnector)
  )
  override def fakeApplication(): Application = applicationBuilderMutableRetrievalAction(mutableFakeDataRetrievalAction, extraModules).build()

  private def httpPathGET: String = controllers.racdac.bulk.routes.FinishedStatusController.onPageLoad.url

  private def getView(req: Request[_]) = {
    app.injector.instanceOf[views.html.racdac.FinishedStatusView].apply(
      mockAppConfig.yourPensionSchemesUrl,
      mockAppConfig.racDacMigrationTransfer
    )(req, implicitly)
  }

  override def beforeEach(): Unit = {
    super.beforeEach()
    when(mockRenderer.render(any(), any())(any())).thenReturn(Future.successful(Html("")))
  }

  "FinishedStatusController" must {

    "return OK and the correct view for a GET" in {
      when(mockQueueConnector.deleteAll(any())(any(), any())).thenReturn(Future.successful(true))

      val req = httpGETRequest(httpPathGET)
      val result = route(app, req).value

      status(result) mustEqual OK

      compareResultAndView(result, getView(req))
    }
  }
}
