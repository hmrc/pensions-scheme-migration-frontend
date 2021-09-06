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

package utils.datacompletion

import identifiers.trustees.TrusteeKindId
import identifiers.trustees.company.CompanyDetailsId
import identifiers.trustees.company.details._
import identifiers.trustees.individual.TrusteeNameId
import identifiers.trustees.partnership.address.{AddressYearsId => PartnershipAddressYearsId,
  TradingTimeId => PartnershipTradingTimeId, PreviousAddressId => PartnershipPreviousAddressId, AddressId => PartnershipAddressId}
import identifiers.trustees.individual.address.{AddressId, AddressYearsId, PreviousAddressId}
import identifiers.trustees.individual.contact.{EnterEmailId, EnterPhoneId}
import identifiers.trustees.company.{contacts => companyContact}
import identifiers.trustees.partnership.{contact => partnershipContact}
import identifiers.trustees.individual.details._
import utils.UserAnswers
import identifiers.trustees.company.address.{TradingTimeId, AddressId => CompanyAddressId, AddressYearsId => CompanyAddressYearsId, PreviousAddressId => CompanyPreviousAddressId}
import identifiers.trustees.partnership.PartnershipDetailsId

trait DataCompletionTrustees extends DataCompletion {

  self: UserAnswers =>

  def isTrusteeIndividualComplete(index: Int): Boolean =
    isComplete(
      Seq(
        isAnswerComplete(TrusteeNameId(index)),
        isAnswerComplete(TrusteeKindId(index))
      )
    ).getOrElse(false)

  def isTrusteeIndividualDetailsCompleted(index: Int): Option[Boolean] =
    isComplete(
      Seq(
        isAnswerComplete(TrusteeDOBId(index)),
        isAnswerComplete(TrusteeHasNINOId(index), TrusteeNINOId(index), Some(TrusteeNoNINOReasonId(index))),
        isAnswerComplete(TrusteeHasUTRId(index), TrusteeUTRId(index), Some(TrusteeNoUTRReasonId(index)))
      )
    )

  def isTrusteeIndividualAddressCompleted(
                                           index: Int,
                                           userAnswers: UserAnswers
                                         ): Option[Boolean] = {
    val atAddressMoreThanOneYear = userAnswers.get(AddressYearsId(index)).contains(true)
    isComplete(
      Seq(
        isAnswerComplete(AddressId(index)),
        isAnswerComplete(AddressYearsId(index)),
        if (atAddressMoreThanOneYear) Some(true) else isAnswerComplete(PreviousAddressId(index))
      )
    )
  }

  def isTrusteeIndividualContactDetailsCompleted(index: Int): Option[Boolean] =
    isComplete(
      Seq(
        isAnswerComplete(EnterEmailId(index)),
        isAnswerComplete(EnterPhoneId(index))
      )
    )

  def isTrusteeCompanyComplete(index: Int): Boolean =
    isComplete(
      Seq(
        isAnswerComplete(CompanyDetailsId(index)),
        isAnswerComplete(TrusteeKindId(index))
      )
    ).getOrElse(false)


  def isTrusteeCompanyDetailsCompleted(index: Int): Option[Boolean] = {
    isComplete(
      Seq(
        isAnswerComplete(HaveCompanyNumberId(index), CompanyNumberId(index), Some(NoCompanyNumberReasonId(index))),
        isAnswerComplete(HaveUTRId(index), CompanyUTRId(index), Some(NoUTRReasonId(index))),
        isAnswerComplete(HaveVATId(index), VATId(index), None),
        isAnswerComplete(HavePAYEId(index), PAYEId(index), None)
      )
    )
  }

  def isTrusteeCompanyAddressCompleted(
    index: Int,
    userAnswers: UserAnswers
  ): Option[Boolean] = {

    val previousAddress = (userAnswers.get(CompanyAddressYearsId(index)), userAnswers.get(TradingTimeId(index))) match {
      case (Some(true), _) => Some(true)
      case (Some(false), Some(true)) => isAnswerComplete(CompanyPreviousAddressId(index))
      case (Some(false), Some(false)) => Some(true)
      case _ => None
    }

    isComplete(
      Seq(
        isAnswerComplete(CompanyAddressId(index)),
        isAnswerComplete(CompanyAddressYearsId(index)),
        previousAddress
      )
    )
  }

  def isTrusteeCompanyContactDetailsCompleted(index: Int): Option[Boolean] =
    isComplete(
      Seq(
        isAnswerComplete(companyContact.EnterEmailId(index)),
        isAnswerComplete(companyContact.EnterPhoneId(index))
      )
    )

  def isTrusteePartnershipComplete(index: Int): Boolean =
    isComplete(
      Seq(
        isAnswerComplete(PartnershipDetailsId(index)),
        isAnswerComplete(TrusteeKindId(index))
      )
    ).getOrElse(false)

  def isTrusteePartnershipContactDetailsCompleted(index: Int): Option[Boolean] =
    isComplete(
      Seq(
        isAnswerComplete(partnershipContact.EnterEmailId(index)),
        isAnswerComplete(partnershipContact.EnterPhoneId(index))
      )
    )

  def isTrusteePartnershipAddressCompleted(
                                        index: Int,
                                        userAnswers: UserAnswers
                                      ): Option[Boolean] = {

    val previousAddress = (userAnswers.get(PartnershipAddressYearsId(index)),
      userAnswers.get(PartnershipTradingTimeId(index))) match {
      case (Some(true), _) => Some(true)
      case (Some(false), Some(true)) => isAnswerComplete(PartnershipPreviousAddressId(index))
      case (Some(false), Some(false)) => Some(true)
      case _ => None
    }

    isComplete(
      Seq(
        isAnswerComplete(PartnershipAddressId(index)),
        isAnswerComplete(PartnershipAddressYearsId(index)),
        previousAddress
      )
    )
  }
}
