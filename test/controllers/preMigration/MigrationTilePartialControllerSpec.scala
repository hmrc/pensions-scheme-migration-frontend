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

package controllers.preMigration

import connectors.cache.BulkMigrationQueueConnector
import controllers.ControllerSpecBase
import controllers.actions._
import matchers.JsonMatchers
import models.PageLink
import org.mockito.ArgumentMatchers.any
import org.scalatest.TryValues
import play.api.i18n.Messages
import play.api.mvc.Result
import play.api.test.Helpers.{status, _}
import uk.gov.hmrc.govukfrontend.views.Aliases.Text
import uk.gov.hmrc.viewmodels.MessageInterpolators

import scala.concurrent.Future
class MigrationTilePartialControllerSpec extends ControllerSpecBase with JsonMatchers with TryValues  {

  private val mockQueueConnector = mock[BulkMigrationQueueConnector]

  val transferLinks: Seq[PageLink] = Seq(
    PageLink("add-pension-schemes", appConfig.schemesMigrationTransfer, Text(Messages("messages__migrationLink__addSchemesLink"))),
    PageLink("add-rac-dacs", appConfig.racDacMigrationTransfer, Text(Messages("messages__migrationLink__addRacDacsLink")))
  )

  val transferLinksInProgress: Seq[PageLink] = Seq(
    PageLink("add-pension-schemes", appConfig.schemesMigrationTransfer, Text(Messages("messages__migrationLink__addSchemesLink"))),
    PageLink("check-rac-dacs", appConfig.racDacMigrationCheckStatus, Text(Messages("messages__migrationLink__checkStatusRacDacsLink")))
  )

  private def controller(): MigrationTilePartialController =
    new MigrationTilePartialController(appConfig, messagesApi, new FakeAuthAction(), mockQueueConnector,
      controllerComponents)

  "MigrationTilePartialController" must {

    "return OK and the correct partial for a GET when migration feature is transfer-enabled and request is not in progress" in {
      when(mockQueueConnector.isRequestInProgress(any())(any(), any())).thenReturn(Future.successful(false))
      val result: Future[Result] = controller().migrationPartial()(fakeDataRequest())

      status(result) mustBe OK
      val view = views.html.preMigration.MigrationLinksPartialView(
        transferLinks
      )(messages)

      compareResultAndView(result, view)
    }

    "return OK and the correct partial for a GET when migration feature is transfer-enabled and request is in progress" in {
      when(mockQueueConnector.isRequestInProgress(any())(any(), any())).thenReturn(Future.successful(true))

      val result: Future[Result] = controller().migrationPartial()(fakeDataRequest())

      status(result) mustBe OK
      val view = views.html.preMigration.MigrationLinksPartialView(
        transferLinksInProgress
      )(messages)
      compareResultAndView(result, view)
    }
  }
}
