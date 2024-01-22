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

package controllers.establishers.individual.address

import controllers.ControllerSpecBase
import controllers.actions.MutableFakeDataRetrievalAction
import helpers.cya.establishers.individual.EstablisherAddressCYAHelper
import matchers.JsonMatchers
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceableModule
import play.api.libs.json.{JsObject, Json}
import play.api.test.Helpers._
import uk.gov.hmrc.viewmodels.SummaryList.{Action, Key, Row, Value}
import uk.gov.hmrc.viewmodels.Text.Literal
import uk.gov.hmrc.viewmodels.{Html, NunjucksSupport}
import utils.Data.{schemeName, ua}
import utils.UserAnswers

import scala.concurrent.Future

class CheckYourAnswersControllerSpec extends ControllerSpecBase with NunjucksSupport with JsonMatchers {
  private val userAnswers: Option[UserAnswers] = Some(ua)
  private val mutableFakeDataRetrievalAction: MutableFakeDataRetrievalAction = new MutableFakeDataRetrievalAction()

  private val templateToBeRendered = "check-your-answers.njk"
  private val mockCyaHelper: EstablisherAddressCYAHelper = mock[EstablisherAddressCYAHelper]
  private def httpPathGET: String = controllers.establishers.individual.address.routes.CheckYourAnswersController.onPageLoad(0).url
  val extraModules: Seq[GuiceableModule] = Seq(
    bind[EstablisherAddressCYAHelper].toInstance(mockCyaHelper)
  )
  private val application: Application = applicationBuilderMutableRetrievalAction(mutableFakeDataRetrievalAction, extraModules).build()
  private val rows = Seq(
    Row(
      key = Key(Literal("test-key"), classes = Seq("govuk-!-width-one-half")),
      value = Value(msg"site.incomplete", classes = Seq("govuk-!-width-one-third")),
      actions = List(
        Action(
          content = Html(s"<span  aria-hidden=true >${messages("site.add")}</span>"),
          href = "/test-url",
          visuallyHiddenText = Some(Literal("hidden-text"))
        )
      )
    )
  )

  private val jsonToPassToTemplate: JsObject = Json.obj(
    "list" -> rows,
    "schemeName" -> schemeName
  )

  override def beforeEach(): Unit = {
    super.beforeEach()
    when(mockRenderer.render(any(), any())(any())).thenReturn(Future.successful(play.twirl.api.Html("")))
    when(mockCyaHelper.rows(any())(any(), any())).thenReturn(rows)
  }

  "CheckYourAnswers Controller" must {

    "return OK and the correct view for a GET" in {
      mutableFakeDataRetrievalAction.setDataToReturn(userAnswers)
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
