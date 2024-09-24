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

package controllers.aboutMembership

import controllers.ControllerSpecBase
import controllers.actions.MutableFakeDataRetrievalAction
import helpers.cya.AboutCYAHelper
import matchers.JsonMatchers
import org.mockito.ArgumentMatchers.any
import play.api.Application
import play.api.i18n.Messages
import play.api.inject.bind
import play.api.inject.guice.GuiceableModule
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.HtmlContent
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist._
import uk.gov.hmrc.viewmodels.NunjucksSupport
import utils.Data.{schemeName, ua}
import utils.UserAnswers
import views.html.CheckYourAnswersView

import scala.concurrent.Future

class CheckYourAnswersControllerSpec extends ControllerSpecBase with NunjucksSupport with JsonMatchers {
  private val userAnswers: Option[UserAnswers] = Some(ua)
  private val mutableFakeDataRetrievalAction: MutableFakeDataRetrievalAction = new MutableFakeDataRetrievalAction()
  private val mockCyaHelper: AboutCYAHelper = mock[AboutCYAHelper]
  val extraModules: Seq[GuiceableModule] = Seq(
    bind[AboutCYAHelper].to(mockCyaHelper)
  )

  private val application: Application = applicationBuilderMutableRetrievalAction(mutableFakeDataRetrievalAction, extraModules).build()

  private def httpPathGET: String = controllers.aboutMembership.routes.CheckYourAnswersController.onPageLoad.url

  private val rows = Seq(
    SummaryListRow(
      key = Key(HtmlContent(Messages("currentMembers.title", schemeName))),
      value = Value(HtmlContent(Messages("site.incomplete"))),
      actions = Some(
        Actions(
          items = Seq(
            ActionItem(
              content = HtmlContent(s"<span aria-hidden=true >${messages("site.add")}</span>"),
              href = controllers.aboutMembership.routes.CurrentMembersController.onPageLoad.url,
              visuallyHiddenText = Some(Messages("messages__visuallyhidden__currentMembers"))
            )
          )
        )
      )
    ),
    SummaryListRow(
      key = Key(HtmlContent(Messages("futureMembers.title", schemeName))),
      value = Value(HtmlContent(Messages("site.incomplete"))),
      actions = Some(
        Actions(
          items = Seq(
            ActionItem(
              content = HtmlContent(s"<span aria-hidden=true >${messages("site.add")}</span>"),
              href = routes.FutureMembersController.onPageLoad.url,
              visuallyHiddenText = Some(Messages("messages__visuallyhidden__futureMembers"))
            )
          )
        )
      )
    )
  )

  override def beforeEach(): Unit = {
    super.beforeEach()
    when(mockRenderer.render(any(), any())(any())).thenReturn(Future.successful(play.twirl.api.Html("")))
    when(mockCyaHelper.membershipRows(any(), any())).thenReturn(rows)
  }


  "CheckYourAnswers Controller" must {

    "return OK and the correct view for a GET" in {
      mutableFakeDataRetrievalAction.setDataToReturn(userAnswers)

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
