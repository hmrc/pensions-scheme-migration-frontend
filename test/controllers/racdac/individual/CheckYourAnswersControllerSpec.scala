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

package controllers.racdac.individual

import connectors.{ListOfSchemesConnector, MinimalDetailsConnector}
import controllers.ControllerSpecBase
import controllers.actions._
import helpers.cya.RacDacIndividualCYAHelper
import identifiers.racdac.ContractOrPolicyNumberId
import matchers.JsonMatchers
import models.{Items, ListOfLegacySchemes, RacDac}
import org.mockito.ArgumentMatchers.any
import org.mockito.{ArgumentCaptor, MockitoSugar}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.TryValues.convertTryToSuccessOrFailure
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceableModule
import play.api.libs.json.{JsObject, Json}
import play.api.test.Helpers._
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.viewmodels.SummaryList.{Action, Key, Row, Value}
import uk.gov.hmrc.viewmodels.Text.Literal
import uk.gov.hmrc.viewmodels.{Html, NunjucksSupport}
import utils.Data._
import utils.{Data, UserAnswers}

import scala.concurrent.Future

class CheckYourAnswersControllerSpec extends ControllerSpecBase with BeforeAndAfterEach with MockitoSugar with JsonMatchers with NunjucksSupport  {

  private val mockListOfSchemesConnector = mock[ListOfSchemesConnector]
  private val mockRacDacIndividualCYAHelper = mock[RacDacIndividualCYAHelper]
  private val mutableFakeDataRetrievalAction: MutableFakeDataRetrievalAction = new MutableFakeDataRetrievalAction()
  val extraModules: Seq[GuiceableModule] = Seq(
    bind[RacDacIndividualCYAHelper].toInstance(mockRacDacIndividualCYAHelper),
    bind[ListOfSchemesConnector].toInstance(mockListOfSchemesConnector),
    bind[MinimalDetailsConnector].toInstance(mockMinimalDetailsConnector),
  )
  private val application: Application = applicationBuilderMutableRetrievalAction(mutableFakeDataRetrievalAction,extraModules).build()
  private val templateToBeRendered = "racdac/individual/check-your-answers.njk"

  def listOfSchemes: ListOfLegacySchemes = ListOfLegacySchemes(2, Some(fullSchemes))
  def emptySchemes: ListOfLegacySchemes = ListOfLegacySchemes(0, None)
  private val pstr1: String = "pstr"
  private val pstr2: String = "10000678RD"
  def fullSchemes: List[Items] =
    List(
      Items(pstr1, "2020-10-10", racDac = true, "Test scheme name", "1989-12-12", None),
      Items(pstr2, "2020-10-10", racDac = false, "Test scheme name-2", "2000-10-12", Some("12345678"))
    )
  private def httpPathGET: String = routes.CheckYourAnswersController.onPageLoad.url

  private val rows = Seq(
    Row(
      key = Key(Literal("test-key"), classes = Seq("govuk-!-width-one-half")),
      value = Value(msg"site.not_entered", classes = Seq("govuk-!-width-one-third")),
      actions = List(
        Action(
          content = Html(s"<span  aria-hidden=true >${messages("site.add")}</span>"),
          href = "",
          visuallyHiddenText = None
        )
      )
    )
  )

  private val jsonToPassToTemplate: JsObject = Json.obj(
    "list" -> rows,
    "schemeName" -> schemeName,
    "submitUrl" -> controllers.racdac.routes.DeclarationController.onPageLoad().url,
    "psaName" -> Data.psaName,
    "returnUrl" -> mockAppConfig.psaOverviewUrl
  )

  override def beforeEach: Unit = {
    super.beforeEach
    when(mockUserAnswersCacheConnector.save(any(), any())(any(), any())).thenReturn(Future.successful(Json.obj()))
    when(mockRenderer.render(any(), any())(any())).thenReturn(Future.successful(play.twirl.api.Html("")))
    when(mockRacDacIndividualCYAHelper.detailsRows(any())(any())).thenReturn(rows)

  }


  "CheckYourAnswersController" must {

    "return OK and the correct view for a GET when data present in userAnswers" in {
      val userAnswers: UserAnswers = ua.set(ContractOrPolicyNumberId, "123456789").success.value
      mutableFakeDataRetrievalAction.setDataToReturn(Some(userAnswers))
      when(mockMinimalDetailsConnector.getPSAName(any(),any())).thenReturn(Future.successful(Data.psaName))
      val templateCaptor : ArgumentCaptor[String] = ArgumentCaptor.forClass(classOf[String])
      val jsonCaptor: ArgumentCaptor[JsObject] = ArgumentCaptor.forClass(classOf[JsObject])

      val result = route(application, httpGETRequest(httpPathGET)).value

      status(result) mustEqual OK

      verify(mockRenderer, times(1)).render(templateCaptor.capture(), jsonCaptor.capture())(any())

      templateCaptor.getValue mustEqual templateToBeRendered

      jsonCaptor.getValue must containJson(jsonToPassToTemplate)
    }

    "redirect to List of schemes if lock can not be retrieved " in {
      mutableFakeDataRetrievalAction.setLockToReturn(None)
      val result = route(application, httpGETRequest(httpPathGET)).value

      status(result) mustEqual SEE_OTHER
      redirectLocation(result) mustBe Some(controllers.preMigration.routes.ListOfSchemesController.onPageLoad(RacDac).url)

    }

    "retrieved data from API store it and return  OK" in {
      mutableFakeDataRetrievalAction.setDataToReturn(None)
      mutableFakeDataRetrievalAction.setLockToReturn(Some(Data.migrationLock))
      when(mockMinimalDetailsConnector.getPSAName(any(),any())).thenReturn(Future.successful(Data.psaName))
      when(mockListOfSchemesConnector.getListOfSchemes(any())(any(),any())).thenReturn(Future.successful(Right(listOfSchemes)))
      val templateCaptor : ArgumentCaptor[String] = ArgumentCaptor.forClass(classOf[String])
      val jsonCaptor: ArgumentCaptor[JsObject] = ArgumentCaptor.forClass(classOf[JsObject])

      when( mockUserAnswersCacheConnector.save(any(), any())(any(),any())).thenReturn(Future.successful(Json.obj()))

      val result = route(application, httpGETRequest(httpPathGET)).value


      status(result) mustEqual OK

      verify(mockRenderer, times(1)).render(templateCaptor.capture(), jsonCaptor.capture())(any())

      templateCaptor.getValue mustEqual templateToBeRendered

      jsonCaptor.getValue must containJson(jsonToPassToTemplate)
    }

    "retrieved data from API store it and redirect to List of schemes if racDac Empty" in {
      mutableFakeDataRetrievalAction.setDataToReturn(None)
      mutableFakeDataRetrievalAction.setLockToReturn(Some(Data.migrationLock))
      when(mockListOfSchemesConnector.getListOfSchemes(any())(any(),any())).thenReturn(Future.successful(Right(emptySchemes)))

      val result = route(application, httpGETRequest(httpPathGET)).value

      status(result) mustEqual SEE_OTHER
      redirectLocation(result) mustBe Some(controllers.preMigration.routes.ListOfSchemesController.onPageLoad(RacDac).url)
    }
    "retrieved data from API store it and redirect to List of schemes Failed" in {
      mutableFakeDataRetrievalAction.setDataToReturn(None)
      mutableFakeDataRetrievalAction.setLockToReturn(Some(Data.migrationLock))
      when(mockListOfSchemesConnector.getListOfSchemes(any())(any(),any())).thenReturn(
        Future.successful(Left(HttpResponse(status = BAD_REQUEST,body = "Bad Request"))))

      val result = route(application, httpGETRequest(httpPathGET)).value

      status(result) mustEqual SEE_OTHER
      redirectLocation(result) mustBe Some(mockAppConfig.psaOverviewUrl)
    }

  }
}


