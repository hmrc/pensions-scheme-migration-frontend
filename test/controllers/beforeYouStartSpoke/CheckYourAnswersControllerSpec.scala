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

package controllers.beforeYouStartSpoke

import controllers.ControllerSpecBase
import controllers.actions.MutableFakeDataRetrievalAction
import helpers.cya.BeforeYouStartCYAHelper
import matchers.JsonMatchers
import org.mockito.ArgumentMatchers.any
import play.api.Application
import play.api.i18n.Messages
import play.api.inject.bind
import play.api.inject.guice.GuiceableModule
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.HtmlContent
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.{Key, SummaryListRow, Value}
import uk.gov.hmrc.viewmodels.NunjucksSupport
import utils.Data.{schemeName, ua}
import views.html.CheckYourAnswersView

import scala.concurrent.Future

class CheckYourAnswersControllerSpec extends ControllerSpecBase with NunjucksSupport with JsonMatchers {

  private val mutableFakeDataRetrievalAction: MutableFakeDataRetrievalAction = new MutableFakeDataRetrievalAction()
  private val mockCyaHelper: BeforeYouStartCYAHelper = mock[BeforeYouStartCYAHelper]
  val extraModules: Seq[GuiceableModule] = Seq(
    bind[BeforeYouStartCYAHelper].to(mockCyaHelper)
  )

  private val application: Application = applicationBuilderMutableRetrievalAction(mutableFakeDataRetrievalAction, extraModules).build()

  private def httpPathGET: String = controllers.beforeYouStartSpoke.routes.CheckYourAnswersController.onPageLoad.url

  private val rows = Seq(
    SummaryListRow(
      key = Key(HtmlContent(Messages("messages__cya__scheme_name"))),
      value = Value(HtmlContent(schemeName)),
      actions = None
    )
  )

  override def beforeEach(): Unit = {
    super.beforeEach()
    when(mockCyaHelper.rowsForCYA(any())(any(), any())).thenReturn(rows)
  }


  "CheckYourAnswers Controller" must {

    "return OK and the correct view for a GET" in {
      mutableFakeDataRetrievalAction.setDataToReturn(Some(ua))

      val request = FakeRequest(GET, httpPathGET)
      val result = route(application, httpGETRequest(httpPathGET)).value

      status(result) mustEqual OK

      val view = application.injector.instanceOf[CheckYourAnswersView].apply(
        controllers.routes.TaskListController.onPageLoad.url,
        schemeName,
        rows
      )(request, messages)

      compareResultAndView(result, view)
    }

  }
}
