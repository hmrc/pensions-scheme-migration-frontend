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

import connectors.cache.{BulkMigrationEventsLogConnector, BulkMigrationQueueConnector}
import controllers.ControllerSpecBase
import controllers.actions.MutableFakeDataRetrievalAction
import matchers.JsonMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceableModule
import play.api.mvc.{Call, Request}
import play.api.test.Helpers._
import utils.Enumerable

import scala.concurrent.Future
class ProcessingRequestControllerSpec extends ControllerSpecBase with JsonMatchers with Enumerable.Implicits {

  private val mockQueueConnector = mock[BulkMigrationQueueConnector]
  private val mockBulkMigrationEventsLogConnector = mock[BulkMigrationEventsLogConnector]
  private val mutableFakeDataRetrievalAction: MutableFakeDataRetrievalAction = new MutableFakeDataRetrievalAction()
  private val extraModules: Seq[GuiceableModule] = Seq(
    bind[BulkMigrationQueueConnector].to(mockQueueConnector),
    bind[BulkMigrationEventsLogConnector].to(mockBulkMigrationEventsLogConnector)
  )
  override def fakeApplication(): Application = applicationBuilderMutableRetrievalAction(mutableFakeDataRetrievalAction, extraModules).build()

  private def httpPathGET: String = controllers.racdac.bulk.routes.ProcessingRequestController.onPageLoad.url

  private def getView(req: Request[_], heading: String, content: String, redirect: Option[Call]) = {
    app.injector.instanceOf[views.html.racdac.ProcessingRequestView].apply(
      heading,
      heading,
      content,
      redirect
    )(req, implicitly)
  }

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockBulkMigrationEventsLogConnector)
  }

  "ProcessingRequestController" must {

    "return OK and redirect for a GET when migration events log is ACCEPTED" in {
      when(mockBulkMigrationEventsLogConnector.getStatus(any(), any())).thenReturn(Future.successful(ACCEPTED))

      val req = httpGETRequest(httpPathGET)
      val result = route(app, req).value

      status(result) mustEqual SEE_OTHER
    }

    "return OK and the correct view for a GET when migration events log is NOT_FOUND" in {
      when(mockBulkMigrationEventsLogConnector.getStatus(any(), any())).thenReturn(Future.successful(NOT_FOUND))

      val req = httpGETRequest(httpPathGET)
      val result = route(app, req).value

      status(result) mustEqual OK
      compareResultAndView(result, getView(
        req,
        "messages__processingRequest__h1_processing",
        "messages__processingRequest__content_processing",
        Some(routes.ProcessingRequestController.onPageLoad)
      ))
    }

    "return OK and the correct view for a GET when migration events log is INTERNAL_SERVER_ERROR" in {
      when(mockBulkMigrationEventsLogConnector.getStatus(any(), any())).thenReturn(Future.successful(INTERNAL_SERVER_ERROR))

      val req = httpGETRequest(httpPathGET)
      val result = route(app, req).value

      status(result) mustEqual OK
      compareResultAndView(result, getView(
        req,
        "messages__processingRequest__h1_failure",
        "messages__thereIsAProblem__p1",
        None
      ))
    }
  }
}
