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

package controllers

import connectors.LegacySchemeDetailsConnector
import controllers.actions._
import matchers.JsonMatchers
import models.{Scheme, TaskListLink}
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.scalatest.BeforeAndAfterEach
import org.mockito.Mockito._
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceableModule
import play.api.libs.json.{JsObject, JsValue, Json}
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.TaskListService
import utils.Data
import utils.Data._
import views.html.TaskListView

import scala.concurrent.Future
class TaskListControllerSpec extends ControllerSpecBase with BeforeAndAfterEach  with JsonMatchers {

  private val mockTaskListService = mock[TaskListService]
  private val mutableFakeDataRetrievalAction: MutableFakeDataRetrievalAction = new MutableFakeDataRetrievalAction()
  val extraModules: Seq[GuiceableModule] = Seq(
    bind[TaskListService].to(mockTaskListService),
    bind[LegacySchemeDetailsConnector].toInstance(mockLegacySchemeDetailsConnector)
  )
  private val application: Application = applicationBuilderMutableRetrievalAction(mutableFakeDataRetrievalAction, extraModules).build()

  private def httpPathGET: String = controllers.routes.TaskListController.onPageLoad.url

  private val basicDetailsSection = TaskListLink("Change Test scheme name basic details",
    controllers.beforeYouStartSpoke.routes.CheckYourAnswersController.onPageLoad.url, None, false)
  private val membershipDetailsSection = TaskListLink("Change Test scheme name membership details",
    controllers.aboutMembership.routes.CheckYourAnswersController.onPageLoad.url, None, true)

  private val declarationSection =
    TaskListLink(
    text = "You must complete every section before you can declare.",
    target = "",
    visuallyHiddenText = None,
    status = false
  )

  private val schemeDetailsTL : Seq[TaskListLink] =
    Seq(basicDetailsSection,
      membershipDetailsSection)


  val json = Json.obj(
    "schemeStatus" -> "Scheme Details are incomplete",
    "schemeStatusDescription" -> "You have completed 1 of 2 sections",
    "expiryDate" -> "14 November 2021",
    "taskSections" -> schemeDetailsTL,
    "schemeName" -> schemeName,
    "declarationEnabled" -> false,
    "declaration" -> declarationSection,
    "returnUrl" -> controllers.routes.PensionSchemeRedirectController.onPageLoad.url
  )
 val expectedJson = Json.obj("anyTrustees" -> false)

  val itemList : JsValue = json

  override def beforeEach(): Unit = {
    super.beforeEach()
    when(mockUserAnswersCacheConnector.save(any(), any())(any(), any())).thenReturn(Future.successful(Json.obj()))
    when(mockTaskListService.schemeCompletionStatus(any(), any())).thenReturn("Scheme Details are incomplete")
    when(mockTaskListService.schemeCompletionDescription(any(), any())).thenReturn("You have completed 1 of 2 sections")
    when(mockTaskListService.taskSections(any(), any())).thenReturn(schemeDetailsTL)
    when(mockTaskListService.getSchemeName(any())).thenReturn(schemeName)
    when(mockTaskListService.declarationEnabled(any())).thenReturn(false)
    when(mockTaskListService.declarationSection(any(), any())).thenReturn(declarationSection)
    when(mockTaskListService.getExpireAt(any())).thenReturn("14 November 2021")
    when(mockLegacySchemeDetailsConnector.getLegacySchemeDetails(any(), any())(any(), any())).thenReturn(Future.successful(Right(itemList)))
  }

  val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, httpPathGET)

  "TaskList Controller" must {

    "return OK and the correct view for a GET when data present in userAnswers" in {
      mutableFakeDataRetrievalAction.setDataToReturn(Some(ua))


      val result = route(application, httpGETRequest(httpPathGET)).value

      status(result) mustEqual OK

      val view = application.injector.instanceOf[TaskListView].apply(
        "Scheme Details are incomplete",
        "You have completed 1 of 2 sections",
        "14 November 2021",
        schemeDetailsTL,
        schemeName,
        declarationEnabled = false,
        declarationSection,
        controllers.routes.PensionSchemeRedirectController.onPageLoad.url,
        isCompleted = false
      )(request, messages)

      compareResultAndView(result, view)
    }

    "redirect to List of schemes if lock can not be retrieved " in {
      mutableFakeDataRetrievalAction.setLockToReturn(None)
      val result = route(application, httpGETRequest(httpPathGET)).value

      status(result) mustEqual SEE_OTHER
      redirectLocation(result) mustBe Some(controllers.preMigration.routes.ListOfSchemesController.onPageLoad(Scheme).url)

    }

    "retrieved data from API store it and return OK" in {
      mutableFakeDataRetrievalAction.setDataToReturn(None)
      mutableFakeDataRetrievalAction.setLockToReturn(Some(Data.migrationLock))

      val result = route(application, httpGETRequest(httpPathGET)).value

      status(result) mustEqual OK

      val view = application.injector.instanceOf[TaskListView].apply(
        "Scheme Details are incomplete",
        "You have completed 1 of 2 sections",
        "14 November 2021",
        schemeDetailsTL,
        schemeName,
        declarationEnabled = false,
        declarationSection,
        controllers.routes.PensionSchemeRedirectController.onPageLoad.url,
        isCompleted = false
      )(request, messages)

      compareResultAndView(result, view)
    }

    "retrieved data from API store it and correct value for AnyTrusteesId and return  OK" in {
      mutableFakeDataRetrievalAction.setDataToReturn(None)
      mutableFakeDataRetrievalAction.setLockToReturn(Some(Data.migrationLock))
      when(mockLegacySchemeDetailsConnector.getLegacySchemeDetails(any(), any())(any(), any())).thenReturn(Future.successful(Right(itemList)))

      val jsonCaptor: ArgumentCaptor[JsObject] = ArgumentCaptor.forClass(classOf[JsObject])
      val result = route(application, httpGETRequest(httpPathGET)).value

      status(result) mustEqual OK

      verify(mockUserAnswersCacheConnector, times(1)).save(any(), jsonCaptor.capture())(any(), any())

      jsonCaptor.getValue must containJson(expectedJson)
    }

  }
}


