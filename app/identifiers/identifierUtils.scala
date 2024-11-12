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

package identifiers

import models.entities.{Company, EntityType, Individual, Partnership, PensionManagementType, Trustee}
import models.requests.DataRequest
import models.{Index, entities}
import play.api.libs.json.Reads
import utils.{entityTypeError, managementTypeError}

object identifierUtils {

  //scalastyle:off
  def getNameOfEntityType(index: Index,
                          pensionManagementType: PensionManagementType,
                          entityType: EntityType,
                          emptyEntityName: String)(implicit request: DataRequest[_]): String = {

    def getEntityName[T](typedIdentifier: TypedIdentifier[T])
                        (block: T => String)
                        (implicit request: DataRequest[_],
                         reads: Reads[T]): String = {
      request.userAnswers
        .get(typedIdentifier)
        .fold(emptyEntityName)(block)
    }

    pensionManagementType match {
      case entities.Establisher =>
        import identifiers.establishers._
        entityType match {
          case Company =>
            getEntityName(company.CompanyDetailsId(index))(_.companyName)
          case Individual =>
            getEntityName(individual.EstablisherNameId(index))(_.fullName)
          case Partnership =>
            getEntityName(partnership.PartnershipDetailsId(index))(_.partnershipName)
          case e => entityTypeError(e)
        }
      case Trustee =>
        import identifiers.trustees._
        entityType match {
          case Company =>
            getEntityName(company.CompanyDetailsId(index))(_.companyName)
          case Individual =>
            getEntityName(individual.TrusteeNameId(index))(_.fullName)
          case Partnership =>
            getEntityName(partnership.PartnershipDetailsId(index))(_.partnershipName)
          case e => entityTypeError(e)
        }
      case e => managementTypeError(e)
    }
  }
}
