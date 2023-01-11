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

package utils.datacompletion

import identifiers.establishers.company.details._
import identifiers.establishers.company.director.details._
import identifiers.establishers.company.director.{address => directorAddress, contact => directorContact}
import identifiers.establishers.company.{OtherDirectorsId, address => companyAddress, contact => companyContact}
import identifiers.establishers.individual.address.{AddressId, AddressYearsId, PreviousAddressId}
import identifiers.establishers.individual.contact.{EnterEmailId, EnterPhoneId}
import identifiers.establishers.individual.details._
import identifiers.establishers.partnership.partner.details._
import identifiers.establishers.partnership.partner.{address => partnerAddress, contact => partnerContact}
import identifiers.establishers.partnership.{OtherPartnersId, address => partnershipAddress, contact => partnershipContact, details => partnershipDetails}
import utils.UserAnswers

trait DataCompletionEstablishers extends DataCompletion {

  self: UserAnswers =>

  def isEstablisherIndividualComplete(index: Int): Boolean =
    isComplete(
      Seq(
        isEstablisherIndividualDetailsComplete(index),
        isEstablisherIndividualAddressComplete(index),
        isEstablisherIndividualContactDetailsComplete(index)
      )
    ).getOrElse(false)

  def isEstablisherIndividualDetailsComplete(index: Int): Option[Boolean] =
    isComplete(
      Seq(
        isAnswerComplete(EstablisherDOBId(index)),
        isAnswerComplete(EstablisherHasNINOId(index), EstablisherNINOId(index), Some(EstablisherNoNINOReasonId(index))),
        isAnswerComplete(EstablisherHasUTRId(index), EstablisherUTRId(index), Some(EstablisherNoUTRReasonId(index)))
      )
    )

  def isEstablisherIndividualAddressComplete(index: Int): Option[Boolean] =
    isAddressComplete(AddressId(index), PreviousAddressId(index), AddressYearsId(index), None)

  def isEstablisherIndividualContactDetailsComplete(index: Int): Option[Boolean] =
    isContactDetailsComplete(EnterEmailId(index), EnterPhoneId(index))

  def isEstablisherCompanyAndDirectorsComplete(index: Int): Boolean = {
    val allDirectors = allDirectorsAfterDelete(index)

    val allDirectorsCompleted = allDirectors.nonEmpty && allDirectors.forall(_.isCompleted) &&
      (allDirectors.size < 10 || get(OtherDirectorsId(index)).isDefined)

    allDirectorsCompleted & isEstablisherCompanyComplete(index)
  }

  def isEstablisherCompanyComplete(index: Int): Boolean = {
    isComplete(
      Seq(isEstablisherCompanyDetailsComplete(index),
        isEstablisherCompanyAddressComplete(index),
        isEstablisherCompanyContactDetailsComplete(index)))
      .getOrElse(false)
  }

  def isEstablisherCompanyDetailsComplete(index: Int): Option[Boolean] = {
    isComplete(
      Seq(
        isAnswerComplete(HaveCompanyNumberId(index),CompanyNumberId(index),Some(NoCompanyNumberReasonId(index))),
        isAnswerComplete(HaveUTRId(index), CompanyUTRId(index), Some(NoUTRReasonId(index))),
        isAnswerComplete(HaveVATId(index),VATId(index),None),
        isAnswerComplete(HavePAYEId(index),PAYEId(index),None)
      )
    )
  }

  def isEstablisherCompanyAddressComplete(index: Int): Option[Boolean] =
    isAddressComplete(
      companyAddress.AddressId(index),
      companyAddress.PreviousAddressId(index),
      companyAddress.AddressYearsId(index),
      Some(companyAddress.TradingTimeId(index)))

  def isEstablisherCompanyContactDetailsComplete(index: Int): Option[Boolean] =
    isContactDetailsComplete(companyContact.EnterEmailId(index), companyContact.EnterPhoneId(index))

  def isDirectorComplete(estIndex: Int, dirIndex: Int): Boolean =
    isComplete(Seq(
      isDirectorDetailsComplete(estIndex, dirIndex),
      isDirectorAddressComplete(estIndex, dirIndex),
      isContactDetailsComplete(directorContact.EnterEmailId(estIndex, dirIndex), directorContact.EnterPhoneId(estIndex, dirIndex))
    )
    ).getOrElse(false)

  def isDirectorDetailsComplete(estIndex: Int, dirIndex: Int): Option[Boolean]  =
    isComplete(Seq(
      isAnswerComplete(DirectorDOBId(estIndex, dirIndex)),
      isAnswerComplete(DirectorHasNINOId(estIndex, dirIndex), DirectorNINOId(estIndex, dirIndex), Some(DirectorNoNINOReasonId(estIndex, dirIndex))),
      isAnswerComplete(DirectorHasUTRId(estIndex, dirIndex), DirectorEnterUTRId(estIndex, dirIndex), Some(DirectorNoUTRReasonId(estIndex, dirIndex)))
    ))

  def isDirectorAddressComplete(estIndex: Int, dirIndex: Int): Option[Boolean] =
    isAddressComplete(
      directorAddress.AddressId(estIndex, dirIndex),
      directorAddress.PreviousAddressId(estIndex, dirIndex),
      directorAddress.AddressYearsId(estIndex, dirIndex),
      None)



  def isEstablisherPartnershipAndPartnersComplete(index: Int): Boolean = {
    val allPartners = allPartnersAfterDelete(index)
    val allPartnersCompleted = allPartners.size > 1 && allPartners.forall(_.isCompleted) &&
    (allPartners.size < 10 || get(OtherPartnersId(index)).isDefined)
    allPartnersCompleted & isEstablisherPartnershipComplete(index)
  }

  def isEstablisherPartnershipComplete(index: Int): Boolean =
    isComplete(Seq(
      isEstablisherPartnershipDetailsComplete(index),
      isEstablisherPartnershipAddressComplete(index),
      isEstablisherPartnershipContactDetailsComplete(index))).getOrElse(false)

  def isEstablisherPartnershipDetailsComplete(index: Int): Option[Boolean] =
    isComplete(
      Seq(
        isAnswerComplete(partnershipDetails.HaveUTRId(index), partnershipDetails.PartnershipUTRId(index), Some(partnershipDetails.NoUTRReasonId(index))),
        isAnswerComplete(partnershipDetails.HaveVATId(index),partnershipDetails.VATId(index),None),
        isAnswerComplete(partnershipDetails.HavePAYEId(index),partnershipDetails.PAYEId(index),None)
      )
    )

  def isEstablisherPartnershipAddressComplete(index: Int): Option[Boolean] =
    isAddressComplete(
      partnershipAddress.AddressId(index),
      partnershipAddress.PreviousAddressId(index),
      partnershipAddress.AddressYearsId(index),
      Some(partnershipAddress.TradingTimeId(index)))

  def isEstablisherPartnershipContactDetailsComplete(index: Int): Option[Boolean] =
    isContactDetailsComplete(partnershipContact.EnterEmailId(index), partnershipContact.EnterPhoneId(index))

  def isPartnerComplete(estIndex: Int, dirIndex: Int): Boolean =
    isComplete(Seq(
      isPartnerDetailsComplete(estIndex, dirIndex),
      isPartnerAddressComplete(estIndex, dirIndex),
      isContactDetailsComplete(partnerContact.EnterEmailId(estIndex, dirIndex), partnerContact.EnterPhoneId(estIndex, dirIndex))
    )
    ).getOrElse(false)

  def isPartnerDetailsComplete(estIndex: Int, dirIndex: Int): Option[Boolean]  =
    isComplete(Seq(
      isAnswerComplete(PartnerDOBId(estIndex, dirIndex)),
      isAnswerComplete(PartnerHasNINOId(estIndex, dirIndex), PartnerNINOId(estIndex, dirIndex), Some(PartnerNoNINOReasonId(estIndex, dirIndex))),
      isAnswerComplete(PartnerHasUTRId(estIndex, dirIndex), PartnerEnterUTRId(estIndex, dirIndex), Some(PartnerNoUTRReasonId(estIndex, dirIndex)))
    ))

  def isPartnerAddressComplete(estIndex: Int, dirIndex: Int): Option[Boolean] =
    isAddressComplete(
      partnerAddress.AddressId(estIndex, dirIndex),
      partnerAddress.PreviousAddressId(estIndex, dirIndex),
      partnerAddress.AddressYearsId(estIndex, dirIndex),
      None)
}
