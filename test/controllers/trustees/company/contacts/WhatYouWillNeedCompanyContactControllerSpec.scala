/*
 * Copyright 2022 HM Revenue & Customs
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

package controllers.trustees.company.contacts

import controllers.ControllerSpecBase
import controllers.actions._
import identifiers.trustees.company.CompanyDetailsId
import matchers.JsonMatchers
import models.{CompanyDetails, NormalMode}
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{times, verify, when}
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
class WhatYouWillNeedCompanyContactControllerSpec
  extends ControllerSpecBase
    with NunjucksSupport
    with JsonMatchers
    with TryValues {

  private val company: CompanyDetails = CompanyDetails("test")
  private val userAnswers: UserAnswers = ua.set(CompanyDetailsId(0), company).success.value
  private val templateToBeRendered: String = "whatYouWillNeedContact.njk"
  private def json: JsObject =
    Json.obj(
      "name"        -> company.companyName,
      "pageHeading" -> Message("messages__title_company"),
      "entityType" -> Message("messages__company"),
      "continueUrl" -> controllers.trustees.company.contacts.routes.EnterEmailController.onPageLoad(0, NormalMode).url,
      "schemeName"  -> schemeName
    )

  private def controller(
                          dataRetrievalAction: DataRetrievalAction
                        ): WhatYouWillNeedCompanyContactController =
    new WhatYouWillNeedCompanyContactController(
      messagesApi          = messagesApi,
      authenticate         = new FakeAuthAction(),
      getData              = dataRetrievalAction,
      requireData          = new DataRequiredActionImpl,
      controllerComponents = controllerComponents,
      renderer             = new Renderer(mockAppConfig, mockRenderer)
    )

  "WhatYouWillNeedCompanyContactController" must {
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
