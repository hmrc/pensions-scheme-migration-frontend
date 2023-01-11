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

package controllers

import controllers.actions.MutableFakeDataRetrievalAction
import matchers.JsonMatchers
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import play.api.Application
import play.api.libs.json.JsObject
import play.api.test.Helpers._
import play.twirl.api.Html
import uk.gov.hmrc.nunjucks.NunjucksSupport
import utils.{Enumerable, UserAnswers}

import scala.concurrent.Future

class IndexControllerSpec extends ControllerSpecBase with NunjucksSupport with JsonMatchers with Enumerable.Implicits {

  private val templateToBeRendered = "index.njk"

  private val mutableFakeDataRetrievalAction: MutableFakeDataRetrievalAction = new MutableFakeDataRetrievalAction()

  private val application: Application = applicationBuilderMutableRetrievalAction(mutableFakeDataRetrievalAction).build()

  private def httpPathGET: String = controllers.routes.IndexController.onPageLoad.url

  override def beforeEach(): Unit = {
    when(mockRenderer.render(any(), any())(any())).thenReturn(Future.successful(Html("")))
  }

  "IndexController" must {

    "return OK and the correct view for a GET" in {
      mutableFakeDataRetrievalAction.setDataToReturn(Some(UserAnswers()))

      val templateCaptor : ArgumentCaptor[String] = ArgumentCaptor.forClass(classOf[String])
      val jsonCaptor: ArgumentCaptor[JsObject] = ArgumentCaptor.forClass(classOf[JsObject])

      val result = route(application, httpGETRequest(httpPathGET)).value

      status(result) mustEqual OK

      verify(mockRenderer, times(1)).render(templateCaptor.capture(), jsonCaptor.capture())(any())

      templateCaptor.getValue mustEqual templateToBeRendered
    }

  }
}
