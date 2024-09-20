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

package controllers.establishers.partnership.details

import controllers.ControllerSpecBase
import controllers.actions._
import identifiers.establishers.partnership.PartnershipDetailsId
import matchers.JsonMatchers
import models.{Index, NormalMode}
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.scalatest.TryValues
import play.api.i18n.Messages
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.Result
import play.api.test.Helpers.{status, _}
import play.twirl.api.Html
import renderer.Renderer
import services.common.details.CommonWhatYouWillNeedDetailsService
import uk.gov.hmrc.viewmodels.NunjucksSupport
import utils.Data.{partnershipDetails, ua}
import utils.UserAnswers

import scala.concurrent.Future

class WhatYouWillNeedControllerSpec extends ControllerSpecBase with NunjucksSupport with JsonMatchers with TryValues {

  private val index: Index = Index(0)
  private val userAnswers: UserAnswers = ua.set(PartnershipDetailsId(0), partnershipDetails).success.value
  private val templateToBeRendered: String = "establishers/partnership/details/whatYouWillNeed.njk"
  private def json: JsObject =
    Json.obj(
      "name"        -> partnershipDetails.partnershipName,
      "pageTitle" -> Messages("messages__partnershipDetails__whatYouWillNeed_title"),
      "continueUrl" -> routes.HaveUTRController.onPageLoad(index, NormalMode).url,
      "schemeName"  -> "Test scheme name"
    )

  private def controller(dataRetrievalAction: DataRetrievalAction): WhatYouWillNeedController =
    new WhatYouWillNeedController(messagesApi, new FakeAuthAction(), dataRetrievalAction,
      new DataRequiredActionImpl, common = new CommonWhatYouWillNeedDetailsService(
        controllerComponents = controllerComponents,
        renderer = new Renderer(mockAppConfig, mockRenderer),
        messagesApi = messagesApi
      ))

  "WhatYouWillNeedController" must {
    "return OK and the correct view for a GET" in {
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
