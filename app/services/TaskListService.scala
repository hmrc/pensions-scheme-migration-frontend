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

package services

import controllers.establishers.routes.{AddEstablisherController, EstablisherKindController}
import controllers.trustees.routes.{AddTrusteeController, TrusteeKindController}
import helpers.cya.MandatoryAnswerMissingException
import identifiers.adviser.AdviserNameId
import identifiers.beforeYouStart.{SchemeNameId, WorkingKnowledgeId}
import models.NewTaskListLink
import play.api.i18n.Messages
import uk.gov.hmrc.nunjucks.NunjucksSupport
import utils.UserAnswers

class TaskListService extends NunjucksSupport  {

  val messageKeyPrefix = "messages__newSchemeTaskList__"

  def getLinkKey(value: String, isCompletionDefined: Boolean) :  String = {
    val messagePrefix = s"$messageKeyPrefix" + value

    val linkKey: String =
      if (isCompletionDefined)
        s"${messagePrefix}changeLink"
      else
        s"${messagePrefix}addLink"

    linkKey
  }

  def getSchemeName[A](implicit ua: UserAnswers): String =
    ua.get(SchemeNameId).getOrElse(throw MandatoryAnswerMissingException)

  def basicDetails (implicit ua: UserAnswers, messages: Messages): NewTaskListLink =
  {
    NewTaskListLink(
      text = messages(getLinkKey("basicDetails_", Some(ua.isBeforeYouStartCompleted).isDefined), getSchemeName),
      target = controllers.beforeYouStartSpoke.routes.CheckYourAnswersController.onPageLoad().url,
      visuallyHiddenText = None,
      status = ua.isBeforeYouStartCompleted
    )
  }

  def membershipDetails (implicit ua: UserAnswers, messages: Messages): NewTaskListLink =
  {
    NewTaskListLink(
      text = messages(getLinkKey("membershipDetails_", Some(ua.isMembersComplete).isDefined), getSchemeName),
      target = controllers.aboutMembership.routes.CheckYourAnswersController.onPageLoad().url,
      visuallyHiddenText = None,
      status = ua.isMembersComplete.getOrElse(false)
    )
  }

  def benefitsAndInsuranceDetails (implicit ua: UserAnswers, messages: Messages): NewTaskListLink =
  {
    NewTaskListLink(
      text = messages(getLinkKey("benefitsAndInsuranceDetails_", Some(ua.isBenefitsAndInsuranceComplete).isDefined), getSchemeName),
      target = controllers.benefitsAndInsurance.routes.CheckYourAnswersController.onPageLoad().url,
      visuallyHiddenText = None,
      status = ua.isBenefitsAndInsuranceComplete.getOrElse(false)
    )
  }

  def workingKnowledgeDetails (implicit ua: UserAnswers, messages: Messages): Option[NewTaskListLink] =
  {
    ua.get(WorkingKnowledgeId) match {
      case Some(false) =>
        if (ua.get(AdviserNameId).isEmpty)
          Some(NewTaskListLink(
            text = messages(getLinkKey("workingKnowledge_", false), getSchemeName),
            target = controllers.adviser.routes.WhatYouWillNeedController.onPageLoad().url,
            visuallyHiddenText = None,
            status = ua.isAdviserComplete.getOrElse(false)
          ))
        else
          Some(NewTaskListLink(
            text = messages(getLinkKey("workingKnowledge_", true), ua.get(AdviserNameId).getOrElse("")),
            target = controllers.adviser.routes.CheckYourAnswersController.onPageLoad().url,
            visuallyHiddenText = None,
            status = ua.isAdviserComplete.getOrElse(false)
          ))
      case _ =>
        None
    }
  }

  def establishersDetails (implicit ua: UserAnswers, messages: Messages): NewTaskListLink =
  {
    if (ua.allEstablishersAfterDelete.isEmpty)
      NewTaskListLink(
        text = messages(getLinkKey("establishers_", Some(ua.isEstablishersSectionComplete).isDefined), getSchemeName),
        target = EstablisherKindController.onPageLoad(ua.allEstablishers.size).url,
        visuallyHiddenText = None,
        status = ua.isEstablishersSectionComplete
      )
    else
      NewTaskListLink(
        text = messages(getLinkKey("establishers_", Some(ua.isEstablishersSectionComplete).isDefined), getSchemeName),
        target = AddEstablisherController.onPageLoad().url,
        visuallyHiddenText = None,
        status = ua.isEstablishersSectionComplete
      )
  }

  def trusteesDetails (implicit ua: UserAnswers, messages: Messages): NewTaskListLink =
  {
    if (ua.allTrusteesAfterDelete.isEmpty)
      NewTaskListLink(
        text = messages(getLinkKey("trustees_", Some(ua.isTrusteesSectionComplete).isDefined), getSchemeName),
        target = TrusteeKindController.onPageLoad(ua.allTrustees.size).url,
        visuallyHiddenText = None,
        status = ua.isTrusteesSectionComplete
      )
    else
      NewTaskListLink(
        text = messages(getLinkKey("trustees_", Some(ua.isTrusteesSectionComplete).isDefined), getSchemeName),
        target = AddTrusteeController.onPageLoad().url,
        visuallyHiddenText = None,
        status = ua.isTrusteesSectionComplete
      )
  }

  def completionSpokeCount(implicit ua: UserAnswers, messages: Messages): Int = taskSections.count(x => x.exists(_.status))

  def schemeCompletionStatus(implicit ua: UserAnswers, messages: Messages) : String = {
    if(completionSpokeCount == taskSections.size)
      messages("messages__newSchemeTaskList__schemeStatus_heading", messages("messages__newSchemeTaskList__schemeStatus_complete"))
    else
      messages("messages__newSchemeTaskList__schemeStatus_heading", messages("messages__newSchemeTaskList__schemeStatus_incomplete"))
    }

  def schemeCompletionDescription(implicit ua: UserAnswers, messages: Messages) : String =
    messages("messages__newSchemeTaskList__schemeStatus_desc", completionSpokeCount, taskSections.size)

  def declarationEnabled(implicit ua: UserAnswers): Boolean =
    Seq(
      Some(ua.isBeforeYouStartCompleted),
      ua.isMembersComplete,
      ua.isBenefitsAndInsuranceComplete,
      ua.isWorkingKnowledgeComplete,
      Some(ua.isEstablishersSectionComplete),
      Some(ua.isTrusteesSectionComplete)
    ).forall(_.contains(true))

  def declarationSection (implicit userAnswers: UserAnswers, messages: Messages): NewTaskListLink =
    if(!declarationEnabled)
      NewTaskListLink(
        text = messages("messages__schemeTaskList__sectionDeclaration_incomplete"),
        target = "",
        visuallyHiddenText = None,
        status = declarationEnabled
      )
    else
      NewTaskListLink(
        text = messages("messages__schemeTaskList__declaration_link"),
        target = controllers.routes.DeclarationController.onPageLoad().url,
        visuallyHiddenText = None,
        status = declarationEnabled
      )

  def taskSections (implicit ua: UserAnswers, messages: Messages): Seq[Option[NewTaskListLink]] = {
    if(workingKnowledgeDetails.isEmpty)
      Seq (Some(basicDetails),
        Some(membershipDetails),
        Some(benefitsAndInsuranceDetails),
        Some(establishersDetails),
        Some(trusteesDetails)
      )
    else
      Seq (Some(basicDetails),
        Some(membershipDetails),
        Some(benefitsAndInsuranceDetails),
        workingKnowledgeDetails,
        Some(establishersDetails),
        Some(trusteesDetails)
      )
  }
}
