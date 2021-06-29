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

import controllers.establishers.routes._
import helpers.spokes.establishers.individual._
import helpers.spokes.{BeforeYouStartSpoke, AboutMembersSpoke, Spoke, BenefitsAndInsuranceSpoke}
import models.{TaskListLink, EntitySpoke, Index}
import play.api.i18n.Messages
import utils.{UserAnswers, Enumerable}

class SpokeCreationService extends Enumerable.Implicits {

  def getBeforeYouStartSpoke(answers: UserAnswers, name: String)
                            (implicit messages: Messages): Seq[EntitySpoke] =
    Seq(createSpoke(answers, BeforeYouStartSpoke, name))

  def membershipDetailsSpoke(answers: UserAnswers, name: String)
                            (implicit messages: Messages): Seq[EntitySpoke] =
    Seq(createSpoke(answers, AboutMembersSpoke, name))
  def aboutSpokes(answers: UserAnswers, name: String)(implicit messages: Messages): Seq[EntitySpoke] =
    Seq(
      createSpoke(answers, AboutMembersSpoke, name),
      createSpoke(answers, BenefitsAndInsuranceSpoke, name)
    )

  def getAddEstablisherHeaderSpokes(answers: UserAnswers, viewOnly: Boolean)
                                   (implicit messages: Messages): Seq[EntitySpoke] =
    if (viewOnly)
      Nil
    else if (answers.allEstablishersAfterDelete.isEmpty)
      Seq(
        EntitySpoke(
          link = TaskListLink(
            text = messages("messages__schemeTaskList__sectionEstablishers_add_link"),
            target = EstablisherKindController.onPageLoad(answers.allEstablishers.size).url
          ),
          isCompleted = None
        )
      )
    else
      Seq(
        EntitySpoke(
          link = TaskListLink(
            text = messages("messages__schemeTaskList__sectionEstablishers_change_link"),
            target = AddEstablisherController.onPageLoad().url
          ),
          isCompleted = None
        )
      )

  def getEstablisherIndividualSpokes(answers: UserAnswers, name: String, index: Index)
                                    (implicit messages: Messages): Seq[EntitySpoke] = {
//    val isEstablisherNew = answers.get(IsEstablisherNewId(indexToInt(index.getOrElse(Index(0))))).getOrElse(false)
    Seq(
      createSpoke(answers, EstablisherIndividualDetails(index, answers), name),
      createSpoke(answers, EstablisherIndividualAddress(index, answers), name),
      createSpoke(answers, EstablisherIndividualContactDetails, name)
    )
  }

  def declarationSpoke(implicit messages: Messages): Seq[EntitySpoke] =
    Seq(
      EntitySpoke(
        link = TaskListLink(
          text = messages("messages__schemeTaskList__declaration_link"),
          target = controllers.routes.DeclarationController.onPageLoad().url
        )
      )
    )

  def createSpoke(answers: UserAnswers, spoke: Spoke, name: String)
                 (implicit messages: Messages): EntitySpoke =
    EntitySpoke(
      link = spoke.changeLink(name),
      isCompleted = spoke.completeFlag(answers)
    )

}

