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

package controllers.trustees.partnership

import controllers.ControllerSpecBase
import controllers.actions._
import helpers.SpokeCreationService
import identifiers.trustees.partnership.PartnershipDetailsId
import matchers.JsonMatchers
import models.PartnershipDetails
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.scalatest.TryValues
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.Result
import play.api.test.Helpers.{status, _}
import play.twirl.api.Html
import renderer.Renderer
import uk.gov.hmrc.viewmodels.NunjucksSupport
import utils.Data.{schemeName, ua}
import utils.UserAnswers
import viewmodels.Message

import scala.concurrent.Future
class SpokeTaskListControllerSpec
  extends ControllerSpecBase
    with NunjucksSupport
    with JsonMatchers
    with TryValues {

  private val partnership: PartnershipDetails = PartnershipDetails("test")
  private val userAnswers: UserAnswers = ua.set(PartnershipDetailsId(0), partnership).success.value
  private val mockSpokeCreationService = mock[SpokeCreationService]
  private val templateToBeRendered: String = "spokeTaskList.njk"

  private def json: JsObject =
    Json.obj(
      "taskSections" -> Nil,
      "entityName" -> partnership.partnershipName,
      "schemeName" -> schemeName,
      "totalSpokes" -> 0,
      "completedCount" -> 0,
      "entityType" -> Message("messages__tasklist__trustee"),
      "submitUrl" -> controllers.trustees.routes.AddTrusteeController.onPageLoad.url
    )

  private def controller(
                          dataRetrievalAction: DataRetrievalAction
                        ): SpokeTaskListController =
    new SpokeTaskListController(
      messagesApi          = messagesApi,
      authenticate         = new FakeAuthAction(),
      getData              = dataRetrievalAction,
      requireData          = new DataRequiredActionImpl,
      controllerComponents = controllerComponents,
      spokeCreationService = mockSpokeCreationService,
      renderer             = new Renderer(mockAppConfig, mockRenderer)
    )

  "Task List Controller" must {
    "return OK and the correct view for a GET" in {
      when(mockSpokeCreationService.getTrusteePartnershipSpokes(any(), any(), any())(any())).thenReturn(Nil)
      when(mockRenderer.render(any(), any())(any())).thenReturn(Future.successful(Html("")))

      val templateCaptor : ArgumentCaptor[String] = ArgumentCaptor.forClass(classOf[String])
      val jsonCaptor: ArgumentCaptor[JsObject] = ArgumentCaptor.forClass(classOf[JsObject])

      val getData = new FakeDataRetrievalAction(Some(userAnswers))
      val result: Future[Result] = controller(getData).onPageLoad(0)(fakeDataRequest(userAnswers))

      status(result) mustBe OK

      verify(mockRenderer, times(1)).render(templateCaptor.capture(), jsonCaptor.capture())(any())

      templateCaptor.getValue mustEqual templateToBeRendered

      jsonCaptor.getValue must containJson(json)
    }
  }
}
