/*
 * Copyright 2022 HM Revenue & Customs
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

package services

import base.SpecBase
import identifiers.ExpireAtId
import identifiers.aboutMembership.{CurrentMembersId, FutureMembersId}
import identifiers.adviser.{AdviserNameId, AddressId => AdviserAddressId, EnterEmailId => AdviserEnterEmailId, EnterPhoneId => AdviserEnterPhoneId}
import identifiers.beforeYouStart.{EstablishedCountryId, SchemeNameId, SchemeTypeId, WorkingKnowledgeId}
import identifiers.benefitsAndInsurance._
import identifiers.establishers.EstablisherKindId
import identifiers.establishers.individual.EstablisherNameId
import identifiers.establishers.individual.address.{AddressId, AddressYearsId}
import identifiers.establishers.individual.contact.{EnterEmailId, EnterPhoneId}
import identifiers.establishers.individual.details._
import identifiers.trustees.TrusteeKindId
import identifiers.trustees.individual.TrusteeNameId
import identifiers.trustees.individual.address.{AddressId => TrusteeAddressId, AddressYearsId => TrusteeAddressYearsId}
import identifiers.trustees.individual.contact.{EnterEmailId => TrusteeEnterEmailId, EnterPhoneId => TrusteeEnterPhoneId}
import identifiers.trustees.individual.details._
import matchers.JsonMatchers
import models._
import models.benefitsAndInsurance.{BenefitsProvisionType, BenefitsType}
import models.establishers.EstablisherKind
import models.trustees.TrusteeKind
import org.scalatest.BeforeAndAfterEach
import uk.gov.hmrc.nunjucks.NunjucksSupport
import utils.Data.schemeName
import utils.{Data, Enumerable, UserAnswers}

import java.time.LocalDate

class TaskListServiceSpec extends SpecBase with BeforeAndAfterEach  with NunjucksSupport with JsonMatchers with Enumerable.Implicits {

  private val taskListService = new TaskListService(appConfig)

  "taskSections " must {
    "return all the sections without working knowledge and with status complete" in {
      val ua = trusteesUa(establishersUa(benefitsAndInsuranceUa(membersUa(beforeYouStartUa()))))
      val expectedSections = Seq(basicDetailsSection(), membershipDetailsSection(),
        benefitsAndInsuranceDetails(), establisherSection, trusteeSection)
      val result = taskListService.taskSections(ua, implicitly)
      result mustBe expectedSections
    }

    "return all the sections without working knowledge and with status incomplete with no scheme type" in {
      val ua = UserAnswers().setOrException(SchemeNameId, schemeName)
      val expectedSections = Seq(basicDetailsSection(false), membershipDetailsSection(false, false),
        benefitsAndInsuranceDetails(false, false), establisherSectionIncomplete,
        trusteeSectionIncomplete(controllers.trustees.routes.AnyTrusteesController.onPageLoad.url))
      val result = taskListService.taskSections(ua, implicitly)
      result mustBe expectedSections
    }

    "return all the sections with incomplete status for scheme type as SingleTrust" in {
      val ua = UserAnswers().setOrException(SchemeNameId, schemeName)
        .setOrException(SchemeTypeId, SchemeType.SingleTrust)
      val expectedSections = Seq(basicDetailsSection(false), membershipDetailsSection(false, false),
        benefitsAndInsuranceDetails(false, false), establisherSectionIncomplete,
        trusteeSectionIncomplete(controllers.trustees.routes.TrusteeKindController.onPageLoad(0).url))
      val result = taskListService.taskSections(ua, implicitly)
      result mustBe expectedSections
    }

    "return all the sections with incomplete status for scheme type as BodyCorporate" in {
      val ua = UserAnswers().setOrException(SchemeNameId, schemeName)
        .setOrException(SchemeTypeId, SchemeType.BodyCorporate)
      val expectedSections = Seq(basicDetailsSection(false), membershipDetailsSection(false, false),
        benefitsAndInsuranceDetails(false, false), establisherSectionIncomplete,
        trusteeSectionIncomplete(controllers.trustees.routes.AnyTrusteesController.onPageLoad.url))
      val result = taskListService.taskSections(ua, implicitly)
      result mustBe expectedSections
    }

    "return all the sections with working knowledge and status complete" in {
      val ua = wkUa(trusteesUa(establishersUa(benefitsAndInsuranceUa(membersUa(beforeYouStartUa(false))))))
      val expectedSections = Seq(basicDetailsSection(), membershipDetailsSection(),
        benefitsAndInsuranceDetails(), wkSection, establisherSection, trusteeSection)
      val result = taskListService.taskSections(ua, implicitly)
      result mustBe expectedSections
    }

    "return all the sections with working knowledge incomplete" in {
      val ua = trusteesUa(establishersUa(benefitsAndInsuranceUa(membersUa(beforeYouStartUa(false)))))
      val expectedSections = Seq(basicDetailsSection(), membershipDetailsSection(),
        benefitsAndInsuranceDetails(), wkSectionIncomplete, establisherSection, trusteeSection)
      val result = taskListService.taskSections(ua, implicitly)
      result mustBe expectedSections
    }
  }

  "declarationEnabled" must {

    "return true if all sections are complete" in {
      val ua = trusteesUa(establishersUa(benefitsAndInsuranceUa(membersUa(beforeYouStartUa()))))
      val result = taskListService.declarationEnabled(ua)
      result mustBe true
    }

    "return false if all sections are not complete" in {
      val ua = trusteesUa(establishersUa(benefitsAndInsuranceUa(membersUa(UserAnswers()))))
      val result = taskListService.declarationEnabled(ua)
      result mustBe false
    }
  }

  "declarationSection" must {

    "return no declaration link if declaration is not enabled or sections are not complete" in {
      val ua = trusteesUa(establishersUa(benefitsAndInsuranceUa(membersUa(UserAnswers()))))
      val result = taskListService.declarationSection(ua, implicitly)
      result mustBe TaskListLink(
        text = messages("messages__schemeTaskList__sectionDeclaration_incomplete"),
        target = "",
        visuallyHiddenText = None,
        status = false
      )
    }

    "return declaration link if declaration is enabled or sections are complete" in {
      val ua = trusteesUa(establishersUa(benefitsAndInsuranceUa(membersUa(beforeYouStartUa()))))
      val result = taskListService.declarationSection(ua, implicitly)
      result mustBe TaskListLink(
        text = messages("messages__schemeTaskList__declaration_link"),
        target = controllers.routes.DeclarationController.onPageLoad.url,
        visuallyHiddenText = None,
        status = true
      )
    }
  }

  "schemeCompletionStatus" must {
    "return correct heading when all the sections are complete" in {
      val ua = trusteesUa(establishersUa(benefitsAndInsuranceUa(membersUa(beforeYouStartUa()))))
      val result = taskListService.schemeCompletionStatus(ua, implicitly)
      result mustBe  messages("messages__newSchemeTaskList__schemeStatus_heading", messages("messages__newSchemeTaskList__schemeStatus_complete"))
    }

    "return correct heading when all the sections are not complete" in {
      val ua = trusteesUa(establishersUa(benefitsAndInsuranceUa(membersUa(UserAnswers().setOrException(SchemeNameId, schemeName)))))
      val result = taskListService.schemeCompletionStatus(ua, implicitly)
      result mustBe  messages("messages__newSchemeTaskList__schemeStatus_heading", messages("messages__newSchemeTaskList__schemeStatus_incomplete"))
    }
  }

  "getExpireAt" must {
    "return the correct formatted expire At date" in {
      val ua = UserAnswers().setOrException(ExpireAtId, 1636848000000L)
      val result = taskListService.getExpireAt(ua)
      result mustBe "14 November 2021"
    }
  }

  private def basicDetailsSection(complete: Boolean = true, started: Boolean = true): Option[TaskListLink] = {
    val linkText = if (started) "messages__newSchemeTaskList__basicDetails_changeLink" else
      "messages__newSchemeTaskList__basicDetails_addLink"
    Some(TaskListLink(
      text = messages(linkText, schemeName),
      target = controllers.beforeYouStartSpoke.routes.CheckYourAnswersController.onPageLoad.url,
      visuallyHiddenText = None,
      status = complete
    ))
  }

  private def membershipDetailsSection(complete: Boolean = true, started: Boolean = true): Option[TaskListLink] = {
    val linkText = if (started) "messages__newSchemeTaskList__membershipDetails_changeLink" else
      "messages__newSchemeTaskList__membershipDetails_addLink"
    Some(TaskListLink(
      text = messages(linkText, schemeName),
      target = controllers.aboutMembership.routes.CheckYourAnswersController.onPageLoad.url,
      visuallyHiddenText = None,
      status = complete
    ))
  }

  private def benefitsAndInsuranceDetails(complete: Boolean = true, started: Boolean = true): Option[TaskListLink] = {
    val linkText = if (started) "messages__newSchemeTaskList__benefitsAndInsuranceDetails_changeLink" else
      "messages__newSchemeTaskList__benefitsAndInsuranceDetails_addLink"
    Some(TaskListLink(
      text = messages(linkText, schemeName),
      target = controllers.benefitsAndInsurance.routes.CheckYourAnswersController.onPageLoad.url,
      visuallyHiddenText = None,
      status = complete
    ))
  }

  private val establisherSection = Some(TaskListLink(
    text = messages("messages__newSchemeTaskList__establishers_changeLink", schemeName),
    target = controllers.establishers.routes.AddEstablisherController.onPageLoad.url,
    visuallyHiddenText = None,
    status = true
  ))

  private val establisherSectionIncomplete = Some(TaskListLink(
    text = messages("messages__newSchemeTaskList__establishers_addLink", schemeName),
    target = controllers.establishers.routes.EstablisherKindController.onPageLoad(0).url,
    visuallyHiddenText = None,
    status = false
  ))
  private val trusteeSection = Some(TaskListLink(
    text = messages("messages__newSchemeTaskList__trustees_changeLink", schemeName),
    target = controllers.trustees.routes.AddTrusteeController.onPageLoad.url,
    visuallyHiddenText = None,
    status = true
  ))

  private def trusteeSectionIncomplete(url: String):
  Option[TaskListLink] = Some(TaskListLink(
    text = messages("messages__newSchemeTaskList__trustees_addLink", schemeName),
    target = url,
    visuallyHiddenText = None,
    status = false
  ))

  private val wkSection = Some(TaskListLink(
    text = messages("messages__newSchemeTaskList__workingKnowledge_changeLink", "test adviser"),
    target = controllers.adviser.routes.CheckYourAnswersController.onPageLoad.url,
    visuallyHiddenText = None,
    status = true
  ))

  private val wkSectionIncomplete = Some(TaskListLink(
    text = messages("messages__newSchemeTaskList__workingKnowledge_addLink", "test adviser"),
    target = controllers.adviser.routes.WhatYouWillNeedController.onPageLoad.url,
    visuallyHiddenText = None,
    status = false
  ))


  private def beforeYouStartUa(workingK: Boolean = true) = UserAnswers().setOrException(SchemeNameId, schemeName)
    .setOrException(SchemeTypeId, SchemeType.SingleTrust)
    .setOrException(EstablishedCountryId, "GB")
    .setOrException(WorkingKnowledgeId, workingK)

  private def membersUa(ua: UserAnswers): UserAnswers = ua.setOrException(CurrentMembersId, Members.None)
    .setOrException(FutureMembersId, Members.One)

  private def benefitsAndInsuranceUa(ua: UserAnswers): UserAnswers = ua.setOrException(HowProvideBenefitsId, BenefitsProvisionType.MoneyPurchaseOnly)
    .setOrException(BenefitsTypeId, BenefitsType.CashBalanceBenefits)
    .setOrException(AreBenefitsSecuredId, false)
    .setOrException(IsInvestmentRegulatedId, false)
    .setOrException(IsOccupationalId, false)

  private def establishersUa(ua: UserAnswers): UserAnswers = ua.setOrException(EstablisherKindId(0), EstablisherKind.Individual)
    .setOrException(EstablisherNameId(0), PersonName("a", "b"))
    .setOrException(EstablisherDOBId(0), LocalDate.parse("2001-01-01"))
    .setOrException(EstablisherHasNINOId(0), true)
    .setOrException(EstablisherNINOId(0), ReferenceValue("AB123456C"))
    .setOrException(EstablisherHasUTRId(0), true)
    .setOrException(EstablisherUTRId(0), ReferenceValue("1234567890"))
    .setOrException(EnterEmailId(0), "test@test.com")
    .setOrException(EnterPhoneId(0), "123")
    .setOrException(AddressId(0), Data.address)
    .setOrException(AddressYearsId(0), true)

  private def trusteesUa(ua: UserAnswers): UserAnswers = ua.setOrException(TrusteeKindId(0), TrusteeKind.Individual)
    .setOrException(TrusteeNameId(0), PersonName("a", "b"))
    .setOrException(TrusteeDOBId(0), LocalDate.parse("2001-01-01"))
    .setOrException(TrusteeHasNINOId(0), true)
    .setOrException(TrusteeNINOId(0), ReferenceValue("AB123456C"))
    .setOrException(TrusteeHasUTRId(0), true)
    .setOrException(TrusteeUTRId(0), ReferenceValue("1234567890"))
    .setOrException(TrusteeEnterEmailId(0), "test@test.com")
    .setOrException(TrusteeEnterPhoneId(0), "123")
    .setOrException(TrusteeAddressId(0), Data.address)
    .setOrException(TrusteeAddressYearsId(0), true)

  private def wkUa(ua: UserAnswers): UserAnswers = ua.setOrException(AdviserNameId, "test adviser")
    .setOrException(AdviserEnterEmailId, "test@test.com")
    .setOrException(AdviserEnterPhoneId, "123")
    .setOrException(AdviserAddressId, Data.address)
}
object TaskListServiceSpec {

}




