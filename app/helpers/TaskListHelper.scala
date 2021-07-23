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
import identifiers.beforeYouStart.{HaveAnyTrusteesId, SchemeNameId, SchemeTypeId}
import identifiers.establishers.company.CompanyDetailsId
import identifiers.establishers.individual.EstablisherNameId
import identifiers.trustees.company.{CompanyDetailsId => TrusteeCompanyDetailsId}
import identifiers.trustees.individual.TrusteeNameId
import models.SchemeType
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

  private[helpers] def isAvailableTrusteesSection(userAnswers: UserAnswers) =
    !userAnswers.get(SchemeTypeId).exists(st=>st == SchemeType.BodyCorporate || st == SchemeType.GroupLifeDeath) ||
      userAnswers.get(HaveAnyTrusteesId).forall(identity)

  private[helpers] def addTrusteeHeader(viewOnly: Boolean)
    (implicit userAnswers: UserAnswers, messages: Messages): Option[TaskListEntitySection] =
    if (userAnswers.allTrusteesAfterDelete.isEmpty && viewOnly) {
      Some(TaskListEntitySection(None, Nil, None, messages("messages__schemeTaskList__sectionTrustees_no_trustees")))
    } else {
      if (isAvailableTrusteesSection(userAnswers)) {
        spokeCreationService.getAddTrusteeHeaderSpokes(userAnswers, viewOnly) match {
          case Nil =>
            None
          case trusteeHeaderSpokes =>
            Some(TaskListEntitySection(None, trusteeHeaderSpokes, None))
        }
      } else {
        None
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

            case _ =>
              throw new RuntimeException("Unknown section id:" + establisher.id)
          }
    }
  }

  protected[helpers] def trusteesSection(implicit userAnswers: UserAnswers, messages: Messages): Option[Seq[TaskListEntitySection]] = {
    if (isAvailableTrusteesSection(userAnswers)) {
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
                  spokeCreationService.getTrusteeCompanySpokes(userAnswers, trustee.name, trustee.index),
                  Some(trustee.name))
                )

              case _ =>
                throw new RuntimeException("Unknown section id:" + trustee.id)
            }
      }
      Some(section)
    } else {
      None
    }
  }

  def declarationEnabled(implicit userAnswers: UserAnswers): Boolean =
    Seq(
      Some(userAnswers.isBeforeYouStartCompleted),
      userAnswers.isMembersCompleted,
      userAnswers.isBenefitsAndInsuranceCompleted
    ).forall(_.contains(true))

  private[helpers] def declarationSection(viewOnly: Boolean)
                                         (implicit userAnswers: UserAnswers, messages: Messages): Option[TaskListEntitySection] =
    if (viewOnly || !declarationEnabled) {
      None
    } else {
      Some(TaskListEntitySection(
        isCompleted = None,
        entities = spokeCreationService.declarationSpoke,
        Some("messages__schemeTaskList__sectionDeclaration_header"),
        "messages__schemeTaskList__sectionDeclaration_incomplete_v1",
        "messages__schemeTaskList__sectionDeclaration_incomplete_v2"
      ))
    }

}


