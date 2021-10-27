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

import base.SpecBase
import helpers.routes.EstablishersIndividualRoutes.contactRoute
import helpers.routes.{EstablishersIndividualRoutes, TrusteesIndividualRoutes}
import identifiers.adviser.{AdviserNameId, EnterEmailId => adviserEnterEmailId, EnterPhoneId => adviserEnterPhoneId, AddressId => adviserAddressId}
import identifiers.beforeYouStart.{SchemeTypeId, EstablishedCountryId, WorkingKnowledgeId}
import identifiers.establishers.EstablisherKindId
import identifiers.establishers.company.CompanyDetailsId
import identifiers.establishers.company.contact.EnterPhoneId
import identifiers.establishers.company.details.{CompanyNumberId, HaveCompanyNumberId}
import identifiers.establishers.company.director.DirectorNameId
import identifiers.establishers.individual.EstablisherNameId
import identifiers.establishers.individual.address.{AddressId => IndividualAddressId}
import identifiers.establishers.partnership.address.{AddressId => PartnershipAddressId}
import identifiers.establishers.partnership.contact.EnterEmailId
import identifiers.establishers.partnership.details.{PartnershipUTRId, HaveUTRId}
import identifiers.establishers.partnership.partner.PartnerNameId
import identifiers.establishers.partnership.{PartnershipDetailsId => EstablisherPartnershipDetailsId}
import identifiers.trustees.company.{details => trusteeCompanyDetails}
import identifiers.trustees.individual.TrusteeNameId
import identifiers.trustees.individual.contact.{EnterEmailId => TrusteeEmailId, EnterPhoneId => TrusteePhoneId}
import identifiers.trustees.individual.details.{TrusteeDOBId, TrusteeNINOId, TrusteeUTRId}
import identifiers.trustees.partnership.address.{AddressYearsId => TrusteePartnershipAddressYearsId, AddressId => TrusteePartnershipAddressId}
import identifiers.trustees.partnership.contact.{EnterEmailId => TrusteePartnershipEmailId, EnterPhoneId => TrusteePartnershipPhoneId}
import identifiers.trustees.partnership.details.{HavePAYEId => TrusteeHavePAYEId, PAYEId => TrusteePAYEId, HaveVATId => TrusteeHaveVATId, VATId => TrusteeVATId, HaveUTRId => TrusteeHaveUTRId}
import identifiers.trustees.partnership.{PartnershipDetailsId => TrusteePartnershipDetailsId}
import identifiers.trustees.{TrusteeKindId, company => trusteeCompany}
import models._
import models.establishers.EstablisherKind
import models.trustees.TrusteeKind
import org.scalatest.matchers.must.Matchers
import org.scalatest.{OptionValues, TryValues}
import utils.Data.{schemeName, ua}
import utils.{Enumerable, Data}
import viewmodels.Message

import java.time.LocalDate

class SpokeCreationServiceSpec
  extends SpecBase
    with Matchers
    with OptionValues
    with TryValues
    with Enumerable.Implicits {

  val spokeCreationService = new SpokeCreationService()

  "getBeforeYouStartSpoke" must {
    "display the spoke with link to cya page with complete status if the spoke is completed" in {
      val userAnswers = ua.set(SchemeTypeId, SchemeType.SingleTrust).get.set(WorkingKnowledgeId, true).get
        .set(EstablishedCountryId, "GB").get

      val expectedSpoke = Seq(EntitySpoke(TaskListLink(Message("messages__schemeTaskList__before_you_start_link_text", schemeName),
        controllers.beforeYouStartSpoke.routes.CheckYourAnswersController.onPageLoad().url), Some(true)))

      val result = spokeCreationService.getBeforeYouStartSpoke(userAnswers, schemeName)
      result mustBe expectedSpoke
    }
  }

  "getAboutSpoke" when {
    "in subscription" must {
      "display all the spokes with link to first page, blank status if the spoke is uninitiated" in {

        val expectedSpoke = Seq(
          EntitySpoke(TaskListLink(Message("messages__schemeTaskList__about_members_link_text", schemeName),
            controllers.aboutMembership.routes.CheckYourAnswersController.onPageLoad().url), None),
          EntitySpoke(TaskListLink(
            messages("messages__schemeTaskList__about_benefits_and_insurance_link_text", schemeName),
            controllers.benefitsAndInsurance.routes.CheckYourAnswersController.onPageLoad.url
          ), None)
        )

        val result = spokeCreationService.aboutSpokes(ua, schemeName)
        result mustBe expectedSpoke
      }

    }
  }

  "workingKnowledgeSpoke" must {
    "display the spoke with link to wyn page with in complete status if the spoke is in progress" in {
      val expectedSpoke =
        Seq(EntitySpoke(
          TaskListLink(Message("messages__schemeTaskList__add_details_wk"),
            controllers.adviser.routes.WhatYouWillNeedController.onPageLoad.url, None),
          isCompleted = None
        ))
      val result = spokeCreationService.workingKnowledgeSpoke(ua)

      result mustBe expectedSpoke
    }
    "display the spoke with link to cya page with complete status if the spoke is in progress" in {
      val userAnswers =
        ua
          .set(AdviserNameId, "test").success.value
          .set(adviserEnterEmailId, Data.email).success.value
          .set(adviserEnterPhoneId, Data.phone).success.value
          .set(adviserAddressId, Data.address).success.value

      val expectedSpoke =
        Seq(EntitySpoke(
          TaskListLink(Message("messages__schemeTaskList__details_changeLink","test"),
            controllers.adviser.routes.CheckYourAnswersController.onPageLoad.url,None),
          isCompleted = Some(true)
        ))
      val result = spokeCreationService.workingKnowledgeSpoke(userAnswers)

      result mustBe expectedSpoke
    }

  }

  "getAddEstablisherHeaderSpokes" must {
    "return no spokes when no establishers and view only" in {
      val result = spokeCreationService.getAddEstablisherHeaderSpokes(ua, viewOnly = true)
      result mustBe Nil
    }

    "return all the spokes with appropriate links when no establishers and NOT view only" in {
      val expectedSpoke =
        Seq(EntitySpoke(
          TaskListLink(Message("messages__schemeTaskList__sectionEstablishers_add_link"),
            controllers.establishers.routes.EstablisherKindController.onPageLoad(0).url), None)
        )

      val result = spokeCreationService.getAddEstablisherHeaderSpokes(ua, viewOnly = false)
      result mustBe expectedSpoke
    }

    "return all the spokes with appropriate links when establishers and NOT view only" in {
      val userAnswers = ua.set(EstablisherKindId(0), EstablisherKind.Individual).flatMap(
        _.set(EstablisherNameId(0), PersonName("a", "b"))).get

      val expectedSpoke =
        Seq(EntitySpoke(
          TaskListLink(Message("messages__schemeTaskList__sectionEstablishers_change_link"),
            controllers.establishers.routes.AddEstablisherController.onPageLoad().url), None)
        )

      val result = spokeCreationService.getAddEstablisherHeaderSpokes(userAnswers, viewOnly = false)
      result mustBe expectedSpoke
    }
  }

  "getEstablisherIndividualSpokes" must {
    "display all the spokes with appropriate links and incomplete status when no data is returned from TPSS" in {
      val userAnswers =
        ua
          .set(EstablisherKindId(0), EstablisherKind.Individual).success.value
          .set(EstablisherNameId(0), PersonName("a", "b")).success.value

      val expectedSpoke =
        Seq(
          EntitySpoke(
            link = TaskListLink(
              text = "Add details for a b",
              target = EstablishersIndividualRoutes.detailsRoute(0, NormalMode).url,
              visuallyHiddenText = None
            ),
            isCompleted = None
          ),
          EntitySpoke(
            link = TaskListLink(
              text = "Add address for a b",
              target = EstablishersIndividualRoutes.wywnAddressRoute(0, NormalMode).url,
              visuallyHiddenText = None
            ),
            isCompleted = None
          ),
          EntitySpoke(
            link = TaskListLink(
              text = "Add contact details for a b",
              target = contactRoute(0, NormalMode).url,
              visuallyHiddenText = None
            ),
            isCompleted = None
          )
        )

      val result =
        spokeCreationService.getEstablisherIndividualSpokes(
          answers = userAnswers,
          name = "a b",
          index = 0
        )
      result mustBe expectedSpoke
    }
  }

  "getEstablisherCompanySpokes" must {
    "display all the spokes with appropriate links and incomplete status when no data is returned from TPSS" in {
      val userAnswers =
        ua
          .set(EstablisherKindId(0), EstablisherKind.Company).success.value
          .set(CompanyDetailsId(0), CompanyDetails("test")).success.value

      val expectedSpoke =
        Seq(EntitySpoke(
          link = TaskListLink(
            text = "Add details for test",
            target = controllers.establishers.company.details.routes.WhatYouWillNeedController.onPageLoad(0).url,
            visuallyHiddenText = None
          ),
          isCompleted = None
        ),
          EntitySpoke(
            link = TaskListLink(
              text = "Add address for test",
              target = controllers.establishers.company.address.routes.WhatYouWillNeedController.onPageLoad(0).url,
              visuallyHiddenText = None
            ),
            isCompleted = None
          ),
          EntitySpoke(
            link = TaskListLink(
              text = "Add contact details for test",
              target = controllers.establishers.company.contact.routes.WhatYouWillNeedCompanyContactController.onPageLoad(0).url,
              visuallyHiddenText = None
            ),
            isCompleted = None
          ),
          EntitySpoke(
            link = TaskListLink(
              text = "Add directors for test",
              target = controllers.establishers.company.director.details.routes.WhatYouWillNeedController.onPageLoad(0).url,
              visuallyHiddenText = None
            ),
            Some(false)
          )
        )

      val result =
        spokeCreationService.getEstablisherCompanySpokes(
          answers = userAnswers,
          name = "test",
          index = 0
        )
      result mustBe expectedSpoke
    }

  "display all the spokes with appropriate links and incomplete status when data is returned from TPSS for company address spoke" in {
    val userAnswers =
      ua
        .set(EstablisherKindId(0), EstablisherKind.Company).success.value
        .set(CompanyDetailsId(0), CompanyDetails("test")).success.value
        .setOrException(HaveCompanyNumberId(0), true)
        .setOrException(CompanyNumberId(0), ReferenceValue("12345678"))
        .setOrException(IndividualAddressId(0), Data.address)
        .setOrException(EnterPhoneId(0), "1234567890")

      val expectedSpoke =
        Seq(
          EntitySpoke(
            link = TaskListLink(
              text = "Change details for test",
              target = controllers.establishers.company.details.routes.CheckYourAnswersController.onPageLoad(0).url,
              visuallyHiddenText = None
            ),
            isCompleted = Some(false)
          ),
          EntitySpoke(
            link = TaskListLink(
              text = "Change address for test",
              target = controllers.establishers.company.address.routes.CheckYourAnswersController.onPageLoad(0).url,
              visuallyHiddenText = None
            ),
            isCompleted = Some(false)
          ),
          EntitySpoke(
            link = TaskListLink(
              text = "Change contact details for test",
              target = controllers.establishers.company.contact.routes.CheckYourAnswersController.onPageLoad(0).url,
              visuallyHiddenText = None
            ),
            isCompleted = Some(false)
          ),
          EntitySpoke(
            link = TaskListLink(
              text = "Add directors for test",
              target = controllers.establishers.company.director.details.routes.WhatYouWillNeedController.onPageLoad(0).url,
              visuallyHiddenText = None
            ),
            isCompleted = Some(false)
          )
        )

      val result =
        spokeCreationService.getEstablisherCompanySpokes(
          answers = userAnswers,
          name = "test",
          index = 0
        )
      result mustBe expectedSpoke
    }

  "display all the spokes with appropriate links and change  link should display when data is returned from TPSS for director" in {
    val userAnswers =
      ua
        .set(EstablisherKindId(0), EstablisherKind.Company).success.value
        .set(CompanyDetailsId(0), CompanyDetails("test")).success.value
        .set(DirectorNameId(0,0), PersonName("Jane", "Doe")).success.value
        .setOrException(HaveCompanyNumberId(0), true)
        .setOrException(CompanyNumberId(0), ReferenceValue("12345678"))
        .setOrException(IndividualAddressId(0), Data.address)
        .setOrException(EnterPhoneId(0), "1234567890")

    val expectedSpoke =
      Seq(
        EntitySpoke(
          link = TaskListLink(
            text = "Change details for test",
            target = controllers.establishers.company.details.routes.CheckYourAnswersController.onPageLoad(0).url,
            visuallyHiddenText = None
          ),
          isCompleted = Some(false)
        ),
        EntitySpoke(
          link = TaskListLink(
            text = "Change address for test",
            target = controllers.establishers.company.address.routes.CheckYourAnswersController.onPageLoad(0).url,
            visuallyHiddenText = None
          ),
          isCompleted = Some(false)
        ),
        EntitySpoke(
          link = TaskListLink(
            text = "Change contact details for test",
            target = controllers.establishers.company.contact.routes.CheckYourAnswersController.onPageLoad(0).url,
            visuallyHiddenText = None
          ),
          isCompleted = Some(false)
        ),
        EntitySpoke(
          link = TaskListLink(
            text = "Change directors for test",
            target = controllers.establishers.company.routes.AddCompanyDirectorsController.onPageLoad(0,NormalMode).url,
            visuallyHiddenText = None
          ),
          Some(false)
        )
      )

      val result =
        spokeCreationService.getEstablisherCompanySpokes(
          answers = userAnswers,
          name = "test",
          index = 0
        )
      result mustBe expectedSpoke
    }
  }

  "getEstablisherPartnershipSpokes" must {
    "display all the spokes with appropriate links and incomplete status when no data is returned from TPSS" in {
      val userAnswers =
        ua
          .set(EstablisherKindId(0), EstablisherKind.Partnership).success.value
          .set(EstablisherPartnershipDetailsId(0), PartnershipDetails("test")).success.value

      val expectedSpoke =
        Seq(
          EntitySpoke(
            link = TaskListLink(
              text = "Add details for test",
              target = controllers.establishers.partnership.details.routes.WhatYouWillNeedController.onPageLoad(0).url,
              visuallyHiddenText = None
            ),
            isCompleted = None
          ),
          EntitySpoke(
            link = TaskListLink(
              text = "Add address for test",
              target = controllers.establishers.partnership.address.routes.WhatYouWillNeedController.onPageLoad(0).url,
              visuallyHiddenText = None
            ),
            isCompleted = None
          ),
          EntitySpoke(
            link = TaskListLink(
              text = "Add contact details for test",
              target = controllers.establishers.partnership.contact.routes.WhatYouWillNeedController.onPageLoad(0).url,
              visuallyHiddenText = None
            ),
            isCompleted = None
          ),
          EntitySpoke(
            link = TaskListLink(
              text = "Add partners for test",
              target = controllers.establishers.partnership.partner.details.routes.WhatYouWillNeedController.onPageLoad(0).url,
              visuallyHiddenText = None
            ),
            Some(false)
          )
        )

      val result =
      spokeCreationService.getEstablisherPartnershipSpokes(
          answers = userAnswers,
          name = "test",
          index = 0
        )
      result mustBe expectedSpoke
    }

    "display all the spokes with appropriate links and incomplete status when data is returned from TPSS" in {
      val userAnswers =
        ua
          .set(EstablisherKindId(0), EstablisherKind.Partnership).success.value
          .set(EstablisherPartnershipDetailsId(0), PartnershipDetails("test")).success.value
          .setOrException(HaveUTRId(0), true)
          .setOrException(PartnershipUTRId(0), ReferenceValue("12345678"))
          .setOrException(PartnershipAddressId(0), Data.address)
          .setOrException(EnterEmailId(0), "11")
          .set(identifiers.establishers.partnership.partner.address.AddressId(0,0), Data.address).success.value
          .set(identifiers.establishers.partnership.partner.address.AddressYearsId(0,0), true).success.value
          .set(identifiers.establishers.partnership.partner.contact.EnterEmailId(0,0), "t@t.c").success.value
          .set(identifiers.establishers.partnership.partner.contact.EnterPhoneId(0,0), "1").success.value
          .set(identifiers.establishers.partnership.partner.details.PartnerDOBId(0,0), LocalDate.of(2000,12,12)).success.value
          .set(identifiers.establishers.partnership.partner.details.PartnerEnterUTRId(0,0), ReferenceValue("test")).success.value
          .set(identifiers.establishers.partnership.partner.details.PartnerNINOId(0,0), ReferenceValue("test")).success.value
          .set(identifiers.establishers.partnership.partner.details.PartnerHasUTRId(0,0), true).success.value
          .set(identifiers.establishers.partnership.partner.details.PartnerHasNINOId(0,0), true).success.value
          .set(PartnerNameId(0,0), PersonName("Jane", "Doe")).success.value

      val expectedSpoke =
        Seq(
          EntitySpoke(
            link = TaskListLink(
              text = "Change details for test",
              target = controllers.establishers.partnership.details.routes.CheckYourAnswersController.onPageLoad(0).url,
              visuallyHiddenText = None
            ),
            isCompleted = Some(false)
          ),
          EntitySpoke(
            link = TaskListLink(
              text = "Change address for test",
              target = controllers.establishers.partnership.address.routes.CheckYourAnswersController.onPageLoad(0).url,
              visuallyHiddenText = None
            ),
            isCompleted = Some(false)
          ),
          EntitySpoke(
            link = TaskListLink(
              text = "Change contact details for test",
              target = controllers.establishers.partnership.contact.routes.CheckYourAnswersController.onPageLoad(0).url,
              visuallyHiddenText = None
            ),
            isCompleted = Some(false)
          ),
          EntitySpoke(
            link = TaskListLink(
              text = "Change partners for test",
              target = controllers.establishers.partnership.routes.AddPartnersController.onPageLoad(0,NormalMode).url,
              visuallyHiddenText = None
            ),
            Some(false)
          )
        )

      val result =
        spokeCreationService.getEstablisherPartnershipSpokes(
          answers = userAnswers,
          name = "test",
          index = 0
        )
      result mustBe expectedSpoke
    }
  }

  "getTrusteesIndividualSpokes" must {
    "display all the spokes with appropriate links and incomplete status when no data is returned from TPSS" in {

      val userAnswers =
        ua
          .set(TrusteeKindId(0), TrusteeKind.Individual).success.value
          .set(TrusteeNameId(0), PersonName("a", "b")).success.value

      val expectedSpoke =
        Seq(
          EntitySpoke(
            link = TaskListLink(
              text = "Add details for a b",
              target = TrusteesIndividualRoutes.detailsRoute(0, NormalMode).url,
              visuallyHiddenText = None
            ),
            isCompleted = None
          ),
          EntitySpoke(
            link = TaskListLink(
              text = "Add address for a b",
              target = TrusteesIndividualRoutes.wywnAddressRoute(0, NormalMode).url,
              visuallyHiddenText = None
            ),
            isCompleted = None
          ),
          EntitySpoke(
            link = TaskListLink(
              text = "Add contact details for a b",
              target = TrusteesIndividualRoutes.contactRoute(0, NormalMode).url,
              visuallyHiddenText = None
            ),
            isCompleted = None
          )
        )

      val result =
        spokeCreationService.getTrusteeIndividualSpokes(
          answers = userAnswers,
          name = "a b",
          index = 0
        )
      result mustBe expectedSpoke
    }

    "display all the spokes with appropriate links and incomplete status when complete data is returned from TPSS" in {
      val userAnswers =
        ua
          .set(TrusteeKindId(0), TrusteeKind.Individual).success.value
          .set(TrusteeNameId(0), PersonName("a", "b")).success.value
          .set(TrusteeDOBId(0), LocalDate.now).success.value
          .set(TrusteeNINOId(0), ReferenceValue("AB123456C")).success.value
          .set(TrusteeUTRId(0), ReferenceValue("1234567890")).success.value
          .set(TrusteeEmailId(0), "test@test.com").success.value
          .set(TrusteePhoneId(0), "1234").success.value
          .setOrException(identifiers.trustees.individual.address.AddressId(0), Data.address)

      val expectedSpoke =
        Seq(
          EntitySpoke(
            link = TaskListLink(
              text = "Change details for a b",
              target = TrusteesIndividualRoutes.cyaDetailsRoute(0, NormalMode).url,
              visuallyHiddenText = None
            ),
            isCompleted = Some(true)
          ),
          EntitySpoke(
            link = TaskListLink(
              text = "Change address for a b",
              target = TrusteesIndividualRoutes.cyaAddressRoute(0, NormalMode).url,
              visuallyHiddenText = None
            ),
            isCompleted = Some(false)
          ),
          EntitySpoke(
            link = TaskListLink(
              text = "Change contact details for a b",
              target = TrusteesIndividualRoutes.cyaContactRoute(0, NormalMode).url,
              visuallyHiddenText = None
            ),
            isCompleted = Some(true)
          )
        )

      val result =
        spokeCreationService.getTrusteeIndividualSpokes(
          answers = userAnswers,
          name = "a b",
          index = 0
        )
      result mustBe expectedSpoke
    }
  }

  "getTrusteeCompanySpokes" must {
    "display all the spokes with appropriate links and incomplete status when no data is returned from TPSS" in {
      val userAnswers =
        ua
          .set(TrusteeKindId(0), TrusteeKind.Company).success.value
          .set(trusteeCompany.CompanyDetailsId(0), CompanyDetails("test")).success.value

      val expectedSpoke =
        Seq(EntitySpoke(
          link = TaskListLink(
            text = "Add details for test",
            target = controllers.trustees.company.details.routes.WhatYouWillNeedController.onPageLoad(0).url,
            visuallyHiddenText = None
          ),
          isCompleted = None
        ),
          EntitySpoke(
            link = TaskListLink(
              text = "Add address for test",
              target = controllers.trustees.company.address.routes.WhatYouWillNeedController.onPageLoad(0).url,
              visuallyHiddenText = None
            ),
            isCompleted = None
          ),
          EntitySpoke(
            link = TaskListLink(
              text = "Add contact details for test",
              target = controllers.trustees.company.contacts.routes.WhatYouWillNeedCompanyContactController.onPageLoad(0).url,
              visuallyHiddenText = None
            ),
            isCompleted = None
          )
        )

      val result =
        spokeCreationService.getTrusteeCompanySpokes(
          answers = userAnswers,
          name = "test",
          index = 0
        )
      result mustBe expectedSpoke
    }

  "display all the spokes with appropriate links and incomplete status when data is returned from TPSS for trustee company spokes" in {
    val userAnswers =
      ua
        .set(TrusteeKindId(0), TrusteeKind.Company).success.value
        .set(trusteeCompany.CompanyDetailsId(0), CompanyDetails("test")).success.value
        .setOrException(trusteeCompanyDetails.HaveCompanyNumberId(0), true)
        .setOrException(trusteeCompanyDetails.CompanyNumberId(0), ReferenceValue("12345678"))


      val expectedSpoke =
        Seq(
          EntitySpoke(
            link = TaskListLink(
              text = "Change details for test",
              target = controllers.trustees.company.details.routes.CheckYourAnswersController.onPageLoad(0).url,
              visuallyHiddenText = None
            ),
            isCompleted = Some(false)
          ),
          EntitySpoke(
            link = TaskListLink(
              text = "Add address for test",
              target = controllers.trustees.company.address.routes.WhatYouWillNeedController.onPageLoad(0).url,
              visuallyHiddenText = None
            ),
            isCompleted = None
          ),
          EntitySpoke(
            link = TaskListLink(
              text = "Add contact details for test",
              target = controllers.trustees.company.contacts.routes.WhatYouWillNeedCompanyContactController.onPageLoad(0).url,
              visuallyHiddenText = None
            ),
            isCompleted = None
          )
        )

      val result =
        spokeCreationService.getTrusteeCompanySpokes(
          answers = userAnswers,
          name = "test",
          index = 0
        )
      result mustBe expectedSpoke
    }

  "display all the spokes with appropriate links and complete status when data is returned from TPSS for trustee company spokes" in {
    val userAnswers =
      ua
        .set(EstablisherKindId(0), EstablisherKind.Company).success.value
        .set(CompanyDetailsId(0), CompanyDetails("test")).success.value
        .set(trusteeCompanyDetails.HaveCompanyNumberId(0), true).success.value
        .set(trusteeCompanyDetails.CompanyNumberId(0), ReferenceValue("AB123456C")).success.value
        .set(trusteeCompanyDetails.HaveUTRId(0), true).success.value
        .set(trusteeCompanyDetails.CompanyUTRId(0), ReferenceValue("1234567890")).success.value
        .set(trusteeCompanyDetails.HaveVATId(0), true).success.value
        .set(trusteeCompanyDetails.VATId(0), ReferenceValue("123456789")).success.value
        .set(trusteeCompanyDetails.HavePAYEId(0), true).success.value
        .set(trusteeCompanyDetails.PAYEId(0), ReferenceValue("12345678")).success.value


        val expectedSpoke =
          Seq(
            EntitySpoke(
              link = TaskListLink(
                text = "Change details for test",
                target = controllers.trustees.company.details.routes.CheckYourAnswersController.onPageLoad(0).url,
                visuallyHiddenText = None
              ),
              isCompleted = Some(true)
            ),
            EntitySpoke(
              link = TaskListLink(
                text = "Add address for test",
                target = controllers.trustees.company.address.routes.WhatYouWillNeedController.onPageLoad(0).url,
                visuallyHiddenText = None
              ),
              isCompleted = None
            ),
            EntitySpoke(
              link = TaskListLink(
                text = "Add contact details for test",
                target = controllers.trustees.company.contacts.routes.WhatYouWillNeedCompanyContactController.onPageLoad(0).url,
                visuallyHiddenText = None
              ),
              isCompleted = None
            )
          )

        val result =
          spokeCreationService.getTrusteeCompanySpokes(
            answers = userAnswers,
            name = "test",
            index = 0
          )
        result mustBe expectedSpoke
      }
  }

  "getTrusteePartnershipSpokes" must {

    "display all the spokes with appropriate links and incomplete status when no data is returned from TPSS" in {

      val userAnswers =
        ua
          .set(TrusteeKindId(0), TrusteeKind.Partnership).success.value
          .set(TrusteeNameId(0), PersonName("a", "b")).success.value
          .set(TrusteePartnershipDetailsId(0), PartnershipDetails("a b",false)).success.value

      val expectedSpoke =
        Seq(
          EntitySpoke(
            link = TaskListLink(
              text = "Add details for a b",
              target = controllers.trustees.partnership.details.routes.WhatYouWillNeedController.onPageLoad(0).url,
              visuallyHiddenText = None
            ),
            isCompleted = None
          ),
          EntitySpoke(
            link = TaskListLink(
              text = "Add address for a b",
              target = controllers.trustees.partnership.address.routes.WhatYouWillNeedController.onPageLoad(0).url,
              visuallyHiddenText = None
            ),
            isCompleted = None
          ),
          EntitySpoke(
            link = TaskListLink(
              text = "Add contact details for a b",
              target = controllers.trustees.partnership.contact.routes.WhatYouWillNeedController.onPageLoad(0).url,
              visuallyHiddenText = None
            ),
            isCompleted = None
          )
        )

      val result =
        spokeCreationService.getTrusteePartnershipSpokes(
          answers = userAnswers,
          name = "a b",
          index = 0
        )
      result mustBe expectedSpoke
    }

    "display all the spokes with appropriate links and complete status when data is returned from TPSS incl partners incomplete when only one partner" in {
      val userAnswers =
        ua
          .set(TrusteeKindId(0), TrusteeKind.Partnership).success.value
          .set(TrusteePartnershipDetailsId(0), PartnershipDetails("a b", false)).success.value
          .set(TrusteeHaveUTRId(0), true).success.value
          .set(TrusteeUTRId(0), ReferenceValue("1234567890")).success.value
          .set(TrusteeHaveVATId(0), true).success.value
          .set(TrusteeVATId(0), ReferenceValue("123456789")).success.value
          .set(TrusteeHavePAYEId(0), true).success.value
          .set(TrusteePAYEId(0), ReferenceValue("12345678")).success.value
          .set(TrusteeNameId(0), PersonName("a", "b")).success.value
          .set(TrusteePartnershipAddressId(0), Data.address).success.value
          .set(TrusteePartnershipAddressYearsId(0), true).success.value
          .set(TrusteePartnershipEmailId(0), "test@test.com").success.value
          .set(TrusteePartnershipPhoneId(0), "1234").success.value

      val expectedSpoke =
        Seq(
          EntitySpoke(
            link = TaskListLink(
              text = "Change details for a b",
              target = controllers.trustees.partnership.details.routes.CheckYourAnswersController.onPageLoad(0).url,
              visuallyHiddenText = None
            ),
            isCompleted = Some(true)
          ),
          EntitySpoke(
            link = TaskListLink(
              text = "Change address for a b",
              target = controllers.trustees.partnership.address.routes.CheckYourAnswersController.onPageLoad(0).url,
              visuallyHiddenText = None
            ),
            isCompleted = Some(true)
          ),
          EntitySpoke(
            link = TaskListLink(
              text = "Change contact details for a b",
              target = controllers.trustees.partnership.contact.routes.CheckYourAnswersController.onPageLoad(0).url,
              visuallyHiddenText = None
            ),
            isCompleted = Some(true)
          )
        )

      val result =
        spokeCreationService.getTrusteePartnershipSpokes(
          answers = userAnswers,
          name = "a b",
          index = 0
        )
      result mustBe expectedSpoke
    }
  }

  "declarationSpoke" must {

    "return declaration spoke with link" in {
      val expectedSpoke = Seq(EntitySpoke(TaskListLink(
        messages("messages__schemeTaskList__declaration_link"),
        controllers.routes.DeclarationController.onPageLoad().url)
      ))

      spokeCreationService.declarationSpoke mustBe expectedSpoke
    }
  }
}


