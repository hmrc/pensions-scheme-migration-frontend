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

package controllers.aboutMembership

import controllers.ControllerSpecBase
import controllers.actions.MutableFakeDataRetrievalAction
import helpers.cya.AboutCYAHelper
import matchers.JsonMatchers
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{times, verify, when}
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceableModule
import play.api.libs.json.{JsObject, Json}
import play.api.test.Helpers._
import uk.gov.hmrc.viewmodels.SummaryList.{Action, Key, Row, Value}
import uk.gov.hmrc.viewmodels.{Html, NunjucksSupport}
import utils.Data.{schemeName, ua}
import utils.UserAnswers

import scala.concurrent.Future

class CheckYourAnswersControllerSpec extends ControllerSpecBase with NunjucksSupport with JsonMatchers {
  private val userAnswers: Option[UserAnswers] = Some(ua)
  private val mutableFakeDataRetrievalAction: MutableFakeDataRetrievalAction = new MutableFakeDataRetrievalAction()
  private val mockCyaHelper: AboutCYAHelper = mock[AboutCYAHelper]
  val extraModules: Seq[GuiceableModule] = Seq(
    bind[AboutCYAHelper].to(mockCyaHelper)
  )

  private val application: Application = applicationBuilderMutableRetrievalAction(mutableFakeDataRetrievalAction, extraModules).build()
  private val templateToBeRendered = "check-your-answers.njk"

  private def httpPathGET: String = controllers.aboutMembership.routes.CheckYourAnswersController.onPageLoad.url

  private val rows = Seq(
    Row(
      key = Key(msg"currentMembers.title".withArgs(schemeName), classes = Seq("govuk-!-width-one-half")),
      value = Value(msg"site.not_entered", classes = Seq("govuk-!-width-one-third")),
      actions = List(
        Action(
          content = Html(s"<span aria-hidden=true >${messages("site.add")}</span>"),
          href = controllers.aboutMembership.routes.CurrentMembersController.onPageLoad().url,
          visuallyHiddenText = Some(msg"messages__visuallyhidden__currentMembers")
        )
      )
    ),
    Row(
      key = Key(msg"futureMembers.title".withArgs(schemeName), classes = Seq("govuk-!-width-one-half")),
      value = Value(msg"site.not_entered", classes = Seq("govuk-!-width-one-third")),
      actions = List(
        Action(
          content = Html(s"<span aria-hidden=true >${messages("site.add")}</span>"),
          href = routes.FutureMembersController.onPageLoad().url,
          visuallyHiddenText = Some(msg"messages__visuallyhidden__futureMembers")
        )
      )
    )
  )

  private val jsonToPassToTemplate: JsObject = Json.obj(
    "list" -> rows,
    "schemeName" -> schemeName,
    "submitUrl" -> controllers.routes.TaskListController.onPageLoad().url
  )

  override def beforeEach: Unit = {
    super.beforeEach
    when(mockRenderer.render(any(), any())(any())).thenReturn(Future.successful(play.twirl.api.Html("")))
    when(mockCyaHelper.membershipRows(any(), any())).thenReturn(rows)
  }


  "CheckYourAnswers Controller" must {

    "return OK and the correct view for a GET" in {
      mutableFakeDataRetrievalAction.setDataToReturn(userAnswers)
      val templateCaptor = ArgumentCaptor.forClass(classOf[String])
      val jsonCaptor = ArgumentCaptor.forClass(classOf[JsObject])

      val result = route(application, httpGETRequest(httpPathGET)).value

      status(result) mustEqual OK

      verify(mockRenderer, times(1)).render(templateCaptor.capture(), jsonCaptor.capture())(any())

      templateCaptor.getValue mustEqual templateToBeRendered

      jsonCaptor.getValue must containJson(jsonToPassToTemplate)
    }

  }
}
