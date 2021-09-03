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

package controllers.establishers.partnership.contact

import controllers.ControllerSpecBase
import controllers.actions.{DataRequiredActionImpl, DataRetrievalAction, FakeAuthAction, FakeDataRetrievalAction}
import matchers.JsonMatchers.containJson
import models.NormalMode
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.scalatest.matchers.must.Matchers
import play.api.libs.json.{JsObject, Json}
import play.api.test.Helpers._
import play.twirl.api.Html
import renderer.Renderer
import utils.Data.schemeName
import utils.{Data, UserAnswers}

import scala.concurrent.Future

class WhatYouWillNeedContactControllerSpec
  extends ControllerSpecBase
    with Matchers {

  private val userAnswers: UserAnswers = Data.ua
  private val templateToBeRendered: String = "whatYouWillNeedContact.njk"

  private def json: JsObject =
    Json.obj(
      "name"        -> establishers.partnership.partnershipName,
      "continueUrl" -> controllers.establishers.company.contact.routes.EnterEmailController.onPageLoad(0, NormalMode).url,
      "schemeName"  -> schemeName
    )

  private def createController(
                          dataRetrievalAction: DataRetrievalAction
                        ): WhatYouWillNeedContactController = {
    new WhatYouWillNeedContactController(
      authenticate         = new FakeAuthAction(),
      getData              = dataRetrievalAction,
      requireData          = new DataRequiredActionImpl,
      controllerComponents = controllerComponents,
      renderer             = new Renderer(mockAppConfig, mockRenderer)
    )
  }

    "WhatYouWillNeedPartnershipContactController" must {

      "return OK and the correct view for a GET" in {

        when(mockRenderer.render(any(), any())(any())).thenReturn(Future.successful(Html("")))

        val templateCaptor = ArgumentCaptor.forClass(classOf[String])
        val jsonCaptor = ArgumentCaptor.forClass(classOf[JsObject])

        val getData = new FakeDataRetrievalAction(Some(userAnswers))

        val result = createController(getData).onPageLoad(0)(fakeDataRequest(userAnswers))

        status(result) mustBe OK

        verify(mockRenderer, times(1)).render(templateCaptor.capture(), jsonCaptor.capture())(any())

        templateCaptor.getValue mustEqual templateToBeRendered

        jsonCaptor.getValue must containJson(json)

      }

    }

  }