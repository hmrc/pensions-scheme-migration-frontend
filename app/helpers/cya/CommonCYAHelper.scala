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

package helpers.cya

import models.entities.{EntityType, JourneyType, PensionManagementType}
import models.requests.DataRequest
import models.{Index, entities}
import play.api.i18n.Messages
import play.api.mvc.AnyContent
import uk.gov.hmrc.viewmodels.SummaryList
import utils.{entityTypeError, journeyTypeError, managementTypeError}

import javax.inject.{Inject, Singleton}

@Singleton
class CommonCYAHelper @Inject() (
                                eca: establishers.company.EstablisherCompanyAddressCYAHelper,
                                ecc: establishers.company.EstablisherCompanyContactDetailsCYAHelper,
                                ecd: establishers.company.EstablisherCompanyDetailsCYAHelper,
                                ecdd: establishers.company.EstablisherCompanyDirectorDetailsCYAHelper,
                                eia: establishers.individual.EstablisherAddressCYAHelper,
                                eic: establishers.individual.EstablisherContactDetailsCYAHelper,
                                eid: establishers.individual.EstablisherDetailsCYAHelper,
                                epa: establishers.partnership.EstablisherPartnershipAddressCYAHelper,
                                epc: establishers.partnership.EstablisherPartnershipContactDetailsCYAHelper,
                                epd: establishers.partnership.EstablisherPartnershipDetailsCYAHelper,
                                eppd: establishers.partnership.EstablisherPartnerDetailsCYAHelper,
                                tca: trustees.company.TrusteeAddressCYAHelper,
                                tcc: trustees.company.TrusteeCompanyContactDetailsCYAHelper,
                                tcd: trustees.company.TrusteeCompanyDetailsCYAHelper,
                                tia: trustees.individual.TrusteeAddressCYAHelper,
                                tic: trustees.individual.TrusteeContactDetailsCYAHelper,
                                tid: trustees.individual.TrusteeDetailsCYAHelper,
                                tpa: trustees.partnership.TrusteeAddressCYAHelper,
                                tpc: trustees.partnership.TrusteeContactDetailsCYAHelper,
                                tpd: trustees.partnership.TrusteePartnershipDetailsCYAHelper
                                ) {
  def rows(index: Index,
           pensionManagementType: PensionManagementType,
           entityType: EntityType,
           entityRepresentativeIndex: Option[Index],
           journeyType: JourneyType)
          (implicit request: DataRequest[AnyContent], messages: Messages): Seq[SummaryList.Row] = {

    def establisherResolve = {
      entityType match {
        case entities.Company =>
          entityRepresentativeIndex match {
            case Some(directorIndex) => ecdd.detailsRows(index, directorIndex)
            case None =>
              journeyType match {
                case entities.Address => eca.rows(index)
                case entities.Contacts => ecc.contactDetailsRows(index)
                case entities.Details => ecd.detailsRows(index)
                case e => journeyTypeError(e)
              }
          }
        case entities.Individual =>
          journeyType match {
            case entities.Address => eia.rows(index)
            case entities.Contacts => eic.contactDetailsRows(index)
            case entities.Details => eid.detailsRows(index)
            case e => journeyTypeError(e)
          }
        case entities.Partnership =>
          entityRepresentativeIndex match {
            case Some(partnerIndex) => eppd.detailsRows(index, partnerIndex)
            case None =>
              journeyType match {
                case entities.Address => epa.rows(index)
                case entities.Contacts => epc.contactDetailsRows(index)
                case entities.Details => epd.detailsRows(index)
                case e => journeyTypeError(e)
              }
          }
        case e => entityTypeError(e)
      }
    }

    def trusteeResolve = {
      entityType match {
        case entities.Company =>
          journeyType match {
            case entities.Address => tca.rows(index)
            case entities.Contacts => tcc.contactDetailsRows(index)
            case entities.Details => tcd.detailsRows(index)
            case e => journeyTypeError(e)
          }
        case entities.Individual =>
          journeyType match {
            case entities.Address => tia.rows(index)
            case entities.Contacts => tic.contactDetailsRows(index)
            case entities.Details => tid.detailsRows(index)
            case e => journeyTypeError(e)
          }
        case entities.Partnership =>
          journeyType match {
            case entities.Address => tpa.rows(index)
            case entities.Contacts => tpc.contactDetailsRows(index)
            case entities.Details => tpd.detailsRows(index)
            case e => journeyTypeError(e)
          }
        case e => entityTypeError(e)
      }
    }

    pensionManagementType match {
      case entities.Establisher =>
        establisherResolve
      case entities.Trustee => trusteeResolve
      case e => managementTypeError(e)
    }
  }
}
