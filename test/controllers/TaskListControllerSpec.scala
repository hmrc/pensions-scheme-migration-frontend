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

///*
// * Copyright 2021 HM Revenue & Customs
// *
// * Licensed under the Apache License, Version 2.0 (the "License");
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// *
// *     http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// */
//
//package controllers
//
//import controllers.actions._
//import helpers.TaskListHelper
//import models.{EntitySpoke, TaskListLink}
//import org.mockito.Matchers.any
//import org.mockito.Mockito.when
//import org.scalatest.BeforeAndAfterEach
//import org.scalatestplus.mockito.MockitoSugar
//import play.api.test.Helpers._
//import utils.Data._
//import viewmodels.{Message, TaskList, TaskListEntitySection}
//import views.html.taskList
//
//class TaskListControllerSpec extends ControllerSpecBase with BeforeAndAfterEach with MockitoSugar{
//
//  private val view = injector.instanceOf[taskList]
//  private val taskListHelper = mock[TaskListHelper]
//
//  def controller(dataRetrievalAction: DataRetrievalAction = getSchemeName): TaskListController =
//    new TaskListController(
//      messagesApi,
//      FakeAuthAction,
//      dataRetrievalAction,
//      taskListHelper,
//      controllerComponents,
//      view
//    )
//
//  private val beforeYouStartLinkText = Message("messages__schemeTaskList__before_you_start_link_text", schemeName)
//  private val expectedBeforeYouStartSpoke = Seq(EntitySpoke(TaskListLink(beforeYouStartLinkText,
//    controllers.beforeYouStartSpoke.routes.CheckYourAnswersController.onPageLoad.url), Some(false)))
//
//  private val beforeYouStartHeader = Some(Message("messages__schemeTaskList__before_you_start_header"))
//
//  private val schemeDetailsTL = TaskList(schemeName, TaskListEntitySection(None, expectedBeforeYouStartSpoke, beforeYouStartHeader))
//
//  "SchemeTaskList Controller" when {
//
//      "return OK and the correct view" in {
//        when(taskListHelper.taskList(any())).thenReturn(schemeDetailsTL)
//        val result = controller().onPageLoad()(fakeRequest)
//
//        status(result) mustBe OK
//        contentAsString(result) mustBe view(schemeDetailsTL, Some(schemeName))(fakeRequest, messages).toString()
//      }
//    }
//}
//
//
