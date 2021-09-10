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

package controllers

import connectors.MinimalDetailsConnector
import controllers.actions.MutableFakeDataRetrievalAction
import identifiers.establishers.individual.EstablisherNameId
import matchers.JsonMatchers
import models.PersonName
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceableModule
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.Results.Ok
import play.api.test.Helpers._
import play.twirl.api.Html
import uk.gov.hmrc.nunjucks.NunjucksSupport
import utils.Data.ua
import utils.{Enumerable, UserAnswers}

import scala.concurrent.Future

class SuccessControllerSpec extends ControllerSpecBase with NunjucksSupport with JsonMatchers with Enumerable.Implicits {

  private val email: String = "a@b.c"
  private val name = PersonName("Jane", "Doe")
  private val userAnswers: Option[UserAnswers] = ua.set(EstablisherNameId(0), name).toOption
  private val templateToBeRendered = "success.njk"
  private val mockMinimalDetailsConnector: MinimalDetailsConnector = mock[MinimalDetailsConnector]

  private val mutableFakeDataRetrievalAction: MutableFakeDataRetrievalAction = new MutableFakeDataRetrievalAction()
  val extraModules: Seq[GuiceableModule] = Seq(
    bind[MinimalDetailsConnector].to(mockMinimalDetailsConnector)
  )
  private val application: Application = applicationBuilderMutableRetrievalAction(mutableFakeDataRetrievalAction, extraModules).build()

  private def httpPathGET: String = controllers.routes.SuccessController.onPageLoad().url

  private val jsonToPassToTemplate: JsObject =
    Json.obj(
      "refNumber" -> "1234567890",
      "email" -> email,
      "yourSchemesLink" -> controllers.routes.TaskListController.onPageLoad().url,
      "submitUrl" -> controllers.routes.LogoutController.onPageLoad().url
    )

  override def beforeEach: Unit = {
    super.beforeEach
    when(mockRenderer.render(any(), any())(any())).thenReturn(Future.successful(Html("")))
    when(mockUserAnswersCacheConnector.remove(any())(any(), any())).thenReturn(Future.successful(Ok))
  }


  "SuccessController" must {

    "return OK and the correct view for a GET" in {
      mutableFakeDataRetrievalAction.setDataToReturn(userAnswers)
      when(mockMinimalDetailsConnector.getPSAEmail(any(), any())).thenReturn(Future.successful(email))
      val templateCaptor : ArgumentCaptor[String] = ArgumentCaptor.forClass(classOf[String])
      val jsonCaptor: ArgumentCaptor[JsObject] = ArgumentCaptor.forClass(classOf[JsObject])

      val result = route(application, httpGETRequest(httpPathGET)).value

      status(result) mustEqual OK

      verify(mockRenderer, times(1)).render(templateCaptor.capture(), jsonCaptor.capture())(any())

      templateCaptor.getValue mustEqual templateToBeRendered

      jsonCaptor.getValue must containJson(jsonToPassToTemplate)
    }

  }
}
