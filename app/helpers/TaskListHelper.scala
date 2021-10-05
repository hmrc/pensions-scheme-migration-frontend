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

import com.google.inject.Inject
import helpers.cya.MandatoryAnswerMissingException
import identifiers.beforeYouStart.{SchemeNameId, WorkingKnowledgeId}
import identifiers.establishers.company.CompanyDetailsId
import identifiers.establishers.individual.EstablisherNameId
import identifiers.establishers.partnership.PartnershipDetailsId
import identifiers.trustees.company.{CompanyDetailsId => TrusteeCompanyDetailsId}
import identifiers.trustees.individual.TrusteeNameId
import identifiers.trustees.partnership.{PartnershipDetailsId => TrusteePartnershipDetailsId}
import play.api.i18n.Messages
import utils.UserAnswers
import viewmodels._

class TaskListHelper @Inject()(spokeCreationService: SpokeCreationService) {

  def getSchemeName[A](implicit ua: UserAnswers): String =
    ua.get(SchemeNameId).getOrElse(throw MandatoryAnswerMissingException)

  def taskList(viewOnly: Boolean)
              (implicit answers: UserAnswers, messages: Messages): TaskList =
    TaskList(
      getSchemeName,
      beforeYouStartSection,
      aboutSection,
      workingKnowledgeSection,
      addEstablisherHeader(viewOnly),
      addTrusteeHeader(viewOnly),
      establishersSection,
      trusteesSection,
      declarationSection(viewOnly)
    )

  private[helpers] def beforeYouStartSection(implicit userAnswers: UserAnswers, messages: Messages): TaskListEntitySection =
    TaskListEntitySection(None,
      spokeCreationService.getBeforeYouStartSpoke(userAnswers, getSchemeName),
      Some(messages("messages__schemeTaskList__before_you_start_header"))
    )

  private[helpers] def aboutSection(implicit userAnswers: UserAnswers, messages: Messages): TaskListEntitySection =
    TaskListEntitySection(None,
      spokeCreationService.aboutSpokes(userAnswers, getSchemeName),
      Some(messages("messages__schemeTaskList__about_scheme_header", getSchemeName))
    )

  private[helpers] def workingKnowledgeSection(implicit userAnswers: UserAnswers, messages: Messages): Option[TaskListEntitySection] = {
    userAnswers.get(WorkingKnowledgeId) match {
      case Some(false) =>
        Some(TaskListEntitySection(None,
          spokeCreationService.workingKnowledgeSpoke(userAnswers),
          Some(messages("messages__schemeTaskList__working_knowledge_header"))
        ))
      case _ =>
        None
    }
  }

  private[helpers] def addEstablisherHeader(viewOnly: Boolean)
                                           (implicit userAnswers: UserAnswers, messages: Messages): Option[TaskListEntitySection] =
    if (userAnswers.allEstablishersAfterDelete.isEmpty && viewOnly) {
      Some(TaskListEntitySection(None, Nil, None, messages("messages__schemeTaskList__sectionEstablishers_no_establishers")))
    } else {
      spokeCreationService.getAddEstablisherHeaderSpokes(userAnswers, viewOnly) match {
        case Nil =>
          None
        case establisherHeaderSpokes =>
          Some(TaskListEntitySection(None, establisherHeaderSpokes, None))
      }
    }

  private[helpers] def addTrusteeHeader(viewOnly: Boolean)
    (implicit userAnswers: UserAnswers, messages: Messages): Option[TaskListEntitySection] =
    if (userAnswers.allTrusteesAfterDelete.isEmpty && viewOnly) {
      Some(TaskListEntitySection(None, Nil, None, messages("messages__schemeTaskList__sectionTrustees_no_trustees")))
    } else {
      spokeCreationService.getAddTrusteeHeaderSpokes(userAnswers, viewOnly) match {
        case Nil =>
          None
        case trusteeHeaderSpokes =>
          Some(TaskListEntitySection(None, trusteeHeaderSpokes, None))
      }
    }

  protected[helpers] def establishersSection(implicit userAnswers: UserAnswers, messages: Messages): Seq[TaskListEntitySection] = {
    userAnswers.allEstablishers.flatMap {
      establisher =>
        if (establisher.isDeleted)
          None
        else
          establisher.id match {
            case EstablisherNameId(_) =>
              Some(
                TaskListEntitySection(
                  isCompleted = None,
                  entities = spokeCreationService.getEstablisherIndividualSpokes(
                    answers = userAnswers,
                    name = establisher.name,
                    index = establisher.index
                  ),
                  header = Some(establisher.name)
                )
              )

            case CompanyDetailsId(_) =>
              Some(TaskListEntitySection(
                isCompleted = None,
                entities = spokeCreationService.getEstablisherCompanySpokes(
                  answers = userAnswers,
                  name = establisher.name,
                  index = establisher.index
                ),
                header = Some(establisher.name))
              )

            case PartnershipDetailsId(_) =>
              Some(TaskListEntitySection(
                isCompleted = None,
                entities = spokeCreationService.getEstablisherPartnershipSpokes(
                  answers = userAnswers,
                  name = establisher.name,
                  index = establisher.index
                ),
                header = Some(establisher.name))
              )

            case _ =>
              throw new RuntimeException("Unknown section id:" + establisher.id)
          }
    }
  }

  protected[helpers] def trusteesSection(implicit userAnswers: UserAnswers, messages: Messages): Seq[TaskListEntitySection] = {
    val section = userAnswers.allTrustees.flatMap {
      trustee =>
        if (trustee.isDeleted)
          None
        else
          trustee.id match {
            case TrusteeNameId(_) =>
              Some(
                TaskListEntitySection(
                  isCompleted = None,
                  entities = spokeCreationService.getTrusteeIndividualSpokes(
                    answers = userAnswers,
                    name = trustee.name,
                    index = trustee.index
                  ),
                  header = Some(trustee.name)
                )
              )
            case TrusteeCompanyDetailsId(_) =>
              Some(TaskListEntitySection(
                isCompleted = None,
                entities = spokeCreationService.getTrusteeCompanySpokes(
                  userAnswers,
                  trustee.name,
                  trustee.index
                ),
                header = Some(trustee.name))
              )

            case TrusteePartnershipDetailsId(_) =>
              Some(TaskListEntitySection(
                isCompleted = None,
                entities = spokeCreationService.getTrusteePartnershipSpokes(
                  userAnswers,
                  trustee.name,
                  trustee.index
                ),
                header = Some(trustee.name))
              )

            case _ =>
              throw new RuntimeException("Unknown section id:" + trustee.id)
          }
    }
    section
  }

  def declarationEnabled(implicit userAnswers: UserAnswers): Boolean =
    Seq(
      Some(userAnswers.isBeforeYouStartCompleted),
      userAnswers.isMembersComplete,
      userAnswers.isBenefitsAndInsuranceComplete,
      userAnswers.isWorkingKnowledgeComplete,
      Some(userAnswers.isEstablishersSectionComplete),
      Some(userAnswers.isTrusteesSectionComplete)
    ).forall(_.contains(true))

  private[helpers] def declarationSection(viewOnly: Boolean)
                                         (implicit userAnswers: UserAnswers, messages: Messages): Option[TaskListEntitySection] =
    if (viewOnly) {
      None
    } else if(!declarationEnabled) {
      Some(TaskListEntitySection(
        isCompleted = None,
        entities = Nil,
        Some("messages__schemeTaskList__sectionDeclaration_header"),
        "messages__schemeTaskList__sectionDeclaration_incomplete"
      ))
    } else {
      Some(TaskListEntitySection(
        isCompleted = None,
        entities = spokeCreationService.declarationSpoke,
        Some("messages__schemeTaskList__sectionDeclaration_header"),
        "messages__schemeTaskList__sectionDeclaration_incomplete"
      ))
    }

}


