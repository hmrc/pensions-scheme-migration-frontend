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

import identifiers.establishers.EstablisherKindId
import identifiers.establishers.company.details._
import identifiers.establishers.company.director.details._
import identifiers.establishers.company.director.{address => directorAddress}
import identifiers.establishers.company.{CompanyDetailsId, contact => companyContact}
import identifiers.establishers.individual.address.{AddressId, AddressYearsId, PreviousAddressId}
import identifiers.establishers.individual.contact.{EnterEmailId, EnterPhoneId}
import identifiers.establishers.individual.details._
import identifiers.establishers.individual.EstablisherNameId
import identifiers.establishers.company.director.{contact => directorContact}
import identifiers.establishers.partnership.{PartnershipDetailsId, address => partnershipAddress}
import identifiers.establishers.partnership.{details => partnershipDetails}
import models.establishers.EstablisherKind
import models.{CompanyDetails, PartnershipDetails, PersonName, ReferenceValue}
import org.scalatest.{OptionValues, TryValues}
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec

import utils.{Data, Enumerable, UserAnswers}

import java.time.LocalDate

class DataCompletionEstablishersSpec
  extends AnyWordSpec
    with Matchers
    with OptionValues
    with TryValues
    with Enumerable.Implicits {

  "Establisher Individual completion status should be returned correctly" when {
    "isEstablisherIndividualComplete" must {
      "return true when all answers are present" in {
        val ua =
          UserAnswers()
            .set(EstablisherKindId(0), EstablisherKind.Individual).success.value
            .set(EstablisherNameId(0), PersonName("a", "b")).success.value

        ua.isEstablisherIndividualComplete(0) mustBe true
      }

      "return false when some answer is missing" in {
        val ua =
          UserAnswers()
            .set(EstablisherKindId(0), EstablisherKind.Individual).success.value
            .set(EstablisherNameId(0), PersonName("a", "b")).success.value
            .set(EstablisherKindId(1), EstablisherKind.Individual).success.value

        ua.isEstablisherIndividualComplete(1) mustBe false
      }
    }

    "isEstablisherIndividualDetailsCompleted" must {
      "return true when all answers are present" in {
        val ua =
          UserAnswers()
            .set(EstablisherDOBId(0), LocalDate.parse("2001-01-01")).success.value

        val ua1 =
          ua
            .set(EstablisherHasNINOId(0), true).success.value
            .set(EstablisherNINOId(0), ReferenceValue("AB123456C")).success.value
            .set(EstablisherHasUTRId(0), true).success.value
            .set(EstablisherUTRId(0), ReferenceValue("1234567890")).success.value

        val ua2 =
          ua
            .set(EstablisherHasNINOId(0), false).success.value
            .set(EstablisherNoNINOReasonId(0), "Reason").success.value
            .set(EstablisherHasUTRId(0), false).success.value
            .set(EstablisherNoUTRReasonId(0), "Reason").success.value

        ua1.isEstablisherIndividualDetailsCompleted(0) mustBe Some(true)
        ua2.isEstablisherIndividualDetailsCompleted(0) mustBe Some(true)
      }

      "return false when some answer is missing" in {
        val ua =
          UserAnswers()
            .set(EstablisherDOBId(0), LocalDate.parse("2001-01-01")).success.value

        ua.isEstablisherIndividualDetailsCompleted(0) mustBe Some(false)

      }
    }

    "isEstablisherIndividualAddressCompleted" must {
      "return true when all answers are present" in {
        val ua1 =
          UserAnswers()
            .setOrException(AddressId(0), Data.address)
            .setOrException(AddressYearsId(0), true)

        val ua2 =
          UserAnswers()
            .setOrException(AddressId(0), Data.address)
            .setOrException(AddressYearsId(0), false)
            .setOrException(PreviousAddressId(0), Data.address)

        ua1.isEstablisherIndividualAddressCompleted(0, ua1) mustBe Some(true)
        ua2.isEstablisherIndividualAddressCompleted(0, ua2) mustBe Some(true)
      }

      "return false when some answer is missing" in {
        val ua1 =
          UserAnswers()
            .setOrException(AddressId(0), Data.address)
            .setOrException(AddressYearsId(0), false)

        val ua2 =
          UserAnswers()
            .setOrException(AddressId(0), Data.address)

        ua1.isEstablisherIndividualAddressCompleted(0, ua1) mustBe Some(false)
        ua2.isEstablisherIndividualAddressCompleted(0, ua2) mustBe Some(false)
      }

      "return None when no answers present" in {
        UserAnswers().isEstablisherIndividualAddressCompleted(0, UserAnswers()) mustBe None
      }
    }

    "isEstablisherCompanyDetailsCompleted" must {
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


        ua1.isEstablisherCompanyDetailsCompleted(0) mustBe Some(true)
        ua2.isEstablisherCompanyDetailsCompleted(0) mustBe Some(true)
      }

      "return false when some answer is missing" in {
        val ua =
          UserAnswers()
            .set(HaveCompanyNumberId(0), false).success.value

        ua.isEstablisherCompanyDetailsCompleted(0) mustBe Some(false)

      }
    }

    "isEstablisherIndividualContactDetailsCompleted" must {
      "return true when all answers are present" in {
        val ua =
          UserAnswers()
            .set(EnterEmailId(0), "test@test.com").success.value
            .set(EnterPhoneId(0), "123").success.value

        ua.isEstablisherIndividualContactDetailsCompleted(0).value mustBe true
      }

      "return false when some answer is missing" in {
        val ua =
          UserAnswers()
            .set(EnterEmailId(0), "test@test.com").success.value

        ua.isEstablisherIndividualContactDetailsCompleted(0).value mustBe false
      }

      "return None when no answer is present" in {
        UserAnswers().isEstablisherIndividualContactDetailsCompleted(0) mustBe None
      }
    }
  }

  "Establisher Company completion status should be returned correctly" when {
    "isEstablisherCompanyComplete" must {
      "return true when all answers are present" in {
        val ua =
          UserAnswers()
            .set(EstablisherKindId(0), EstablisherKind.Company).success.value
            .set(CompanyDetailsId(0), CompanyDetails("test company")).success.value

        ua.isEstablisherCompanyComplete(0) mustBe true
      }

      "return false when some answer is missing" in {
        val ua =
          UserAnswers()
            .set(EstablisherKindId(0), EstablisherKind.Company).success.value
            .set(CompanyDetailsId(0), CompanyDetails("test company")).success.value
            .set(EstablisherKindId(1), EstablisherKind.Company).success.value

        ua.isEstablisherCompanyComplete(1) mustBe false
      }
    }
    "isEstablisherCompanyContactDetailsCompleted" must {
      "return true when all answers are present" in {
        val ua =
          UserAnswers()
            .set(companyContact.EnterEmailId(0), "test@test.com").success.value
            .set(companyContact.EnterPhoneId(0), "123").success.value

        ua.isEstablisherCompanyContactDetailsCompleted(0).value mustBe true
      }

      "return false when some answer is missing" in {
        val ua =
          UserAnswers()
            .set(companyContact.EnterEmailId(0), "test@test.com").success.value

        ua.isEstablisherCompanyContactDetailsCompleted(0).value mustBe false
      }

      "return None when no answer is present" in {
        UserAnswers().isEstablisherCompanyContactDetailsCompleted(0) mustBe None
      }
    }

    "Director completion status should be returned correctly" when {
      "isDirectorComplete" must {
        "return true when all answers are present" in {
          val ua1 =
            UserAnswers()
              .set(DirectorDOBId(0, 0), LocalDate.parse("2001-01-01")).success.value
              .set(DirectorHasNINOId(0, 0), true).success.value
              .set(DirectorNINOId(0, 0), ReferenceValue("1234567890")).success.value
              .set(DirectorHasUTRId(0, 0), true).success.value
              .set(DirectorEnterUTRId(0, 0), ReferenceValue("123456789")).success.value
              .setOrException(directorAddress.AddressId(0, 0), Data.address)
              .setOrException(directorAddress.AddressYearsId(0, 0), true)
              .set(directorContact.EnterEmailId(0, 0), "test@test.com").success.value
              .set(directorContact.EnterPhoneId(0, 0), "123").success.value

          val ua2 =
            UserAnswers()
              .set(DirectorDOBId(0, 0), LocalDate.parse("2001-01-01")).success.value
              .set(DirectorHasNINOId(0, 0), false).success.value
              .set(DirectorNoNINOReasonId(0, 0), "Reason").success.value
              .set(DirectorHasUTRId(0, 0), false).success.value
              .set(DirectorNoUTRReasonId(0, 0), "Reason").success.value
              .setOrException(directorAddress.AddressId(0, 0), Data.address)
              .setOrException(directorAddress.AddressYearsId(0, 0), false)
              .setOrException(directorAddress.PreviousAddressId(0, 0), Data.address)
              .set(directorContact.EnterEmailId(0, 0), "test@test.com").success.value
              .set(directorContact.EnterPhoneId(0, 0), "123").success.value

          ua1.isDirectorComplete(0, 0) mustBe true
          ua2.isDirectorComplete(0, 0) mustBe true
        }

        "return false when some answer is missing" in {
          val ua =
            UserAnswers()
              .set(DirectorDOBId(0, 0), LocalDate.parse("2001-01-01")).success.value
              .set(DirectorHasNINOId(0, 0), false).success.value
              .set(DirectorHasUTRId(0, 0), false).success.value
              .setOrException(directorAddress.AddressId(0, 0), Data.address)
              .setOrException(directorAddress.AddressYearsId(0, 0), false)

          ua.isDirectorComplete(0,0) mustBe false
        }
      }
    }
  }

    "Director Details completion status should be returned correctly" when {
      "isDirectorDetailsComplete" must {
        "return true when all answers are present" in {
          val ua1 =
            UserAnswers()
              .set(DirectorDOBId(0, 0), LocalDate.parse("2001-01-01")).success.value
              .set(DirectorHasNINOId(0, 0), true).success.value
              .set(DirectorNINOId(0, 0), ReferenceValue("1234567890")).success.value
              .set(DirectorHasUTRId(0, 0), true).success.value
              .set(DirectorEnterUTRId(0, 0), ReferenceValue("123456789")).success.value

          val ua2 =
            UserAnswers()
              .set(DirectorDOBId(0, 0), LocalDate.parse("2001-01-01")).success.value
              .set(DirectorHasNINOId(0, 0), false).success.value
              .set(DirectorNoNINOReasonId(0, 0), "Reason").success.value
              .set(DirectorHasUTRId(0, 0), false).success.value
              .set(DirectorNoUTRReasonId(0, 0), "Reason").success.value

          ua1.isDirectorDetailsComplete(0, 0) mustBe Some(true)
          ua2.isDirectorDetailsComplete(0, 0) mustBe Some(true)
        }

        "return false when some answer is missing" in {
          val ua =
            UserAnswers()
              .set(DirectorDOBId(0, 0), LocalDate.parse("2001-01-01")).success.value
              .set(DirectorHasNINOId(0, 0), false).success.value
              .set(DirectorHasUTRId(0, 0), false).success.value

          ua.isDirectorDetailsComplete(0,0) mustBe Some(false)
        }
      }
    }

  "Director Address completion status should be returned correctly" when {

    "isDirectorAddressComplete" must {
      "return true when all answers are present" in {
        val ua1 =
          UserAnswers()
            .setOrException(directorAddress.AddressId(0, 0), Data.address)
            .setOrException(directorAddress.AddressYearsId(0, 0), true)

        val ua2 =
          UserAnswers()
            .setOrException(directorAddress.AddressId(0, 0), Data.address)
            .setOrException(directorAddress.AddressYearsId(0, 0), false)
            .setOrException(directorAddress.PreviousAddressId(0, 0), Data.address)

        ua1.isDirectorAddressComplete(0, 0) mustBe Some(true)
        ua2.isDirectorAddressComplete(0, 0) mustBe Some(true)
      }

      "return false when some answer is missing" in {
        val ua =
          UserAnswers()
            .setOrException(directorAddress.AddressId(0, 0), Data.address)
            .setOrException(directorAddress.AddressYearsId(0, 0), false)

        ua.isDirectorAddressComplete(0, 0) mustBe Some(false)
      }
    }

    "Establisher Partnership completion status should be returned correctly" when {
      "isEstablisherPartnershipComplete" must {
        "return true when all answers are present" in {
          val ua =
            UserAnswers()
              .set(EstablisherKindId(0), EstablisherKind.Partnership).success.value
              .set(PartnershipDetailsId(0), PartnershipDetails("test partnership")).success.value

          ua.isEstablisherPartnershipComplete(0) mustBe true
        }

        "return false when some answer is missing" in {
          val ua =
            UserAnswers()
              .set(EstablisherKindId(0), EstablisherKind.Partnership).success.value
              .set(PartnershipDetailsId(0), PartnershipDetails("test partnership")).success.value
              .set(EstablisherKindId(1), EstablisherKind.Partnership).success.value

          ua.isEstablisherPartnershipComplete(1) mustBe false
        }
      }

      "isEstablisherPartnershipDetailsCompleted" must {
        "return true when all answers are present" in {

          val ua1 =
            UserAnswers()
              .set(partnershipDetails.HaveUTRId(0), true).success.value
              .set(partnershipDetails.PartnershipUTRId(0), ReferenceValue("1234567890")).success.value
              .set(partnershipDetails.HaveVATId(0), true).success.value
              .set(partnershipDetails.VATId(0), ReferenceValue("123456789")).success.value
              .set(partnershipDetails.HavePAYEId(0), true).success.value
              .set(partnershipDetails.PAYEId(0), ReferenceValue("12345678")).success.value

          val ua2 =
            UserAnswers()
              .set(partnershipDetails.HaveUTRId(0), false).success.value
              .set(partnershipDetails.NoUTRReasonId(0), "Reason").success.value
              .set(partnershipDetails.HaveVATId(0), false).success.value
              .set(partnershipDetails.HavePAYEId(0), false).success.value


          ua1.isEstablisherPartnershipDetailsCompleted(0) mustBe Some(true)
          ua2.isEstablisherPartnershipDetailsCompleted(0) mustBe Some(true)
        }

        "return false when some answer is missing" in {
          val ua =
            UserAnswers()
              .set(partnershipDetails.HaveUTRId(0), false).success.value

          ua.isEstablisherPartnershipDetailsCompleted(0) mustBe Some(false)

        }
      }

      "isEstablisherPartnershipAddressCompleted" must {
        "return true when all answers are present" in {
          val ua1 =
            UserAnswers()
              .setOrException(partnershipAddress.AddressId(0), Data.address)
              .setOrException(partnershipAddress.AddressYearsId(0), true)

          val ua2 =
            UserAnswers()
              .setOrException(partnershipAddress.AddressId(0), Data.address)
              .setOrException(partnershipAddress.AddressYearsId(0), false)
              .setOrException(partnershipAddress.TradingTimeId(0), true)
              .setOrException(partnershipAddress.PreviousAddressId(0), Data.address)

          ua1.isEstablisherPartnershipAddressCompleted(0, ua1) mustBe Some(true)
          ua2.isEstablisherPartnershipAddressCompleted(0, ua2) mustBe Some(true)
        }

        "return false when some answer is missing" in {
          val ua1 =
            UserAnswers()
              .setOrException(partnershipAddress.AddressId(0), Data.address)
              .setOrException(partnershipAddress.AddressYearsId(0), false)

          val ua2 =
            UserAnswers()
              .setOrException(partnershipAddress.AddressId(0), Data.address)

          ua1.isEstablisherPartnershipAddressCompleted(0, ua1) mustBe Some(false)
          ua2.isEstablisherPartnershipAddressCompleted(0, ua2) mustBe Some(false)
        }

        "return None when no answers present" in {
          UserAnswers().isEstablisherPartnershipAddressCompleted(0, UserAnswers()) mustBe None
        }
      }
    }
  }
}
