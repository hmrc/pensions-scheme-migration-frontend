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

import identifiers.trustees.TrusteeKindId
import identifiers.trustees.company.details._
import identifiers.trustees.company.{CompanyDetailsId, address => companyAddress, contacts => companyContact}
import identifiers.trustees.individual.contact.{EnterEmailId, EnterPhoneId}
import identifiers.trustees.individual.details._
import identifiers.trustees.individual.{TrusteeNameId, address => individualAddress}
import identifiers.trustees.partnership.address.{AddressId, AddressYearsId, PreviousAddressId, TradingTimeId}
import identifiers.trustees.partnership.{PartnershipDetailsId, contact => partnershipContact, details => partnershipDetails}
import models.trustees.TrusteeKind
import models.{CompanyDetails, PartnershipDetails, PersonName, ReferenceValue}
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.{OptionValues, TryValues}
import utils.{Data, Enumerable, UserAnswers}

import java.time.LocalDate

class DataCompletionTrusteesSpec
  extends AnyWordSpec
    with Matchers
    with OptionValues
    with TryValues
    with Enumerable.Implicits {

  "Trustee Individual completion status should be returned correctly" when {
    "isTrusteeIndividualComplete" must {
      "return true when all answers are present" in {
        val ua =
          UserAnswers()
            .set(TrusteeKindId(0), TrusteeKind.Individual).success.value
            .set(TrusteeNameId(0), PersonName("a", "b")).success.value
            .set(TrusteeDOBId(0), LocalDate.parse("2001-01-01")).success.value
            .set(TrusteeHasNINOId(0), true).success.value
            .set(TrusteeNINOId(0), ReferenceValue("AB123456C")).success.value
            .set(TrusteeHasUTRId(0), true).success.value
            .set(TrusteeUTRId(0), ReferenceValue("1234567890")).success.value
            .setOrException(individualAddress.AddressId(0), Data.address)
            .setOrException(individualAddress.AddressYearsId(0), true)
            .set(EnterEmailId(0), "test@test.com").success.value
            .set(EnterPhoneId(0), "123").success.value

        ua.isTrusteeIndividualComplete(0) mustBe true
      }

      "return false when some answer is missing" in {
        val ua =
          UserAnswers()
            .set(TrusteeKindId(0), TrusteeKind.Individual).success.value
            .set(TrusteeNameId(0), PersonName("a", "b")).success.value
            .set(TrusteeKindId(1), TrusteeKind.Individual).success.value

        ua.isTrusteeIndividualComplete(1) mustBe false
      }
    }

    "isTrusteeIndividualDetailsComplete" must {
      "return true when all answers are present" in {
        val ua =
          UserAnswers()
            .set(TrusteeDOBId(0), LocalDate.parse("2001-01-01")).success.value

        val ua1 =
          ua
            .set(TrusteeHasNINOId(0), true).success.value
            .set(TrusteeNINOId(0), ReferenceValue("AB123456C")).success.value
            .set(TrusteeHasUTRId(0), true).success.value
            .set(TrusteeUTRId(0), ReferenceValue("1234567890")).success.value

        val ua2 =
          ua
            .set(TrusteeHasNINOId(0), false).success.value
            .set(TrusteeNoNINOReasonId(0), "Reason").success.value
            .set(TrusteeHasUTRId(0), false).success.value
            .set(TrusteeNoUTRReasonId(0), "Reason").success.value

        ua1.isTrusteeIndividualDetailsComplete(0) mustBe Some(true)
        ua2.isTrusteeIndividualDetailsComplete(0) mustBe Some(true)
      }

      "return false when some answer is missing" in {
        val ua =
          UserAnswers()
            .set(TrusteeDOBId(0), LocalDate.parse("2001-01-01")).success.value

        ua.isTrusteeIndividualDetailsComplete(0) mustBe Some(false)

      }
    }

    "isTrusteeIndividualAddressComplete" must {
      "return true when all answers are present" in {
        val ua1 =
          UserAnswers()
            .setOrException(individualAddress.AddressId(0), Data.address)
            .setOrException(individualAddress.AddressYearsId(0), true)

        val ua2 =
          UserAnswers()
            .setOrException(individualAddress.AddressId(0), Data.address)
            .setOrException(individualAddress.AddressYearsId(0), false)
            .setOrException(individualAddress.PreviousAddressId(0), Data.address)

        ua1.isTrusteeIndividualAddressComplete(0) mustBe Some(true)
        ua2.isTrusteeIndividualAddressComplete(0) mustBe Some(true)
      }

      "return false when some answer is missing" in {
        val ua1 =
          UserAnswers()
            .setOrException(individualAddress.AddressId(0), Data.address)
            .setOrException(individualAddress.AddressYearsId(0), false)

        val ua2 =
          UserAnswers()
            .setOrException(individualAddress.AddressId(0), Data.address)

        ua1.isTrusteeIndividualAddressComplete(0) mustBe Some(false)
        ua2.isTrusteeIndividualAddressComplete(0) mustBe Some(false)
      }

      "return None when no answers present" in {
        UserAnswers().isTrusteeIndividualAddressComplete(0) mustBe None
      }
    }

    "isTrusteeIndividualContactDetailsComplete" must {
      "return true when all answers are present" in {
        val ua =
          UserAnswers()
            .set(EnterEmailId(0), "test@test.com").success.value
            .set(EnterPhoneId(0), "123").success.value

        ua.isTrusteeIndividualContactDetailsComplete(0).value mustBe true
        ua.isTrusteeIndividualContactDetailsComplete(0).value mustBe true
      }

      "return false when some answer is missing" in {
        val ua =
          UserAnswers()
            .set(EnterEmailId(0), "test@test.com").success.value

        ua.isTrusteeIndividualContactDetailsComplete(0).value mustBe false
      }

      "return None when no answer is present" in {
        UserAnswers().isTrusteeIndividualContactDetailsComplete(0) mustBe None
      }
    }
  }

  "Trustee Company completion status should be returned correctly" when {
    "isTrusteeCompanyComplete" must {
      "return true when all answers are present" in {
        val ua =
          UserAnswers()
            .set(TrusteeKindId(0), TrusteeKind.Company).success.value
            .set(CompanyDetailsId(0), CompanyDetails("test company")).success.value
            .set(HaveCompanyNumberId(0), true).success.value
            .set(CompanyNumberId(0), ReferenceValue("AB123456C")).success.value
            .set(HaveUTRId(0), true).success.value
            .set(CompanyUTRId(0), ReferenceValue("1234567890")).success.value
            .set(HaveVATId(0), true).success.value
            .set(VATId(0), ReferenceValue("123456789")).success.value
            .set(HavePAYEId(0), true).success.value
            .set(PAYEId(0), ReferenceValue("12345678")).success.value
            .set(companyAddress.AddressId(0), Data.address).success.value
            .set(companyAddress.AddressYearsId(0), true).success.value
            .set(companyContact.EnterEmailId(0), "test@test.com").success.value
            .set(companyContact.EnterPhoneId(0), "123").success.value

        ua.isTrusteeCompanyComplete(0) mustBe true
      }

      "return false when some answer is missing" in {
        val ua =
          UserAnswers()
            .set(TrusteeKindId(0), TrusteeKind.Company).success.value
            .set(CompanyDetailsId(0), CompanyDetails("test company")).success.value
            .set(TrusteeKindId(1), TrusteeKind.Company).success.value

        ua.isTrusteeCompanyComplete(1) mustBe false
      }
    }

    "isTrusteeCompanyDetailsComplete" must {
      "return true when all answers are present" in {

        val ua1 =
          UserAnswers()
            .set(HaveCompanyNumberId(0), true).success.value
            .set(CompanyNumberId(0), ReferenceValue("AB123456C")).success.value
            .set(HaveUTRId(0), true).success.value
            .set(CompanyUTRId(0), ReferenceValue("1234567890")).success.value
            .set(HaveVATId(0), true).success.value
            .set(VATId(0), ReferenceValue("123456789")).success.value
            .set(HavePAYEId(0), true).success.value
            .set(PAYEId(0), ReferenceValue("12345678")).success.value

        val ua2 =
          UserAnswers()
            .set(HaveCompanyNumberId(0), false).success.value
            .set(NoCompanyNumberReasonId(0), "Reason").success.value
            .set(HaveUTRId(0), false).success.value
            .set(NoUTRReasonId(0), "Reason").success.value
            .set(HaveVATId(0), false).success.value
            .set(HavePAYEId(0), false).success.value


        ua1.isTrusteeCompanyDetailsComplete(0) mustBe Some(true)
        ua2.isTrusteeCompanyDetailsComplete(0) mustBe Some(true)
      }

      "return false when some answer is missing" in {
        val ua =
          UserAnswers()
            .set(HaveCompanyNumberId(0), false).success.value
            .set(CompanyNumberId(0), ReferenceValue("AB123456C")).success.value
            .set(HaveUTRId(0), true).success.value
            .set(CompanyUTRId(0), ReferenceValue("1234567890")).success.value
            .set(HaveVATId(0), true).success.value
            .set(VATId(0), ReferenceValue("123456789")).success.value
            .set(HavePAYEId(0), true).success.value

        ua.isTrusteeCompanyDetailsComplete(0) mustBe Some(false)

      }
    }

    "isTrusteeCompanyAddressComplete" must {
      "return true when address is complete and address years is true" in {
        val ua =
          UserAnswers()
            .set(TrusteeKindId(0), TrusteeKind.Company).success.value
            .set(CompanyDetailsId(0), CompanyDetails("test company")).success.value
            .set(companyAddress.AddressId(0), Data.address).success.value
            .set(companyAddress.AddressYearsId(0), true).success.value

        ua.isTrusteeCompanyAddressComplete(0).value mustBe true
      }

      "return true when address is complete and address years is false and trading time is false" in {
        val ua =
          UserAnswers()
            .set(TrusteeKindId(0), TrusteeKind.Partnership).success.value
            .set(CompanyDetailsId(0), CompanyDetails("test company")).success.value
            .set(companyAddress.AddressId(0), Data.address).success.value
            .set(companyAddress.AddressYearsId(0), false).success.value
            .set(companyAddress.TradingTimeId(0), false).success.value

        ua.isTrusteeCompanyAddressComplete(0).value mustBe true
      }

      "return true when address is complete and previous address is complete" in {
        val ua =
          UserAnswers()
            .set(TrusteeKindId(0), TrusteeKind.Partnership).success.value
            .set(CompanyDetailsId(0), CompanyDetails("test company")).success.value
            .set(companyAddress.AddressId(0), Data.address).success.value
            .set(companyAddress.AddressYearsId(0), false).success.value
            .set(companyAddress.TradingTimeId(0), true).success.value
            .set(companyAddress.PreviousAddressId(0), Data.address).success.value

        ua.isTrusteeCompanyAddressComplete(0).value mustBe true
      }

      "return false when address is complete but no address years is present" in {
        val ua =
          UserAnswers()
            .set(TrusteeKindId(0), TrusteeKind.Company).success.value
            .set(CompanyDetailsId(0), CompanyDetails("test company")).success.value
            .set(companyAddress.AddressId(0), Data.address).success.value

        ua.isTrusteeCompanyAddressComplete(0).value mustBe false
      }

      "return false when address is complete but no previous address is present" in {
        val ua =
          UserAnswers()
            .set(TrusteeKindId(0), TrusteeKind.Company).success.value
            .set(CompanyDetailsId(0), CompanyDetails("test company")).success.value
            .set(companyAddress.AddressId(0), Data.address).success.value
            .set(companyAddress.AddressYearsId(0), false).success.value
            .set(companyAddress.TradingTimeId(0), true).success.value

        ua.isTrusteeCompanyAddressComplete(0).value mustBe false
      }
    }

    "isTrusteeCompanyContactDetailsComplete" must {
      "return true when all answers are present" in {
        val ua =
          UserAnswers()
            .set(companyContact.EnterEmailId(0), "test@test.com").success.value
            .set(companyContact.EnterPhoneId(0), "123").success.value

        ua.isTrusteeCompanyContactDetailsComplete(0).value mustBe true
      }

      "return false when some answer is missing" in {
        val ua =
          UserAnswers()
            .set(companyContact.EnterEmailId(0), "test@test.com").success.value

        ua.isTrusteeCompanyContactDetailsComplete(0).value mustBe false
      }

      "return None when no answer is present" in {
        UserAnswers().isTrusteeCompanyContactDetailsComplete(0) mustBe None
      }
    }
  }

  "Trustee Partnership completion status should be returned correctly" when {
    "isTrusteePartnershipComplete" must {
      "return true when all answers are present" in {
        val ua =
          UserAnswers()
            .set(TrusteeKindId(0), TrusteeKind.Partnership).success.value
            .set(PartnershipDetailsId(0), PartnershipDetails("test partnership")).success.value
            .set(partnershipDetails.HaveUTRId(0), false).success.value
            .set(partnershipDetails.NoUTRReasonId(0), "Reason").success.value
            .set(partnershipDetails.HaveVATId(0), false).success.value
            .set(partnershipDetails.HavePAYEId(0), false).success.value
            .set(AddressId(0), Data.address).success.value
            .set(AddressYearsId(0), true).success.value
            .set(partnershipContact.EnterEmailId(0), "test@test.com").success.value
            .set(partnershipContact.EnterPhoneId(0), "123").success.value

        ua.isTrusteePartnershipComplete(0) mustBe true
      }

      "return false when some answer is missing" in {
        val ua =
          UserAnswers()
            .set(TrusteeKindId(0), TrusteeKind.Partnership).success.value
            .set(PartnershipDetailsId(0), PartnershipDetails("test partnership")).success.value
            .set(TrusteeKindId(1), TrusteeKind.Partnership).success.value

        ua.isTrusteePartnershipComplete(1) mustBe false
      }
    }

    "isTrusteePartnershipAddressComplete" must {
      "return true when address is complete and address years is true" in {
        val ua =
          UserAnswers()
            .set(TrusteeKindId(0), TrusteeKind.Partnership).success.value
            .set(PartnershipDetailsId(0), PartnershipDetails("test partnership")).success.value
            .set(AddressId(0), Data.address).success.value
            .set(AddressYearsId(0), true).success.value

        ua.isTrusteePartnershipAddressComplete(0).value mustBe true
      }

      "return true when address is complete and address years is false and trading time is false" in {
        val ua =
          UserAnswers()
            .set(TrusteeKindId(0), TrusteeKind.Partnership).success.value
            .set(PartnershipDetailsId(0), PartnershipDetails("test partnership")).success.value
            .set(AddressId(0), Data.address).success.value
            .set(AddressYearsId(0), false).success.value
            .set(TradingTimeId(0), false).success.value

        ua.isTrusteePartnershipAddressComplete(0).value mustBe true
      }

      "return true when address is complete and previous address is complete" in {
        val ua =
          UserAnswers()
            .set(TrusteeKindId(0), TrusteeKind.Partnership).success.value
            .set(PartnershipDetailsId(0), PartnershipDetails("test partnership")).success.value
            .set(AddressId(0), Data.address).success.value
            .set(AddressYearsId(0), false).success.value
            .set(TradingTimeId(0), true).success.value
            .set(PreviousAddressId(0), Data.address).success.value

        ua.isTrusteePartnershipAddressComplete(0).value mustBe true
      }

      "return false when address is complete but no address years is present" in {
        val ua =
          UserAnswers()
            .set(TrusteeKindId(0), TrusteeKind.Partnership).success.value
            .set(PartnershipDetailsId(0), PartnershipDetails("test partnership")).success.value
            .set(AddressId(0), Data.address).success.value

        ua.isTrusteePartnershipAddressComplete(0).value mustBe false
      }

      "return false when address is complete but no previous address is present" in {
        val ua =
          UserAnswers()
            .set(TrusteeKindId(0), TrusteeKind.Partnership).success.value
            .set(PartnershipDetailsId(0), PartnershipDetails("test partnership")).success.value
            .set(AddressId(0), Data.address).success.value
            .set(AddressYearsId(0), false).success.value
            .set(TradingTimeId(0), true).success.value

        ua.isTrusteePartnershipAddressComplete(0).value mustBe false
      }
    }

    "isTrusteePartnershipContactDetailsComplete" must {
      "return true when all answers are present" in {
        val ua =
          UserAnswers()
            .set(partnershipContact.EnterEmailId(0), "test@test.com").success.value
            .set(partnershipContact.EnterPhoneId(0), "123").success.value

        ua.isTrusteePartnershipContactDetailsComplete(0).value mustBe true
      }

      "return false when some answer is missing" in {
        val ua =
          UserAnswers()
            .set(partnershipContact.EnterEmailId(0), "test@test.com").success.value

        ua.isTrusteePartnershipContactDetailsComplete(0).value mustBe false
      }

      "return None when no answer is present" in {
        UserAnswers().isTrusteePartnershipContactDetailsComplete(0) mustBe None
      }
    }
  }
}
