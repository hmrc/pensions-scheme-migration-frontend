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
import models.Scheme
import org.mockito.ArgumentMatchers.any
import org.mockito.{ArgumentCaptor, MockitoSugar}
import org.scalatest.TryValues
import play.api.libs.json.{JsObject, JsValue, Json}
import play.api.mvc.Result
import play.api.test.Helpers.{status, _}
import play.twirl.api.Html
import renderer.Renderer
import uk.gov.hmrc.viewmodels.NunjucksSupport
import utils.Data
import utils.Data.ua

import scala.concurrent.Future

class BeforeYouStartControllerSpec extends ControllerSpecBase with NunjucksSupport with JsonMatchers with TryValues with MockitoSugar {
  private val psaName: String = "Nigel"
  private val templateToBeRendered: String = "preMigration/beforeYouStart.njk"
  private def json: JsObject =
    Json.obj(
      "continueUrl" -> controllers.routes.TaskListController.onPageLoad().url,
      "psaName" -> psaName,
      "returnUrl" -> controllers.routes.PensionSchemeRedirectController.onPageLoad().url
    )
  private val mutableFakeDataRetrievalAction: MutableFakeDataRetrievalAction = new MutableFakeDataRetrievalAction()

  val schemeName: String = "Test scheme name"
  val itemList : JsValue = Json.obj(
    "schemeName" -> schemeName
  )
  private def controller(): BeforeYouStartController =
    new BeforeYouStartController(messagesApi, new FakeAuthAction(), mutableFakeDataRetrievalAction,
      mockMinimalDetailsConnector,mockUserAnswersCacheConnector, mockLegacySchemeDetailsConnector, controllerComponents, new Renderer(mockAppConfig, mockRenderer))

  "BeforeYouStartController" must {
    "return OK and the correct view for a GET" in {
      mutableFakeDataRetrievalAction.setDataToReturn(Some(ua))
      when(mockRenderer.render(any(), any())(any())).thenReturn(Future.successful(Html("")))
      when(mockMinimalDetailsConnector.getPSAName(any(),any())).thenReturn(Future.successful(psaName))
      val templateCaptor : ArgumentCaptor[String] = ArgumentCaptor.forClass(classOf[String])
      val jsonCaptor: ArgumentCaptor[JsObject] = ArgumentCaptor.forClass(classOf[JsObject])

      val result: Future[Result] = controller().onPageLoad()(fakeDataRequest())

      status(result) mustBe OK

      verify(mockRenderer, times(1)).render(templateCaptor.capture(), jsonCaptor.capture())(any())

      templateCaptor.getValue mustEqual templateToBeRendered

      jsonCaptor.getValue must containJson(json)
    }

    "return OK and the correct view for a GET when data not present in userAnswers" in {
      mutableFakeDataRetrievalAction.setDataToReturn(None)
      mutableFakeDataRetrievalAction.setLockToReturn(Some(Data.migrationLock))
      when(mockRenderer.render(any(), any())(any())).thenReturn(Future.successful(Html("")))
      when(mockMinimalDetailsConnector.getPSAName(any(),any())).thenReturn(Future.successful(psaName))
      when( mockUserAnswersCacheConnector.save(any(), any())(any(),any())).thenReturn(Future.successful(Json.obj()))
      when(mockLegacySchemeDetailsConnector.getLegacySchemeDetails(any(), any())(any(), any())).thenReturn(Future.successful(Right(itemList)))
      val templateCaptor : ArgumentCaptor[String] = ArgumentCaptor.forClass(classOf[String])
      val jsonCaptor: ArgumentCaptor[JsObject] = ArgumentCaptor.forClass(classOf[JsObject])

      val result: Future[Result] = controller().onPageLoad()(fakeDataRequest())

      status(result) mustBe OK

      verify(mockRenderer, times(1)).render(templateCaptor.capture(), jsonCaptor.capture())(any())

      templateCaptor.getValue mustEqual templateToBeRendered

      jsonCaptor.getValue must containJson(json)
    }

    "redirect to List of schemes if lock can not be retrieved " in {
      mutableFakeDataRetrievalAction.setLockToReturn(None)
      val result: Future[Result] = controller().onPageLoad()(fakeDataRequest())

      status(result) mustEqual SEE_OTHER
      redirectLocation(result) mustBe Some(controllers.preMigration.routes.ListOfSchemesController.onPageLoad(Scheme).url)

    }
  }
}
