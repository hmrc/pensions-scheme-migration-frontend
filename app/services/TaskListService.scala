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

package services

import config.AppConfig
import controllers.establishers.routes.{AddEstablisherController, EstablisherKindController}
import controllers.trustees.routes.{AddTrusteeController, AnyTrusteesController, TrusteeKindController}
import helpers.cya.MandatoryAnswerMissingException
import identifiers.ExpireAtId
import identifiers.adviser.AdviserNameId
import identifiers.beforeYouStart.{SchemeNameId, SchemeTypeId, WorkingKnowledgeId}
import models.{SchemeType, TaskListLink}
import play.api.i18n.Messages
import utils.UserAnswers

import java.text.SimpleDateFormat
import java.time.{LocalDateTime, ZoneOffset}
import java.util.Date
import javax.inject.Inject

class TaskListService @Inject()(appConfig: AppConfig) {

  val messageKeyPrefix = "messages__newSchemeTaskList__"

  private def getLinkKey(value: String, isCompletionDefined: Boolean): String = {
    val messagePrefix = s"$messageKeyPrefix" + value

    val linkKey: String =
      if (isCompletionDefined)
        s"${messagePrefix}changeLink"
      else
        s"${messagePrefix}addLink"

    linkKey
  }

  def getSchemeName[A](implicit ua: UserAnswers): String =
    ua.get(SchemeNameId).getOrElse(throw MandatoryAnswerMissingException(SchemeNameId.toString))

  private def basicDetails(implicit ua: UserAnswers, messages: Messages): TaskListLink = {
    TaskListLink(
      text = messages(getLinkKey("basicDetails_", Some(ua.isBeforeYouStartCompleted).isDefined), getSchemeName),
      target = controllers.beforeYouStartSpoke.routes.CheckYourAnswersController.onPageLoad.url,
      visuallyHiddenText = None,
      status = ua.isBeforeYouStartCompleted
    )
  }

  private def membershipDetails(implicit ua: UserAnswers, messages: Messages): TaskListLink = {
    TaskListLink(
      text = messages(getLinkKey("membershipDetails_", ua.isMembersComplete.isDefined), getSchemeName),
      target = controllers.aboutMembership.routes.CheckYourAnswersController.onPageLoad.url,
      visuallyHiddenText = None,
      status = ua.isMembersComplete.getOrElse(false)
    )
  }

  private def benefitsAndInsuranceDetails(implicit ua: UserAnswers, messages: Messages): TaskListLink = {
    TaskListLink(
      text = messages(getLinkKey("benefitsAndInsuranceDetails_", ua.isBenefitsAndInsuranceComplete.isDefined), getSchemeName),
      target = controllers.benefitsAndInsurance.routes.CheckYourAnswersController.onPageLoad.url,
      visuallyHiddenText = None,
      status = ua.isBenefitsAndInsuranceComplete.getOrElse(false)
    )
  }

  private def workingKnowledgeDetails(implicit ua: UserAnswers, messages: Messages): Option[TaskListLink] = {
    ua.get(WorkingKnowledgeId) match {
      case Some(false) =>
        if (ua.get(AdviserNameId).isEmpty)
          Some(TaskListLink(
            text = messages(getLinkKey("workingKnowledge_", false), getSchemeName),
            target = controllers.adviser.routes.WhatYouWillNeedController.onPageLoad.url,
            visuallyHiddenText = None,
            status = ua.isAdviserComplete.getOrElse(false)
          ))
        else
          Some(TaskListLink(
            text = messages(getLinkKey("workingKnowledge_", true), ua.get(AdviserNameId).getOrElse("")),
            target = controllers.adviser.routes.CheckYourAnswersController.onPageLoad.url,
            visuallyHiddenText = None,
            status = ua.isAdviserComplete.getOrElse(false)
          ))
      case _ =>
        None
    }
  }

  private def establishersDetails(implicit ua: UserAnswers, messages: Messages): TaskListLink = {
    if (ua.allEstablishersAfterDelete.isEmpty)
      TaskListLink(
        text = messages(getLinkKey("establishers_", false), getSchemeName),
        target = EstablisherKindController.onPageLoad(ua.allEstablishers.size).url,
        visuallyHiddenText = None,
        status = ua.isEstablishersSectionComplete
      )
    else
      TaskListLink(
        text = messages(getLinkKey("establishers_", true), getSchemeName),
        target = AddEstablisherController.onPageLoad.url,
        visuallyHiddenText = None,
        status = ua.isEstablishersSectionComplete
      )
  }

  private def trusteesDetails(implicit ua: UserAnswers, messages: Messages): TaskListLink = {
    if (ua.allTrusteesAfterDelete.isEmpty) {
      if (ua.get(SchemeTypeId).contains(SchemeType.SingleTrust))
        TaskListLink(
          text = messages(getLinkKey("trustees_", false), getSchemeName),
          target = TrusteeKindController.onPageLoad(ua.allTrustees.size).url,
          visuallyHiddenText = None,
          status = ua.isTrusteesSectionComplete
        )
      else
        TaskListLink(
          text = messages(getLinkKey("trustees_", ua.isTrusteesSectionComplete), getSchemeName),
          target = AnyTrusteesController.onPageLoad.url,
          visuallyHiddenText = None,
          status = ua.isTrusteesSectionComplete
        )
    }
    else
      TaskListLink(
        text = messages(getLinkKey("trustees_", true), getSchemeName),
        target = AddTrusteeController.onPageLoad.url,
        visuallyHiddenText = None,
        status = ua.isTrusteesSectionComplete
      )
  }

  private def completionSpokeCount(implicit ua: UserAnswers, messages: Messages): Int =
    taskSections.count(_.status)

  def isComplete(implicit ua: UserAnswers, messages: Messages): Boolean = {
    completionSpokeCount == taskSections.size
  }

  def schemeCompletionStatus(implicit ua: UserAnswers, messages: Messages): String = {
    if (isComplete)
      messages("messages__newSchemeTaskList__schemeStatus_heading", messages("messages__newSchemeTaskList__schemeStatus_complete"))
    else
      messages("messages__newSchemeTaskList__schemeStatus_heading", messages("messages__newSchemeTaskList__schemeStatus_incomplete"))
  }

  def schemeCompletionDescription(implicit ua: UserAnswers, messages: Messages): String =
    messages("messages__newSchemeTaskList__schemeStatus_desc", completionSpokeCount, taskSections.size)

  def getExpireAt(implicit ua: UserAnswers): String = {
    val formatter = new SimpleDateFormat("dd MMMM YYYY")
    val calculatedExpiryDate = LocalDateTime.now()
      .plusDays(appConfig.migrationDataTTL).toInstant(ZoneOffset.UTC).toEpochMilli
    formatter.format(new Date(ua.get(ExpireAtId).getOrElse(calculatedExpiryDate)))
  }

  def declarationEnabled(implicit ua: UserAnswers): Boolean = {
    Seq(
      Some(ua.isBeforeYouStartCompleted),
      ua.isMembersComplete,
      ua.isBenefitsAndInsuranceComplete,
      ua.isWorkingKnowledgeComplete,
      Some(ua.isEstablishersSectionComplete),
      Some(ua.isTrusteesSectionComplete)
    ).forall(_.contains(true))
  }

  def declarationSection(implicit userAnswers: UserAnswers, messages: Messages): TaskListLink =
    if (!declarationEnabled)
      TaskListLink(
        text = messages("messages__schemeTaskList__sectionDeclaration_incomplete"),
        target = "",
        visuallyHiddenText = None,
        status = declarationEnabled
      )
    else
      TaskListLink(
        text = messages("messages__schemeTaskList__declaration_link"),
        target = controllers.routes.DeclarationController.onPageLoad.url,
        visuallyHiddenText = None,
        status = declarationEnabled
      )

  def taskSections(implicit ua: UserAnswers, messages: Messages): Seq[TaskListLink] = {
    val seqOtherTasks: Seq[TaskListLink] = Seq(basicDetails, membershipDetails, benefitsAndInsuranceDetails)
    val seqEntityTasks: Seq[TaskListLink] = Seq(establishersDetails, trusteesDetails)

    workingKnowledgeDetails match {
      case Some(workingKnowledgeTaskList) => seqOtherTasks ++ Seq(workingKnowledgeTaskList) ++ seqEntityTasks
      case None => seqOtherTasks ++ seqEntityTasks
    }
  }
}
