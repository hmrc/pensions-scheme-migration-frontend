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
import identifiers.beforeYouStart.SchemeTypeId
import identifiers.establishers.company.CompanyDetailsId
import identifiers.establishers.individual.EstablisherNameId
import identifiers.establishers.partnership.PartnershipDetailsId
import identifiers.trustees.partnership.{PartnershipDetailsId => TrusteePartnershipDetailsId}
import identifiers.establishers.{EstablisherKindId, IsEstablisherNewId}
import identifiers.trustees.individual.TrusteeNameId
import identifiers.trustees.{IsTrusteeNewId, TrusteeKindId}
import models._
import models.establishers.EstablisherKind
import models.trustees.TrusteeKind
import org.mockito.Matchers.any
import org.mockito.Mockito.when
import org.scalatest.MustMatchers
import org.scalatestplus.mockito.MockitoSugar
import utils.Data.{completeUserAnswers, schemeName, ua}
import utils.{Enumerable, UserAnswers}
import viewmodels.{Message, TaskListEntitySection}

class TaskListHelperSpec extends SpecBase with MustMatchers with MockitoSugar with Enumerable.Implicits {

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
      when(mockSpokeCreationService.aboutSpokes(any(), any())(any())).thenReturn(Seq(expectedMembershipDetailsSpoke))
      val expectedAboutSection = TaskListEntitySection(None, Seq(expectedMembershipDetailsSpoke), aboutHeader)

      helper.aboutSection mustBe expectedAboutSection
    }
  }

  "addEstablishersHeaderSection" must {
    "show no establishers message if viewOnly and no establishers found" in {
      val expectedSection = Some(TaskListEntitySection(None, Nil, None, messages("messages__schemeTaskList__sectionEstablishers_no_establishers")))
      helper.addEstablisherHeader(true) mustBe expectedSection
    }

    "show be none if no spoke is returned by service" in {
      when(mockSpokeCreationService.getAddEstablisherHeaderSpokes(any(), any())(any())).thenReturn(Nil)
      helper.addEstablisherHeader(false) mustBe None
    }

    "show a section with the spoke returned by service" in {
      val establisherHeaderSpokes = Seq(EntitySpoke(TaskListLink(messages("messages__schemeTaskList__sectionEstablishers_change_link"),
          controllers.establishers.routes.AddEstablisherController.onPageLoad.url), None))
      when(mockSpokeCreationService.getAddEstablisherHeaderSpokes(any(), any())(any())).thenReturn(establisherHeaderSpokes)
      val expectedSection = Some(TaskListEntitySection(None, establisherHeaderSpokes, None))
      helper.addEstablisherHeader(false) mustBe expectedSection
    }
  }

  "addTrusteesHeaderSection" must {
    "show no trustees message if viewOnly and no trustees found" in {
      val expectedSection = Some(TaskListEntitySection(None, Nil, None, messages("messages__schemeTaskList__sectionTrustees_no_trustees")))
      helper.addTrusteeHeader(true) mustBe expectedSection
    }

    "show be none if no spoke is returned by service" in {
      when(mockSpokeCreationService.getAddTrusteeHeaderSpokes(any(), any())(any())).thenReturn(Nil)
      helper.addTrusteeHeader(false) mustBe None
    }

    "show a section with the spoke returned by service where scheme type is single trust" in {
      val trusteeHeaderSpokes = Seq(EntitySpoke(TaskListLink(messages("messages__schemeTaskList__sectionTrustees_change_link"),
        controllers.trustees.routes.AddTrusteeController.onPageLoad.url), None))
      when(mockSpokeCreationService.getAddTrusteeHeaderSpokes(any(), any())(any())).thenReturn(trusteeHeaderSpokes)
      val expectedSection = Some(TaskListEntitySection(None, trusteeHeaderSpokes, None))
      val userAnswers: UserAnswers = ua
        .setOrException(SchemeTypeId, SchemeType.SingleTrust)
      helper.addTrusteeHeader(false)(userAnswers, implicitly) mustBe expectedSection
    }
  }

  "establishers Section" must {
    "return Nil if all establishers are deleted" in {
      val userAnswers = ua.set(EstablisherKindId(0), EstablisherKind.Individual).flatMap(
        _.set(EstablisherNameId(0), PersonName("a", "b", true)).flatMap(
          _.set(IsEstablisherNewId(0), true).flatMap(
            _.set(EstablisherKindId(1), EstablisherKind.Individual).flatMap(
              _.set(EstablisherNameId(1), PersonName("c", "d", true)).flatMap(
                _.set(IsEstablisherNewId(1), true).flatMap(
                  _.set(EstablisherKindId(2), EstablisherKind.Company).flatMap(
                    _.set(CompanyDetailsId(2), CompanyDetails("test company", true)).flatMap(
                      _.set(IsEstablisherNewId(2), true).flatMap(
                        _.set(EstablisherKindId(3), EstablisherKind.Partnership).flatMap(
                        _.set(PartnershipDetailsId(3), PartnershipDetails("test partnership", true)).flatMap(
                          _.set(IsEstablisherNewId(3), true)
                    )
                  )
                )
              )))))))).get
      helper.establishersSection(userAnswers, messages) mustBe Nil
    }

    "return seq of sections if all establishers are not deleted" in {
      val userAnswers = ua.set(EstablisherKindId(0), EstablisherKind.Individual).flatMap(
        _.set(EstablisherNameId(0), PersonName("a", "b", true)).flatMap(
          _.set(IsEstablisherNewId(0), true).flatMap(
            _.set(EstablisherKindId(1), EstablisherKind.Individual).flatMap(
              _.set(EstablisherNameId(1), PersonName("c", "d")).flatMap(
                _.set(IsEstablisherNewId(1), true).flatMap(
                  _.set(EstablisherKindId(2), EstablisherKind.Company).flatMap(
                    _.set(CompanyDetailsId(2), CompanyDetails("test company")).flatMap(
                      _.set(IsEstablisherNewId(2), true).flatMap(
                        _.set(EstablisherKindId(3), EstablisherKind.Partnership).flatMap(
                        _.set(PartnershipDetailsId(3), PartnershipDetails("test partnership")).flatMap(
                          _.set(IsEstablisherNewId(3), true)
              ))))))))))).get

      val expectedSection =  Seq(TaskListEntitySection(None, null, Some("c d")),
        TaskListEntitySection(None, null, Some("test company")), TaskListEntitySection(None, null, Some("test partnership")))
      helper.establishersSection(userAnswers, messages) mustBe expectedSection
    }
  }

  "trustees Section" must {
    "return Nil if all trustees are deleted" in {
      val userAnswers = ua.set(TrusteeKindId(0), TrusteeKind.Individual).flatMap(
        _.set(TrusteeNameId(0), PersonName("a", "b", true)).flatMap(
          _.set(IsTrusteeNewId(0), true).flatMap(
            _.set(TrusteeKindId(1), TrusteeKind.Individual).flatMap(
              _.set(TrusteeNameId(1), PersonName("c", "d", true)).flatMap(
                _.set(IsTrusteeNewId(1), true).flatMap(
                  _.set(TrusteeKindId(2), TrusteeKind.Partnership).flatMap(
                    _.set(TrusteePartnershipDetailsId(2), PartnershipDetails("ab partnership", true))
                  )
                )
              ))))).get
      helper.trusteesSection(userAnswers, messages) mustBe Nil
    }

    "return seq of sections if all trustees are not deleted" in {
      val userAnswers = ua.set(TrusteeKindId(0), TrusteeKind.Individual).flatMap(
        _.set(TrusteeNameId(0), PersonName("a", "b", true)).flatMap(
          _.set(IsTrusteeNewId(0), true).flatMap(
            _.set(TrusteeKindId(1), TrusteeKind.Individual).flatMap(
              _.set(TrusteeNameId(1), PersonName("c", "d")).flatMap(
                _.set(IsTrusteeNewId(1), true).flatMap(
                  _.set(TrusteeKindId(2), TrusteeKind.Partnership).flatMap(
                    _.set(TrusteePartnershipDetailsId(2), PartnershipDetails("ab partnership"))
              ))))))).get

      val expectedSection =  Seq(TaskListEntitySection(None, null, Some("c d")),
        TaskListEntitySection(None, null, Some("ab partnership")))
      helper.trusteesSection(userAnswers, messages) mustBe expectedSection
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
