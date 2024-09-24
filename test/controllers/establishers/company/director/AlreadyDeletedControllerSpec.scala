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

package controllers.establishers.company.director

import controllers.ControllerSpecBase
import controllers.actions.MutableFakeDataRetrievalAction
import identifiers.establishers.company.director.DirectorNameId
import matchers.JsonMatchers
import models.{Index, NormalMode, PersonName}
import org.mockito.ArgumentMatchers.any
import play.api.Application
import play.api.test.Helpers._
import play.twirl.api.Html
import uk.gov.hmrc.nunjucks.NunjucksSupport
import utils.Data.{schemeName, ua}
import utils.{Enumerable, UserAnswers}
import views.html.AlreadyDeletedView

import scala.concurrent.Future

class AlreadyDeletedControllerSpec extends ControllerSpecBase with NunjucksSupport with JsonMatchers with Enumerable.Implicits {

  private val index: Index = Index(0)
  private val directorIndex: Index = Index(0)
  private val name = PersonName("Jane", "Doe")
  private val userAnswers: Option[UserAnswers] = ua.set(DirectorNameId(0,0), name).toOption

  private val mutableFakeDataRetrievalAction: MutableFakeDataRetrievalAction = new MutableFakeDataRetrievalAction()

  private val application: Application = applicationBuilderMutableRetrievalAction(mutableFakeDataRetrievalAction).build()

  private def httpPathGET: String = controllers.establishers.company.director.routes.AlreadyDeletedController.onPageLoad(index,directorIndex).url

  override def beforeEach(): Unit = {
    super.beforeEach()
    when(mockRenderer.render(any(), any())(any())).thenReturn(Future.successful(Html("")))
  }

  private val request = httpGETRequest(routes.AlreadyDeletedController.onPageLoad(index, directorIndex).url)


  "AlreadyDeletedController" must {

    "return OK and the correct view for a GET" in {
      mutableFakeDataRetrievalAction.setDataToReturn(userAnswers)

      val result = route(application, httpGETRequest(httpPathGET)).value

      status(result) mustEqual OK

      val view = application.injector.instanceOf[AlreadyDeletedView].apply(
        messages("messages__alreadyDeleted__director_title"),
        name.fullName,
        Some(schemeName),
        controllers.establishers.company.routes.AddCompanyDirectorsController.onPageLoad(0,NormalMode).url
      )(request, messages)

      compareResultAndView(result, view)
    }

  }
}
