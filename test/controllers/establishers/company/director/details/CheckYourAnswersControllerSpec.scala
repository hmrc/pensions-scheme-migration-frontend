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

package controllers.establishers.company.director.details

import controllers.ControllerSpecBase
import controllers.actions._
import helpers.EstablisherCompanyDirectorDetailsCYAHelper
import identifiers.establishers.company.director._
import identifiers.establishers.company.director.details.{DirectorDOBId, DirectorEnterUTRId, DirectorHasNINOId, DirectorHasUTRId, DirectorNINOId, DirectorNoNINOReasonId, DirectorNoUTRReasonId}
import matchers.JsonMatchers
import models.requests.DataRequest
import models.{PersonName, ReferenceValue}
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{times, verify, when}
import org.scalatest.TryValues
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.{AnyContent, Result}
import play.api.test.Helpers.{status, _}
import play.twirl.api.Html
import renderer.Renderer
import uk.gov.hmrc.nunjucks.NunjucksSupport
import uk.gov.hmrc.viewmodels.SummaryList
import utils.Data.{schemeName, ua}
import utils.{Enumerable, UserAnswers}

import java.time.LocalDate
import scala.concurrent.Future

class CheckYourAnswersControllerSpec
  extends ControllerSpecBase
    with NunjucksSupport
    with JsonMatchers
    with Enumerable.Implicits
    with TryValues {

  private val cyaHelper =
    new EstablisherCompanyDirectorDetailsCYAHelper

  private val userAnswers =
    ua
      .set(DirectorNameId(0,0), PersonName("Jane", "Doe")).success.value
      .set(DirectorDOBId(0,0), LocalDate.parse("2001-01-01")).success.value

  private val uaNinoUtr: UserAnswers =
    userAnswers
      .set(DirectorHasNINOId(0,0), true).success.value
      .set(DirectorNINOId(0,0), ReferenceValue("AB123456C")).success.value
      .set(DirectorHasUTRId(0,0), true).success.value
      .set(DirectorEnterUTRId(0,0), ReferenceValue("1234567890")).success.value

  private val uaReasons: UserAnswers =
    userAnswers
      .set(DirectorHasNINOId(0,0), false).success.value
      .set(DirectorNoNINOReasonId(0,0), "Reason").success.value
      .set(DirectorHasUTRId(0,0), false).success.value
      .set(DirectorNoUTRReasonId(0,0), "Reason").success.value

  private def rows(implicit request: DataRequest[AnyContent]): Seq[SummaryList.Row] =
    cyaHelper.detailsRows(0,0)

  private val templateToBeRendered: String =
    "check-your-answers.njk"

  val commonJson: JsObject =
    Json.obj(
      "schemeName" ->schemeName,
      "submitUrl"  -> "/migrate-pension-scheme/establisher/1/company/list-of-directors"
    )

  private def jsonToPassToTemplate(answers: Seq[SummaryList.Row]): JsObject =
    Json.obj("list" -> Json.toJson(answers))

  private def controller(
                          dataRetrievalAction: DataRetrievalAction
                        ): CheckYourAnswersController =
    new CheckYourAnswersController(
      messagesApi          = messagesApi,
      authenticate         = new FakeAuthAction(),
      getData              = dataRetrievalAction,
      requiredData          = new DataRequiredActionImpl,
      cyaHelper            = cyaHelper,
      controllerComponents = controllerComponents,
      renderer             = new Renderer(mockAppConfig, mockRenderer)
    )

  "CheckYourAnswersController" must {
    "return OK and the correct view for a GET when user has entered NINO and UTR" in {
      when(mockRenderer.render(any(), any())(any()))
        .thenReturn(Future.successful(Html("")))

      val templateCaptor = ArgumentCaptor.forClass(classOf[String])
      val jsonCaptor = ArgumentCaptor.forClass(classOf[JsObject])

      val getData = new FakeDataRetrievalAction(Some(uaNinoUtr))
      val result: Future[Result] =
        controller(getData)
          .onPageLoad(0,0)(fakeDataRequest(uaNinoUtr))

      status(result) mustBe OK

      verify(mockRenderer, times(1))
        .render(templateCaptor.capture(), jsonCaptor.capture())(any())

      val json: JsObject =
        commonJson ++ jsonToPassToTemplate(rows(fakeDataRequest(uaNinoUtr)))

      templateCaptor.getValue mustEqual templateToBeRendered

      jsonCaptor.getValue must containJson(json)
    }

    "return OK and the correct view for a GET when user has not entered NINO and UTR" in {
      when(mockRenderer.render(any(), any())(any()))
        .thenReturn(Future.successful(Html("")))

      val templateCaptor = ArgumentCaptor.forClass(classOf[String])
      val jsonCaptor = ArgumentCaptor.forClass(classOf[JsObject])

      val getData = new FakeDataRetrievalAction(Some(uaReasons))
      val result: Future[Result] =
        controller(getData)
          .onPageLoad(0,0)(fakeDataRequest(uaReasons))

      status(result) mustBe OK

      verify(mockRenderer, times(1))
        .render(templateCaptor.capture(), jsonCaptor.capture())(any())

      val json: JsObject =
        commonJson ++ jsonToPassToTemplate(rows(fakeDataRequest(uaReasons)))

      templateCaptor.getValue mustEqual templateToBeRendered

      jsonCaptor.getValue must containJson(json)
    }
  }
}
