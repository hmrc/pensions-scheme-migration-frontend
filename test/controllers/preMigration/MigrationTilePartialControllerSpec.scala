/*
 * Copyright 2021 HM Revenue & Customs
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

import controllers.ControllerSpecBase
import controllers.actions._
import matchers.JsonMatchers
import models.PageLink
import org.mockito.ArgumentCaptor
import org.mockito.Matchers.any
import org.mockito.Mockito.{times, verify, when}
import org.scalatest.TryValues
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.Result
import play.api.test.Helpers.{status, _}
import play.twirl.api.Html
import renderer.Renderer
import uk.gov.hmrc.viewmodels.NunjucksSupport

import scala.concurrent.Future

class MigrationTilePartialControllerSpec extends ControllerSpecBase with NunjucksSupport with JsonMatchers with TryValues with MockitoSugar {

  private val templateToBeRendered: String = "preMigration/migrationLinksPartial.njk"

  val links: Seq[PageLink] = Seq(
    PageLink("view-pension-schemes", appConfig.schemesMigrationViewOnly, msg"messages__migrationLink__viewSchemesLink"),
    PageLink("view-rac-dacs", appConfig.racDacMigrationViewOnly, msg"messages__migrationLink__viewRacDacsLink")
  )

  private def controller(): MigrationTilePartialController =
    new MigrationTilePartialController(appConfig, messagesApi, new FakeAuthAction(), controllerComponents, new Renderer(mockAppConfig, mockRenderer))

  "MigrationTilePartialController" must {
    "return OK and the correct partial for a GET" in {
      when(mockRenderer.render(any(), any())(any())).thenReturn(Future.successful(Html("")))
      val templateCaptor = ArgumentCaptor.forClass(classOf[String])
      val jsonCaptor = ArgumentCaptor.forClass(classOf[JsObject])

      val result: Future[Result] = controller().migrationPartial()(fakeDataRequest())

      status(result) mustBe OK

      verify(mockRenderer, times(1)).render(templateCaptor.capture(), jsonCaptor.capture())(any())

      templateCaptor.getValue mustEqual templateToBeRendered

      jsonCaptor.getValue must containJson(Json.obj("links" -> Json.toJson(links)))
    }
  }
}
