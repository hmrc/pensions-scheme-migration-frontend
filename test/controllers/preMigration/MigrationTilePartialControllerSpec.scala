/*
 * Copyright 2023 HM Revenue & Customs
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
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.scalatest.TryValues
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.Result
import play.api.test.Helpers.{status, _}
import play.twirl.api.Html
import renderer.Renderer
import uk.gov.hmrc.viewmodels.NunjucksSupport

import scala.concurrent.Future
class MigrationTilePartialControllerSpec extends ControllerSpecBase with NunjucksSupport with JsonMatchers with TryValues  {

  private val templateToBeRendered: String = "preMigration/migrationLinksPartial.njk"
  private val mockQueueConnector = mock[BulkMigrationQueueConnector]

  val viewOnlyLinks: Seq[PageLink] = Seq(
    PageLink("view-pension-schemes", appConfig.schemesMigrationViewOnly, msg"messages__migrationLink__viewSchemesLink"),
    PageLink("view-rac-dacs", appConfig.racDacMigrationViewOnly, msg"messages__migrationLink__viewRacDacsLink")
  )

  val transferLinks: Seq[PageLink] = Seq(
    PageLink("add-pension-schemes", appConfig.schemesMigrationTransfer, msg"messages__migrationLink__addSchemesLink"),
    PageLink("add-rac-dacs", appConfig.racDacMigrationTransfer, msg"messages__migrationLink__addRacDacsLink")
  )

  val transferLinksInProgress: Seq[PageLink] = Seq(
    PageLink("add-pension-schemes", appConfig.schemesMigrationTransfer, msg"messages__migrationLink__addSchemesLink"),
    PageLink("check-rac-dacs", appConfig.racDacMigrationCheckStatus, msg"messages__migrationLink__checkStatusRacDacsLink")
  )

  private def controller(): MigrationTilePartialController =
    new MigrationTilePartialController(appConfig, messagesApi, new FakeAuthAction(), mockQueueConnector,
      controllerComponents, new Renderer(mockAppConfig, mockRenderer))

  "MigrationTilePartialController" must {

    "return OK and the correct partial for a GET when migration feature is transfer-enabled and request is not in progress" in {
      when(mockRenderer.render(any(), any())(any())).thenReturn(Future.successful(Html("")))
      when(mockQueueConnector.isRequestInProgress(any())(any(), any())).thenReturn(Future.successful(false))

      val templateCaptor: ArgumentCaptor[String] = ArgumentCaptor.forClass(classOf[String])
      val jsonCaptor: ArgumentCaptor[JsObject] = ArgumentCaptor.forClass(classOf[JsObject])
      val result: Future[Result] = controller().migrationPartial()(fakeDataRequest())

      status(result) mustBe OK

      verify(mockRenderer, times(1)).render(templateCaptor.capture(), jsonCaptor.capture())(any())

      templateCaptor.getValue mustEqual templateToBeRendered

      jsonCaptor.getValue must containJson(Json.obj("links" -> Json.toJson(transferLinks)))
    }

    "return OK and the correct partial for a GET when migration feature is transfer-enabled and request is in progress" in {
      when(mockRenderer.render(any(), any())(any())).thenReturn(Future.successful(Html("")))
      when(mockQueueConnector.isRequestInProgress(any())(any(), any())).thenReturn(Future.successful(true))

      val templateCaptor: ArgumentCaptor[String] = ArgumentCaptor.forClass(classOf[String])
      val jsonCaptor: ArgumentCaptor[JsObject] = ArgumentCaptor.forClass(classOf[JsObject])
      val result: Future[Result] = controller().migrationPartial()(fakeDataRequest())

      status(result) mustBe OK

      verify(mockRenderer, times(1)).render(templateCaptor.capture(), jsonCaptor.capture())(any())

      templateCaptor.getValue mustEqual templateToBeRendered

      jsonCaptor.getValue must containJson(Json.obj("links" -> Json.toJson(transferLinksInProgress)))
    }
  }
}
