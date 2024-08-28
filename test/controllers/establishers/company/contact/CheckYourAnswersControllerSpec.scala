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

package controllers.establishers.company.contact

import controllers.ControllerSpecBase
import controllers.actions._
import helpers.cya.establishers.company.EstablisherCompanyContactDetailsCYAHelperForTwirl
import identifiers.establishers.company.CompanyDetailsId
import identifiers.establishers.company.contact.{EnterEmailId, EnterPhoneId}
import matchers.JsonMatchers
import models.CompanyDetails
import models.requests.DataRequest
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.scalatest.TryValues
import play.api.inject.guice.GuiceableModule
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.{AnyContent, Result}
import play.api.test.Helpers.{status, _}
import play.twirl.api.Html
import renderer.Renderer
import uk.gov.hmrc.nunjucks.NunjucksSupport
import utils.Data.ua
import utils.{Enumerable, UserAnswers}
import views.html.CheckYourAnswersView
import play.api.inject.bind
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow

import scala.concurrent.Future

class CheckYourAnswersControllerSpec
  extends ControllerSpecBase
    with NunjucksSupport
    with JsonMatchers
    with Enumerable.Implicits
    with TryValues {

  private val cyaHelper = new EstablisherCompanyContactDetailsCYAHelperForTwirl
  private val uaEmailPhone: UserAnswers = ua
    .set(CompanyDetailsId(0), CompanyDetails("test")).success.value
    .set(EnterEmailId(0), "test@test.com").success.value
    .set(EnterPhoneId(0), "11111").success.value

  private def rows(implicit request: DataRequest[AnyContent]): Seq[SummaryListRow] =
    cyaHelper.contactDetailsRows(0)

  private val templateToBeRendered: String =
    "check-your-answers.njk"

  def commonJson: JsObject =
    Json.obj(
      "schemeName" -> "Test scheme name",
      "submitUrl" -> controllers.establishers.company.routes.SpokeTaskListController.onPageLoad(0).url
    )

  private def jsonToPassToTemplate(answers: Seq[SummaryListRow]): JsObject =
    Json.obj("list" -> Json.toJson(answers))

  private val mockCyaHelper: EstablisherCompanyContactDetailsCYAHelperForTwirl = mock[EstablisherCompanyContactDetailsCYAHelperForTwirl]

  val extraModules: Seq[GuiceableModule] = Seq(
    bind[EstablisherCompanyContactDetailsCYAHelperForTwirl].toInstance(mockCyaHelper)
  )
  private val mutableFakeDataRetrievalAction: MutableFakeDataRetrievalAction = new MutableFakeDataRetrievalAction()
  val application = applicationBuilderMutableRetrievalAction(mutableFakeDataRetrievalAction, extraModules).build()
  val view: CheckYourAnswersView = application.injector.instanceOf[CheckYourAnswersView]

  private def controller(
                          dataRetrievalAction: DataRetrievalAction
                        ): CheckYourAnswersController =
    new CheckYourAnswersController(
      messagesApi = messagesApi,
      authenticate = new FakeAuthAction(),
      getData = dataRetrievalAction,
      requireData = new DataRequiredActionImpl,
      cyaHelper = cyaHelper,
      controllerComponents = controllerComponents,
      renderer = new Renderer(mockAppConfig, mockRenderer),
      checkYourAnswersView = view
    )

  "CheckYourAnswersController" must {
    "return OK and the correct view for a GET when user has entered email and phone number" in {
      when(mockRenderer.render(any(), any())(any())).thenReturn(Future.successful(Html("")))

      val templateCaptor : ArgumentCaptor[String] = ArgumentCaptor.forClass(classOf[String])
      val jsonCaptor: ArgumentCaptor[JsObject] = ArgumentCaptor.forClass(classOf[JsObject])

      val getData = new FakeDataRetrievalAction(Some(uaEmailPhone))
      val result: Future[Result] = controller(getData).onPageLoad(0)(fakeDataRequest(uaEmailPhone))

      status(result) mustBe OK
      verify(mockRenderer, times(1)).render(templateCaptor.capture(), jsonCaptor.capture())(any())

      val json: JsObject = commonJson ++ jsonToPassToTemplate(rows(fakeDataRequest(uaEmailPhone)))

      templateCaptor.getValue mustEqual templateToBeRendered

      jsonCaptor.getValue must containJson(json)
    }
  }
}
