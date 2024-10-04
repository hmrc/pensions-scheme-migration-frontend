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
import org.mockito.ArgumentMatchersSugar.eqTo
import org.scalatest.{BeforeAndAfterEach, TryValues}
import play.api.mvc.{MessagesControllerComponents, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.twirl.api.Html
import renderer.Renderer
import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents
import uk.gov.hmrc.viewmodels.NunjucksSupport
import uk.gov.hmrc.viewmodels.SummaryList.{Action, Key, Row, Value}
import uk.gov.hmrc.viewmodels.Text.Literal
import utils.Data.{companyDetails, ua}
import utils.UserAnswers

import scala.concurrent.Future

class CheckYourAnswersControllerSpec extends ControllerSpecBase with NunjucksSupport
  with JsonMatchers with TryValues with BeforeAndAfterEach {

  val mockCYAHelper: CommonCYAHelper = mock[CommonCYAHelper]
  val messagesControllerComponents: MessagesControllerComponents = stubMessagesControllerComponents()

  private def controller(dataRetrievalAction: DataRetrievalAction): CheckYourAnswersController =
   new CheckYourAnswersController(
    messagesApi = messagesControllerComponents.messagesApi,
    authenticate = new FakeAuthAction(),
    getData = dataRetrievalAction,
    requireData = new DataRequiredActionImpl,
    cyaHelper = mockCYAHelper,
    controllerComponents = messagesControllerComponents,
    renderer = new Renderer(mockAppConfig, mockRenderer)
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
      when(mockRenderer.render(any(), any())(any())).thenReturn(Future.successful(Html("")))

      val result: Future[Result] = controller(getData).onPageLoad(Index(0), Establisher, entities.Company, Details)(fakeDataRequest(userAnswers))

      status(result) mustBe OK
      verify(mockRenderer).render(eqTo("check-your-answers.njk"), any())(any())
    }

    "return BadRequest when MandatoryAnswerMissingException is thrown" in {
      when(mockCYAHelper.rows(any(), any(), any(), any(), any())(any(), any())).thenThrow(MandatoryAnswerMissingException("Mandatory answer missing"))
      when(mockRenderer.render(eqTo("badRequest.njk"), any())(any())).thenReturn(Future.successful(Html("bad request")))

      val result: Future[Result] = controller(getData).onPageLoad(Index(0), Establisher, entities.Company, Details)(FakeRequest())

      status(result) mustBe BAD_REQUEST
      verify(mockRenderer).render(eqTo("badRequest.njk"), any())(any())
    }

    "return InternalServerError for unexpected exceptions" in {
      when(mockCYAHelper.rows(any(), any(), any(), any(), any())(any(), any())).thenThrow(new RuntimeException("Internal server error"))
      when(mockRenderer.render(eqTo("internalServerError.njk"), any())(any())).thenReturn(Future.successful(Html("internal server error")))

      val result: Future[Result] = controller(getData).onPageLoad(Index(0), Establisher, entities.Company, Details)(FakeRequest())

      status(result) mustBe INTERNAL_SERVER_ERROR
      verify(mockRenderer).render(eqTo("internalServerError.njk"), any())(any())
    }
  }
}
