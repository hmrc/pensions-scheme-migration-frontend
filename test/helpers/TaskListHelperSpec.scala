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

package helpers

import base.SpecBase
import models._
import org.mockito.Matchers.any
import org.mockito.Mockito.when
import org.scalatest.MustMatchers
import org.scalatestplus.mockito.MockitoSugar
import utils.Data.{completeUserAnswers, schemeName, ua}
import utils.UserAnswers
import viewmodels.{Message, TaskListEntitySection}

class TaskListHelperSpec extends SpecBase with MustMatchers with MockitoSugar {

  private val mockSpokeCreationService = mock[SpokeCreationService]
  private val helper = new TaskListHelper(mockSpokeCreationService)
  private val beforeYouStartLinkText = messages("messages__schemeTaskList__before_you_start_link_text", schemeName)
  private val membershipDetailsLinkText = messages("messages__schemeTaskList__about_members_link_text", schemeName)
  private val declarationLinkText = messages("messages__schemeTaskList__declaration_link")
  private val beforeYouStartHeader = Some(Message("messages__schemeTaskList__before_you_start_header"))
  private val aboutHeader = Some(Message("messages__schemeTaskList__about_scheme_header", schemeName))
  private val declarationHeader = Some("messages__schemeTaskList__sectionDeclaration_header")
  private val declarationP1 = List("messages__schemeTaskList__sectionDeclaration_incomplete_v1",
    "messages__schemeTaskList__sectionDeclaration_incomplete_v2")

  private val expectedBeforeYouStartSpoke = Seq(EntitySpoke(TaskListLink(beforeYouStartLinkText,
    controllers.beforeYouStartSpoke.routes.CheckYourAnswersController.onPageLoad().url), Some(false)))
  private val expectedMembershipDetailsSpoke = EntitySpoke(TaskListLink(membershipDetailsLinkText,
    controllers.aboutMembership.routes.CheckYourAnswersController.onPageLoad().url), Some(false))
  private val expectedDeclarationSpoke = EntitySpoke(TaskListLink(declarationLinkText,
    controllers.routes.DeclarationController.onPageLoad().url), Some(false))

  implicit val userAnswers: UserAnswers = ua
  "h1" must {
    "display appropriate heading" in {

      helper.taskList(false).h1 mustBe schemeName
    }
  }

  "beforeYouStartSection " must {
    "return correct the correct entity section " in {
      when(mockSpokeCreationService.getBeforeYouStartSpoke(any(), any())(any())).thenReturn(expectedBeforeYouStartSpoke)
      val expectedBeforeYouStartSection = TaskListEntitySection(None, expectedBeforeYouStartSpoke, beforeYouStartHeader)

      helper.beforeYouStartSection mustBe expectedBeforeYouStartSection
    }
  }

  "aboutSection " must {
    "return correct the correct entity section " in {
      when(mockSpokeCreationService.membershipDetailsSpoke(any(), any())(any())).thenReturn(Seq(expectedMembershipDetailsSpoke))
      val expectedAboutSection = TaskListEntitySection(None, Seq(expectedMembershipDetailsSpoke), aboutHeader)

      helper.aboutSection mustBe expectedAboutSection
    }
  }

  "declarationSection " must {
    "return correct the correct entity section " in {
      when(mockSpokeCreationService.declarationSpoke(any())).thenReturn(Seq(expectedDeclarationSpoke))
      val expectedDeclarationSection = TaskListEntitySection(None, Seq(expectedDeclarationSpoke), declarationHeader, declarationP1: _*)

      helper.declarationSection(false)(completeUserAnswers, implicitly) mustBe Some(expectedDeclarationSection)
    }
  }
}