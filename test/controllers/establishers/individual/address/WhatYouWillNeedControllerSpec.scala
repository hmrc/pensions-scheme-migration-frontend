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

package controllers.establishers.individual.address

import controllers.ControllerSpecBase
import controllers.actions._
import helpers.routes.EstablishersIndividualRoutes
import identifiers.establishers.individual.EstablisherNameId
import matchers.JsonMatchers
import models.{NormalMode, PersonName}
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.scalatest.TryValues
import play.api.i18n.Messages
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.Result
import play.api.test.Helpers.{status, _}
import play.twirl.api.Html
import renderer.Renderer
import uk.gov.hmrc.viewmodels.NunjucksSupport
import utils.Data.ua
import utils.UserAnswers

import scala.concurrent.Future

class WhatYouWillNeedControllerSpec
  extends ControllerSpecBase
    with NunjucksSupport
    with JsonMatchers
    with TryValues {

  private val personName: PersonName =
    PersonName("Jane", "Doe")
  private val userAnswers: UserAnswers =
    ua.set(EstablisherNameId(0), personName).success.value
  private val templateToBeRendered: String =
    "address/whatYouWillNeed.njk"
  private def json: JsObject =
    Json.obj(
      "name"        -> "Jane Doe",
      "entityType" -> Messages("messages__title_individual"),
      "continueUrl" -> EstablishersIndividualRoutes.enterPostcodeRoute(0, NormalMode).url,
      "schemeName"  -> "Test scheme name"
    )

  private def controller(
                          dataRetrievalAction: DataRetrievalAction
                        ): WhatYouWillNeedController =
    new WhatYouWillNeedController(
      messagesApi          = messagesApi,
      authenticate         = new FakeAuthAction(),
      getData              = dataRetrievalAction,
      requireData          = new DataRequiredActionImpl,
      controllerComponents = controllerComponents,
      renderer             = new Renderer(mockAppConfig, mockRenderer)
    )

  "WhatYouWillNeedController" must {
    "return OK and the correct view for a GET" in {
      when(mockRenderer.render(any(), any())(any())).thenReturn(Future.successful(Html("")))

      val templateCaptor = ArgumentCaptor.forClass(classOf[String])
      val jsonCaptor = ArgumentCaptor.forClass(classOf[JsObject])

      val getData = new FakeDataRetrievalAction(Some(userAnswers))
      val result: Future[Result] = controller(getData).onPageLoad(0)(fakeDataRequest(userAnswers))

      status(result) mustBe OK

      verify(mockRenderer, times(1)).render(templateCaptor.capture(), jsonCaptor.capture())(any())

      templateCaptor.getValue mustEqual templateToBeRendered

      jsonCaptor.getValue must containJson(json)
    }
  }
}
