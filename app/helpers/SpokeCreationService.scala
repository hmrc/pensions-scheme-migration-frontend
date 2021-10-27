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
import helpers.spokes.establishers.company._
import helpers.spokes.establishers.individual._
import helpers.spokes.establishers.partnership.{EstablisherPartnerDetails, EstablisherPartnershipAddress, EstablisherPartnershipDetails, _}
import helpers.spokes.trustees.company.{TrusteeCompanyAddress, TrusteeCompanyContactDetails, TrusteeCompanyDetails}
import helpers.spokes.trustees.individual.{TrusteeIndividualAddress, TrusteeIndividualContactDetails, TrusteeIndividualDetails}
import helpers.spokes.trustees.partnership.{TrusteePartnershipAddress, TrusteePartnershipContactDetails, TrusteePartnershipDetails}
import helpers.spokes._
import identifiers.adviser.AdviserNameId
import models.Index._
import models.{Entity, EntitySpoke, Index, TaskListLink}
import play.api.i18n.Messages
import utils.{Enumerable, UserAnswers}

class SpokeCreationService extends Enumerable.Implicits {

  def getBeforeYouStartSpoke(answers: UserAnswers, name: String)
                            (implicit messages: Messages): Seq[EntitySpoke] =
    Seq(createSpoke(answers, BeforeYouStartSpoke, name))

  def membershipDetailsSpoke(answers: UserAnswers, name: String)
                            (implicit messages: Messages): Seq[EntitySpoke] =
    Seq(createSpoke(answers, AboutMembersSpoke, name))

  def aboutSpokes(answers: UserAnswers, name: String)
                 (implicit messages: Messages): Seq[EntitySpoke] =
    Seq(
      createSpoke(answers, AboutMembersSpoke, name),
      createSpoke(answers, BenefitsAndInsuranceSpoke, name)
    )

  def workingKnowledgeSpoke(answers: UserAnswers)
                           (implicit messages: Messages): Seq[EntitySpoke] = {
    if (answers.get(AdviserNameId).isEmpty)
      Seq(
        EntitySpoke(
          link = TaskListLink(
            text = messages("messages__schemeTaskList__add_details_wk"),
            target = controllers.adviser.routes.WhatYouWillNeedController.onPageLoad.url
          ),
          isCompleted = None
        )
      )
    else
      Seq(
        EntitySpoke(
          link = WorkingKnowlegedSpoke.changeLink(answers.get(AdviserNameId).getOrElse("")),
          isCompleted = WorkingKnowlegedSpoke.completeFlag(answers)
        )
      )
  }

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
    Seq(
      createSpoke(answers, EstablisherIndividualDetails(index, answers), name),
      createSpoke(answers, EstablisherIndividualAddress(index, answers), name),
      createSpoke(answers, EstablisherIndividualContactDetails(index, answers), name)
    )
  }

  def getEstablisherCompanySpokes(answers: UserAnswers, name: String, index: Index)
                                 (implicit messages: Messages): Seq[EntitySpoke] = {
    Seq(
      createSpoke(answers, EstablisherCompanyDetails(index, answers), name),
      createSpoke(answers, EstablisherCompanyAddress(index, answers), name),
      createSpoke(answers, EstablisherCompanyContactDetails(index, answers), name),
      createDirectorSpoke(answers.allDirectorsAfterDelete(indexToInt(index)), EstablisherCompanyDirectorDetails(index, answers), name)
    )
  }

  def getEstablisherPartnershipSpokes(answers: UserAnswers, name: String, index: Index)
                                 (implicit messages: Messages): Seq[EntitySpoke] = {
    Seq(
      createSpoke(answers, EstablisherPartnershipDetails(index, answers), name),
      createSpoke(answers, EstablisherPartnershipAddress(index, answers), name),
      createSpoke(answers, EstablisherPartnershipContactDetails(index, answers), name),
      createPartnerSpoke(answers.allPartnersAfterDelete(indexToInt(index)),EstablisherPartnerDetails(index, answers), name)
    )
  }

  def createDirectorSpoke(entityList: Seq[Entity[_]],
                          spoke: Spoke,
                          name: String)(implicit messages: Messages): EntitySpoke = {
    val isComplete: Option[Boolean] = {
      (entityList.isEmpty) match {
        case (false) =>
          Some(entityList.forall(_.isCompleted))
        case (true) =>
          Some(false)
      }
    }
    EntitySpoke(spoke.changeLink(name), isComplete)
  }

  private def createPartnerSpoke(entityList: Seq[Entity[_]],
                          spoke: Spoke,
                          name: String)(implicit messages: Messages): EntitySpoke = {
    val isComplete: Option[Boolean] = {
      entityList.isEmpty  match {
        case false if entityList.size == 1 => Some(false)
        case false =>
          Some(entityList.forall(_.isCompleted))
        case true =>
          Some(false)
      }
    }
    EntitySpoke(spoke.changeLink(name), isComplete)
  }

  def getAddTrusteeHeaderSpokes(answers: UserAnswers, viewOnly: Boolean)
                               (implicit messages: Messages): Seq[EntitySpoke] =
    if (viewOnly)
      Nil
    else if (answers.allTrusteesAfterDelete.isEmpty)
      Seq(
        EntitySpoke(
          link = TaskListLink(
            text = messages("messages__schemeTaskList__sectionTrustees_add_link"),
            target = controllers.trustees.routes.TrusteeKindController.onPageLoad(answers.allTrustees.size).url
          ),
          isCompleted = None
        )
      )
    else
      Seq(
        EntitySpoke(
          link = TaskListLink(
            text = messages("messages__schemeTaskList__sectionTrustees_change_link"),
            target = controllers.trustees.routes.AddTrusteeController.onPageLoad().url
          ),
          isCompleted = None
        )
      )

  def getTrusteeIndividualSpokes(answers: UserAnswers, name: String, index: Index)
                                (implicit messages: Messages): Seq[EntitySpoke] = {
    Seq(
      createSpoke(answers, TrusteeIndividualDetails(index, answers), name),
      createSpoke(answers, TrusteeIndividualAddress(index, answers), name),
      createSpoke(answers, TrusteeIndividualContactDetails(index, answers), name)
    )
  }

  def getTrusteeCompanySpokes(answers: UserAnswers, name: String, index: Index)
                             (implicit messages: Messages): Seq[EntitySpoke] = {
    Seq(
      createSpoke(answers, TrusteeCompanyDetails(index, answers), name),
      createSpoke(answers, TrusteeCompanyAddress(index, answers), name),
      createSpoke(answers, TrusteeCompanyContactDetails(index, answers), name))
  }

  def getTrusteePartnershipSpokes(answers: UserAnswers, name: String, index: Index)
                             (implicit messages: Messages): Seq[EntitySpoke] = {
    Seq(
      createSpoke(answers, TrusteePartnershipDetails(index, answers), name),
      createSpoke(answers, TrusteePartnershipAddress(index, answers), name),
      createSpoke(answers, TrusteePartnershipContactDetails(index, answers), name))
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

