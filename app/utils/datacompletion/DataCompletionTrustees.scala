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

import identifiers.trustees.company.address.{TradingTimeId, AddressId => CompanyAddressId, AddressYearsId => CompanyAddressYearsId, PreviousAddressId => CompanyPreviousAddressId}
import identifiers.trustees.company.details._
import identifiers.trustees.company.{contacts => companyContact}
import identifiers.trustees.individual.address.{AddressId, AddressYearsId, PreviousAddressId}
import identifiers.trustees.individual.contact.{EnterEmailId, EnterPhoneId}
import identifiers.trustees.individual.details._
import identifiers.trustees.partnership.address.{AddressId => PartnershipAddressId, AddressYearsId => PartnershipAddressYearsId, PreviousAddressId => PartnershipPreviousAddressId, TradingTimeId => PartnershipTradingTimeId}
import identifiers.trustees.partnership.{contact => partnershipContact, details => partnershipDetails}
import models.Index
import utils.UserAnswers

trait DataCompletionTrustees extends DataCompletion {

  self: UserAnswers =>

  def isTrusteeIndividualComplete(index: Int): Boolean =
    isComplete(
      Seq(
        isTrusteeIndividualDetailsComplete(index),
        isTrusteeIndividualAddressComplete(index),
        isTrusteeIndividualContactDetailsComplete(index)
      )
    ).getOrElse(false)

  def isTrusteeIndividualDetailsComplete(index: Int): Option[Boolean] =
    isComplete(
      Seq(
        isAnswerComplete(TrusteeDOBId(index)),
        isAnswerComplete(TrusteeHasNINOId(index), TrusteeNINOId(index), Some(TrusteeNoNINOReasonId(index))),
        isAnswerComplete(TrusteeHasUTRId(index), TrusteeUTRId(index), Some(TrusteeNoUTRReasonId(index)))
      )
    )

  def isTrusteeIndividualAddressComplete(index: Int): Option[Boolean] =
  isAddressComplete(AddressId(index), PreviousAddressId(index), AddressYearsId(index), None)

  def isTrusteeIndividualContactDetailsComplete(index: Int): Option[Boolean] =
    isContactDetailsComplete(EnterEmailId(index), EnterPhoneId(index))

  def isTrusteeCompanyComplete(index: Int): Boolean =
    isComplete(Seq(
      isTrusteeCompanyDetailsComplete(index),
      isTrusteeCompanyAddressComplete(index),
      isTrusteeCompanyContactDetailsComplete(index))).getOrElse(false)


  def isTrusteeCompanyDetailsComplete(index: Int): Option[Boolean] =
    isComplete(
      Seq(
        isAnswerComplete(HaveCompanyNumberId(index), CompanyNumberId(index), Some(NoCompanyNumberReasonId(index))),
        isAnswerComplete(HaveUTRId(index), CompanyUTRId(index), Some(NoUTRReasonId(index))),
        isAnswerComplete(HaveVATId(index), VATId(index), None),
        isAnswerComplete(HavePAYEId(index), PAYEId(index), None)
      )
    )

  def isTrusteeCompanyAddressComplete(index: Int): Option[Boolean] =
    isAddressComplete(CompanyAddressId(index), CompanyPreviousAddressId(index), CompanyAddressYearsId(index), Some(TradingTimeId(index)))

  def isTrusteeCompanyContactDetailsComplete(index: Int): Option[Boolean] =
    isContactDetailsComplete(companyContact.EnterEmailId(index), companyContact.EnterPhoneId(index))

  def isTrusteePartnershipComplete(index: Int): Boolean =
    isComplete(Seq(
      isTrusteePartnershipDetailsComplete(index),
      isTrusteePartnershipAddressComplete(index),
      isTrusteePartnershipContactDetailsComplete(index))).getOrElse(false)

  def isTrusteePartnershipDetailsComplete(index: Int): Option[Boolean] =
    isComplete(
      Seq(
        isAnswerComplete(partnershipDetails.HaveUTRId(index), partnershipDetails.PartnershipUTRId(index), Some(partnershipDetails.NoUTRReasonId(index))),
        isAnswerComplete(partnershipDetails.HaveVATId(index),partnershipDetails.VATId(index),None),
        isAnswerComplete(partnershipDetails.HavePAYEId(index),partnershipDetails.PAYEId(index),None)
      )
    )

  def isTrusteePartnershipAddressComplete(index: Index): Option[Boolean] =
  isAddressComplete(PartnershipAddressId(index),
    PartnershipPreviousAddressId(index),
    PartnershipAddressYearsId(index),
    Some(PartnershipTradingTimeId(index)))

  def isTrusteePartnershipContactDetailsComplete(index: Int): Option[Boolean] =
    isContactDetailsComplete(partnershipContact.EnterEmailId(index), partnershipContact.EnterPhoneId(index))
}
