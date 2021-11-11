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

package controllers

import connectors.LegacySchemeDetailsConnector
import controllers.actions._
import helpers.TaskListHelper
import matchers.JsonMatchers
import models.{EntitySpoke, Scheme, TaskListLink}
import org.mockito.ArgumentMatchers.any
import org.mockito.{ArgumentCaptor, MockitoSugar}
import org.scalatest.BeforeAndAfterEach
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceableModule
import play.api.libs.json.{JsObject, JsValue, Json}
import play.api.test.Helpers._
import play.twirl.api.Html
import utils.Data
import utils.Data._
import viewmodels.{Message, TaskList, TaskListEntitySection}

import scala.concurrent.Future

class TaskListControllerSpec extends ControllerSpecBase with BeforeAndAfterEach with MockitoSugar with JsonMatchers {

  private val mockTaskListHelper = mock[TaskListHelper]
  private val mutableFakeDataRetrievalAction: MutableFakeDataRetrievalAction = new MutableFakeDataRetrievalAction()
  val extraModules: Seq[GuiceableModule] = Seq(
    bind[TaskListHelper].to(mockTaskListHelper),
    bind[LegacySchemeDetailsConnector].toInstance(mockLegacySchemeDetailsConnector)
  )
  private val application: Application = applicationBuilderMutableRetrievalAction(mutableFakeDataRetrievalAction, extraModules).build()
  private val templateToBeRendered = "taskList.njk"


  private def httpPathGET: String = controllers.routes.TaskListController.onPageLoad.url

  private val expectedSpoke = Seq(EntitySpoke(TaskListLink(Message("messages__schemeTaskList__before_you_start_link_text", schemeName),
    controllers.routes.IndexController.onPageLoad.url), Some(false)))

  private val testHeader = Some(Message("messages__schemeTaskList__before_you_start_header"))
  private val testSection = TaskListEntitySection(None, expectedSpoke, testHeader)

  private val schemeDetailsTL = TaskList(
    h1 = schemeName,
    beforeYouStart = testSection,
    about = testSection,
    workingKnowledge = Some(testSection),
    addEstablisherHeader = Some(testSection),
    addTrusteeHeader = Some(testSection),
    establishers = Seq(testSection),
    trustees = Seq(testSection),
    declaration = Some(testSection)
  )

  val json = Json.obj(
    "taskSections" -> schemeDetailsTL,
    "schemeName" -> schemeName
  )

  val itemList : JsValue = Json.obj(
    "taskSections" -> schemeDetailsTL,
    "schemeName" -> schemeName
  )
  override def beforeEach: Unit = {
    super.beforeEach
    when(mockUserAnswersCacheConnector.save(any(), any())(any(), any())).thenReturn(Future.successful(Json.obj()))
    when(mockTaskListHelper.taskList(any())(any(), any())).thenReturn(schemeDetailsTL)
    when(mockTaskListHelper.getSchemeName(any())).thenReturn(schemeName)
    when(mockRenderer.render(any(), any())(any())).thenReturn(Future.successful(Html("")))
    when(mockLegacySchemeDetailsConnector.getLegacySchemeDetails(any(), any())(any(), any())).thenReturn(Future.successful(Right(itemList)))
  }


  "TaskList Controller" must {

    "return OK and the correct view for a GET when data present in userAnswers" in {
      mutableFakeDataRetrievalAction.setDataToReturn(Some(ua))
      val templateCaptor : ArgumentCaptor[String] = ArgumentCaptor.forClass(classOf[String])
      val jsonCaptor: ArgumentCaptor[JsObject] = ArgumentCaptor.forClass(classOf[JsObject])

      val result = route(application, httpGETRequest(httpPathGET)).value

      status(result) mustEqual OK

      verify(mockRenderer, times(1)).render(templateCaptor.capture(), jsonCaptor.capture())(any())

      templateCaptor.getValue mustEqual templateToBeRendered

      jsonCaptor.getValue must containJson(json)
    }

    "redirect to List of schemes if lock can not be retrieved " in {
      mutableFakeDataRetrievalAction.setLockToReturn(None)
      val result = route(application, httpGETRequest(httpPathGET)).value

      status(result) mustEqual SEE_OTHER
      redirectLocation(result) mustBe Some(controllers.preMigration.routes.ListOfSchemesController.onPageLoad(Scheme).url)

    }

    "retrieved data from API store it and return  OK" in {
      mutableFakeDataRetrievalAction.setDataToReturn(None)
      mutableFakeDataRetrievalAction.setLockToReturn(Some(Data.migrationLock))

      val templateCaptor : ArgumentCaptor[String] = ArgumentCaptor.forClass(classOf[String])
      val jsonCaptor: ArgumentCaptor[JsObject] = ArgumentCaptor.forClass(classOf[JsObject])
      val result = route(application, httpGETRequest(httpPathGET)).value

      status(result) mustEqual OK

      verify(mockRenderer, times(1)).render(templateCaptor.capture(), jsonCaptor.capture())(any())

      templateCaptor.getValue mustEqual templateToBeRendered

    }

  }
}


