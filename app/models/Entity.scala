/*
 * Copyright 2023 HM Revenue & Customs
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

package models

import identifiers.establishers.company.CompanyDetailsId
import identifiers.establishers.company.director.DirectorNameId
import identifiers.establishers.individual.EstablisherNameId
import identifiers.establishers.partnership.PartnershipDetailsId
import identifiers.establishers.partnership.partner.PartnerNameId
import identifiers.trustees.company.{CompanyDetailsId => TrusteeCompanyDetailsId}
import identifiers.trustees.individual.TrusteeNameId
import identifiers.trustees.partnership.{PartnershipDetailsId => TrusteePartnershipDetailsId}
import models.establishers.EstablisherKind
import models.trustees.TrusteeKind
import play.api.libs.json.{Format, Json}

sealed trait Entity[ID] {
  def id: ID

  def name: String

  def isDeleted: Boolean

  def isCompleted: Boolean

  def editLink: Option[String]

  def deleteLink: Option[String]

  def index: Int
}

case class EstablisherIndividualEntity(
                                        id: EstablisherNameId,
                                        name: String,
                                        isDeleted: Boolean,
                                        isCompleted: Boolean,
                                        isNewEntity: Boolean,
                                        noOfRecords: Int
                                      ) extends Establisher[EstablisherNameId] {
  override def editLink: Option[String] =
    Some(controllers.establishers.individual.routes.SpokeTaskListController.onPageLoad(index).url)

  override def deleteLink: Option[String] = {
    Some(controllers.establishers.routes.ConfirmDeleteEstablisherController.onPageLoad(id.index, EstablisherKind.Individual).url)
  }

  override def index: Int = id.index
}

object EstablisherIndividualEntity {
  implicit lazy val formats: Format[EstablisherIndividualEntity] = Json.format[EstablisherIndividualEntity]
}

case class EstablisherCompanyEntity(id: CompanyDetailsId, name: String, isDeleted: Boolean,
                                    isCompleted: Boolean, isNewEntity: Boolean, noOfRecords: Int) extends
  Establisher[CompanyDetailsId] {
  override def editLink: Option[String] = Some(controllers.establishers.company.routes.SpokeTaskListController.onPageLoad(index).url)

  override def deleteLink: Option[String] = {
    Some(controllers.establishers.routes.ConfirmDeleteEstablisherController.onPageLoad(id.index, EstablisherKind.Company).url)
  }

  override def index: Int = id.index
}

object EstablisherCompanyEntity {
  implicit lazy val formats: Format[EstablisherCompanyEntity] = Json.format[EstablisherCompanyEntity]
}

case class EstablisherPartnershipEntity(id: PartnershipDetailsId, name: String, isDeleted: Boolean,
                                    isCompleted: Boolean, isNewEntity: Boolean, noOfRecords: Int) extends
  Establisher[PartnershipDetailsId] {
  override def editLink: Option[String] = Some(controllers.establishers.partnership.routes.SpokeTaskListController.onPageLoad(index).url)

  override def deleteLink: Option[String] = {
    Some(controllers.establishers.routes.ConfirmDeleteEstablisherController.onPageLoad(id.index, EstablisherKind.Partnership).url)
  }

  override def index: Int = id.index
}

object EstablisherPartnershipEntity {
  implicit lazy val formats: Format[EstablisherPartnershipEntity] = Json.format[EstablisherPartnershipEntity]
}


sealed trait Establisher[T] extends Entity[T]

object Establisher {
  implicit lazy val formats: Format[Establisher[_]] = Json.format[Establisher[_]]
}

case class TrusteeIndividualEntity(
  id: TrusteeNameId,
  name: String,
  isDeleted: Boolean,
  isCompleted: Boolean,
  isNewEntity: Boolean,
  noOfRecords: Int
) extends Trustee[TrusteeNameId] {
  override def editLink: Option[String] =
    Some(controllers.trustees.individual.routes.SpokeTaskListController.onPageLoad(index).url)

  override def deleteLink: Option[String] =
      Some(controllers.trustees.routes.ConfirmDeleteTrusteeController.onPageLoad(id.index, TrusteeKind.Individual).url)

  override def index: Int = id.index
}

object TrusteeIndividualEntity {
  implicit lazy val formats: Format[TrusteeIndividualEntity] = Json.format[TrusteeIndividualEntity]
}

case class TrusteeCompanyEntity(id: TrusteeCompanyDetailsId, name: String, isDeleted: Boolean,
                                    isCompleted: Boolean, isNewEntity: Boolean, noOfRecords: Int) extends
  Trustee[TrusteeCompanyDetailsId] {
  override def editLink: Option[String] = Some(controllers.trustees.company.routes.SpokeTaskListController.onPageLoad(index).url)

  override def deleteLink: Option[String] = {
      Some(controllers.trustees.routes.ConfirmDeleteTrusteeController.onPageLoad(id.index, TrusteeKind.Company).url)
  }

  override def index: Int = id.index
}

object TrusteeCompanyEntity {
  implicit lazy val formats: Format[TrusteeCompanyEntity] = Json.format[TrusteeCompanyEntity]
}

case class TrusteePartnershipEntity(id: TrusteePartnershipDetailsId, name: String, isDeleted: Boolean,
                                isCompleted: Boolean, isNewEntity: Boolean, noOfRecords: Int) extends
  Trustee[TrusteePartnershipDetailsId] {
  override def editLink: Option[String] = Some(controllers.trustees.partnership.routes.SpokeTaskListController.onPageLoad(index).url)

  override def deleteLink: Option[String] = {
      Some(controllers.trustees.routes.ConfirmDeleteTrusteeController.onPageLoad(id.index, TrusteeKind.Partnership).url)
  }

  override def index: Int = id.index
}

object TrusteePartnershipEntity {
  implicit lazy val formats: Format[TrusteePartnershipEntity] = Json.format[TrusteePartnershipEntity]
}

sealed trait Trustee[T] extends Entity[T]

object Trustee {
  implicit lazy val formats: Format[Trustee[_]] = Json.format[Trustee[_]]
}

case class DirectorEntity(id: DirectorNameId, name: String, isDeleted: Boolean,
                          isCompleted: Boolean, isNewEntity: Boolean, noOfRecords: Int) extends Director[DirectorNameId] {
  override def editLink: Option[String] = (isNewEntity, isCompleted) match {
    case (false, _) => Some(controllers.establishers.company.director.details.routes.CheckYourAnswersController
      .onPageLoad(
        id.establisherIndex, id.directorIndex).url)
    case (_, true) => Some(controllers.establishers.company.director.details.routes.CheckYourAnswersController
      .onPageLoad(
        id.establisherIndex, id.directorIndex).url)
    case (_, false) => Some(controllers.establishers.company.director.routes.DirectorNameController.onPageLoad(
      id.establisherIndex, id.directorIndex, CheckMode).url)
  }

  override def deleteLink: Option[String] = {
    if (noOfRecords >= 1)
      Some(controllers.establishers.company.director.routes.ConfirmDeleteDirectorController.onPageLoad(id.establisherIndex, id.directorIndex).url)
    else
      None
  }

  override def index: Int = id.directorIndex
}

object DirectorEntity {
  implicit lazy val formats: Format[DirectorEntity] = Json.format[DirectorEntity]
}

sealed trait Director[T] extends Entity[T]

case class PartnerEntity(id: PartnerNameId, name: String, isDeleted: Boolean,
                          isCompleted: Boolean, isNewEntity: Boolean, noOfRecords: Int) extends Partner[PartnerNameId] {
  override def editLink: Option[String] = (isNewEntity, isCompleted) match {
    case (false, _) => Some(controllers.establishers.partnership.partner.details.routes.CheckYourAnswersController
      .onPageLoad(
        id.establisherIndex, id.partnerIndex).url)
    case (_, true) => Some(controllers.establishers.partnership.partner.details.routes.CheckYourAnswersController
      .onPageLoad(
        id.establisherIndex, id.partnerIndex).url)
    case (_, false) => Some(controllers.establishers.partnership.partner.routes.PartnerNameController.onPageLoad(
      id.establisherIndex, id.partnerIndex, CheckMode).url)
  }

  override def deleteLink: Option[String] = {
    if (noOfRecords >= 1)
      Some(controllers.establishers.partnership.partner.routes.ConfirmDeletePartnerController.onPageLoad(id.establisherIndex, id.partnerIndex).url)
    else
      None
  }

  override def index: Int = id.partnerIndex
}

object PartnerEntity {
  implicit lazy val formats: Format[PartnerEntity] = Json.format[PartnerEntity]
}

sealed trait Partner[T] extends Entity[T]



