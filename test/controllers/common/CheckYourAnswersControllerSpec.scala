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

package controllers.common

import controllers.ControllerSpecBase
import controllers.actions.{DataRequiredActionImpl, DataRetrievalAction, FakeAuthAction, FakeDataRetrievalAction}
import helpers.cya.{CommonCYAHelper, MandatoryAnswerMissingException}
import identifiers.establishers.company.CompanyDetailsId
import matchers.JsonMatchers
import models.entities.{Details, Establisher}
import models.{Index, entities}
import org.mockito.ArgumentMatchers.any
import org.scalatest.RecoverMethods.recoverToSucceededIf
import org.scalatest.{BeforeAndAfterEach, TryValues}
import play.api.i18n.MessagesApi
import play.api.mvc.{MessagesControllerComponents, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents
import uk.gov.hmrc.viewmodels.{MessageInterpolators, NunjucksSupport}
import uk.gov.hmrc.viewmodels.SummaryList.{Action, Key, Row, Value}
import uk.gov.hmrc.viewmodels.Text.Literal
import utils.Data.{companyDetails, ua}
import utils.{TwirlMigration, UserAnswers}
import views.html.CheckYourAnswersView

import scala.concurrent.Future

class CheckYourAnswersControllerSpec extends ControllerSpecBase
  with JsonMatchers with TryValues with BeforeAndAfterEach {

  val mockCYAHelper: CommonCYAHelper = mock[CommonCYAHelper]
  val messagesControllerComponents: MessagesControllerComponents = stubMessagesControllerComponents()

  private def controller(dataRetrievalAction: DataRetrievalAction): CheckYourAnswersController =
   new CheckYourAnswersController(
    messagesApi = app.injector.instanceOf[MessagesApi],
    authenticate = new FakeAuthAction(),
    getData = dataRetrievalAction,
    requireData = new DataRequiredActionImpl,
    cyaHelper = mockCYAHelper,
    controllerComponents = messagesControllerComponents,
     view = app.injector.instanceOf[CheckYourAnswersView]
  )

  override def beforeEach(): Unit = {
    reset(mockCYAHelper)
  }

  private val index: Index = Index(0)
  private val userAnswers: UserAnswers = ua.set(CompanyDetailsId(index), companyDetails).success.value
  private val rows = Seq(
    Row(
      key = Key(Literal("test-key"), classes = Seq("govuk-!-width-one-half")),
      value = Value(msg"site.incomplete", classes = Seq("govuk-!-width-one-third")),
      actions = List(
        Action(
          content = uk.gov.hmrc.viewmodels.Html(s"<span aria-hidden=true >${messages("site.add")}</span>"),
          href = "/test-url",
          visuallyHiddenText = Some(Literal("hidden-text"))
        )
      )
    )
  )
  private val getData = new FakeDataRetrievalAction(Some(userAnswers))

  "CheckYourAnswersController" should {

    "return OK and render the check-your-answers template when successful" in {
      when(mockCYAHelper.rows(any(), any(), any(), any(), any())(any(), any())).thenReturn(rows)

      val req = fakeDataRequest(userAnswers)
      val result: Future[Result] = controller(getData).onPageLoad(Index(0), Establisher, entities.Company, Details)(req)
      val view = app.injector.instanceOf[CheckYourAnswersView].apply(
        controllers.common.routes.SpokeTaskListController.onPageLoad(index, Establisher, entities.Company).url,
        "Test scheme name",
        TwirlMigration.summaryListRow(rows)
      )(req, implicitly)


      status(result) mustBe OK
      compareResultAndView(result, view)
    }

    "return throw an exception when MandatoryAnswerMissingException is thrown" in {
      when(mockCYAHelper.rows(any(), any(), any(), any(), any())(any(), any())).thenThrow(MandatoryAnswerMissingException("Mandatory answer missing"))

      val result: Future[Result] = controller(getData).onPageLoad(Index(0), Establisher, entities.Company, Details)(FakeRequest())

      recoverToSucceededIf[MandatoryAnswerMissingException](result)
    }
  }
}
