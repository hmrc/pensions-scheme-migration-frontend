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

package controllers.beforeYouStartSpoke

import controllers.ControllerSpecBase
import controllers.actions.{DataRequiredActionImpl, FakeAuthAction}
import helpers.BeforeYouStartCYAHelper
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatestplus.mockito.MockitoSugar
import play.api.mvc.Call
import play.api.test.Helpers._
import utils.Data._
import utils.{CountryOptions, InputOption}
import viewmodels.{AnswerSection, CYAViewModel}
import views.html.checkYourAnswers

class CheckYourAnswersControllerSpec extends ControllerSpecBase with MockitoSugar {

 private val view = injector.instanceOf[checkYourAnswers]

  val options = Seq(InputOption("territory:AE-AZ", "Abu Dhabi"), InputOption("country:AF", "Afghanistan"))
  val testAnswer = "territory:AE-AZ"
  def countryOptions: CountryOptions = new CountryOptions(options)
  val cyaHelper: BeforeYouStartCYAHelper = mock[BeforeYouStartCYAHelper]

  private def controller: CheckYourAnswersController =
    new CheckYourAnswersController(
      messagesApi,
      FakeAuthAction,
      getSchemeName,
      new DataRequiredActionImpl,
      countryOptions,
      cyaHelper,
      controllerComponents,
      view
    )

  private def postUrl: Call = controllers.routes.TaskListController.onPageLoad()

  private def vm = CYAViewModel(
    answerSections = Seq(AnswerSection(None, Nil)),
    href = postUrl,
    schemeName = schemeName,
    hideEditLinks = false,
    hideSaveAndContinueButton = false
  )

  private def viewAsString: String =
    view(vm)(fakeRequest, messages).toString

  "CheckYourAnswers Controller" when {

    "onPageLoad() is called" must {
      "return OK and the correct view with return to tasklist" in {

        when(cyaHelper.viewmodel(any(), any(), any())).thenReturn(vm)

        val result = controller.onPageLoad()(fakeRequest)

        status(result) mustBe OK
        contentAsString(result) mustBe viewAsString
      }
    }
  }
}

