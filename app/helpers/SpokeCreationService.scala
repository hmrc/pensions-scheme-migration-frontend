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

package helpers

import helpers.spokes.Spoke
import helpers.spokes.establishers.company._
import helpers.spokes.establishers.individual._
import helpers.spokes.establishers.partnership._
import helpers.spokes.trustees.company.{TrusteeCompanyAddress, TrusteeCompanyContactDetails, TrusteeCompanyDetails}
import helpers.spokes.trustees.individual.{TrusteeIndividualAddress, TrusteeIndividualContactDetails, TrusteeIndividualDetails}
import helpers.spokes.trustees.partnership.{TrusteePartnershipAddress, TrusteePartnershipContactDetails, TrusteePartnershipDetails}
import models.Index._
import models._
import models.entities.{EntityType, PensionManagementType}
import play.api.i18n.Messages
import services.DataPrefillService
import utils.{Enumerable, UserAnswers, entityTypeError}

import javax.inject.Inject

class SpokeCreationService @Inject()(dataPrefillService: DataPrefillService) extends Enumerable.Implicits {

  def getAddEstablisherHeaderSpokes(answers: UserAnswers, viewOnly: Boolean)
                                   (implicit messages: Messages): Seq[EntitySpoke] =
    if (viewOnly)
      Nil
    else if (answers.allEstablishersAfterDelete.isEmpty)
      Seq(
        EntitySpoke(
          link = SpokeTaskListLink(
            text = messages("messages__schemeTaskList__sectionEstablishers_add_link"),
            target = controllers.establishers.routes.EstablisherKindController.onPageLoad(answers.allEstablishers.size).url
          ),
          isCompleted = None
        )
      )
    else
      Seq(
        EntitySpoke(
          link = SpokeTaskListLink(
            text = messages("messages__schemeTaskList__sectionEstablishers_change_link"),
            target = controllers.establishers.routes.AddEstablisherController.onPageLoad.url
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
      createDirectorSpoke(answers.allDirectorsAfterDelete(indexToInt(index)), EstablisherCompanyDirectorDetails(index, answers, dataPrefillService), name)
    )
  }

  def getEstablisherPartnershipSpokes(answers: UserAnswers, name: String, index: Index)
                                     (implicit messages: Messages): Seq[EntitySpoke] = {
    Seq(
      createSpoke(answers, EstablisherPartnershipDetails(index, answers), name),
      createSpoke(answers, EstablisherPartnershipAddress(index, answers), name),
      createSpoke(answers, EstablisherPartnershipContactDetails(index, answers), name),
      createPartnerSpoke(answers.allPartnersAfterDelete(indexToInt(index)), EstablisherPartnerDetails(index, answers), name)
    )
  }

  def createDirectorSpoke(entityList: Seq[Entity[?]],
                          spoke: Spoke,
                          name: String)(implicit messages: Messages): EntitySpoke = {
    val isComplete: Option[Boolean] = Some(entityList.nonEmpty && entityList.forall(_.isCompleted))
    EntitySpoke(spoke.changeLink(name), isComplete)
  }

  private def createPartnerSpoke(entityList: Seq[Entity[?]],
                                 spoke: Spoke,
                                 name: String)(implicit messages: Messages): EntitySpoke = {
    val isComplete: Option[Boolean] = {
      entityList.isEmpty match {
        case false if entityList.size == 1 => Some(false)
        case false =>
          Some(entityList.forall(_.isCompleted))
        case true =>
          Some(false)
      }
    }
    EntitySpoke(spoke.changeLink(name), isComplete)
  }

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
        link = SpokeTaskListLink(
          text = messages("messages__schemeTaskList__declaration_link"),
          target = controllers.routes.DeclarationController.onPageLoad.url
        )
      )
    )

  def createSpoke(answers: UserAnswers, spoke: Spoke, name: String)
                 (implicit messages: Messages): EntitySpoke =
    EntitySpoke(
      link = spoke.changeLink(name),
      isCompleted = spoke.completeFlag(answers)
    )


  def getSpokes(pensionManagementType: PensionManagementType, entityType: EntityType, answers: UserAnswers, name: String, index: Index)
               (implicit messages: Messages): Seq[EntitySpoke] = {
    val function = pensionManagementType match {
      case entities.Establisher =>
        entityType match {
          case entities.Company => getEstablisherCompanySpokes
          case entities.Individual => getEstablisherIndividualSpokes
          case entities.Partnership => getEstablisherPartnershipSpokes
          case null => entityTypeError(null)
        }
      case entities.Trustee => entityType match {
        case entities.Company => getTrusteeCompanySpokes
        case entities.Individual => getTrusteeIndividualSpokes
        case entities.Partnership => getTrusteePartnershipSpokes
        case null => entityTypeError(null)
      }
      case null => entityTypeError(null)
    }

    function(answers, name, index)
  }

}

