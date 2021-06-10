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
import identifiers.beforeYouStart.SchemeNameId
import identifiers.establishers.individual.EstablisherNameId
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
      establishersSection,
      declarationSection(viewOnly)
    )

  private[helpers] def beforeYouStartSection(implicit userAnswers: UserAnswers, messages: Messages): TaskListEntitySection =
    TaskListEntitySection(None,
      spokeCreationService.getBeforeYouStartSpoke(userAnswers, getSchemeName),
      Some(messages("messages__schemeTaskList__before_you_start_header"))
    )

  private[helpers] def aboutSection(implicit userAnswers: UserAnswers, messages: Messages): TaskListEntitySection =
    TaskListEntitySection(None,
      spokeCreationService.membershipDetailsSpoke(userAnswers, getSchemeName),
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
                    index = Some(establisher.index)
                  ),
                  header = Some(establisher.name)
                )
              )
            case _ =>
              throw new RuntimeException("Unknown section id:" + establisher.id)
          }
    }
  }

  def declarationEnabled(implicit userAnswers: UserAnswers): Boolean =
    Seq(
      Some(userAnswers.isBeforeYouStartCompleted),
      userAnswers.isMembersCompleted
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


