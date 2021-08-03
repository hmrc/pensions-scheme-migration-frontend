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
import controllers.establishers.individual.details.routes
import identifiers.beforeYouStart.{EstablishedCountryId, SchemeTypeId, WorkingKnowledgeId}
import identifiers.establishers.EstablisherKindId
import identifiers.establishers.company.CompanyDetailsId
import identifiers.establishers.company.contact.EnterPhoneId
import identifiers.establishers.company.details.{CompanyNumberId, HaveCompanyNumberId}
import identifiers.establishers.individual.EstablisherNameId
import identifiers.establishers.individual.address.AddressId
import identifiers.trustees.TrusteeKindId
import identifiers.trustees.{company => trusteeCompany}
import identifiers.trustees.company.{details => trusteeCompanyDetails}
import identifiers.trustees.individual.TrusteeNameId
import identifiers.trustees.individual.contact.{EnterEmailId => TrusteeEmailId, EnterPhoneId => TrusteePhoneId}
import identifiers.trustees.individual.details.{TrusteeDOBId, TrusteeNINOId, TrusteeUTRId}
import models.establishers.EstablisherKind
import models.trustees.TrusteeKind
import models.{EntitySpoke, _}
import org.scalatest.{MustMatchers, OptionValues, TryValues}
import utils.Data.{schemeName, ua}
import utils.{Data, Enumerable}
import viewmodels.Message

import java.time.LocalDate

class SpokeCreationServiceSpec
  extends SpecBase
    with MustMatchers
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
              target = routes.WhatYouWillNeedController.onPageLoad(0).url,
              visuallyHiddenText = None
            ),
            isCompleted = None
          ),
          EntitySpoke(
            link = TaskListLink(
              text = "Add address for a b",
              target = controllers.establishers.individual.address.routes.WhatYouWillNeedController.onPageLoad(0).url,
              visuallyHiddenText = None
            ),
            isCompleted = None
          ),
          EntitySpoke(
            link = TaskListLink(
              text = "Add contact details for a b",
              target = controllers.establishers.individual.contact.routes.WhatYouWillNeedController.onPageLoad(0).url,
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
          .set(CompanyDetailsId(0), CompanyDetails("test",false)).success.value

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

  "display all the spokes with appropriate links and incomplete status when data is returned from TPSS for company address spoke" in {
    val userAnswers =
      ua
        .set(EstablisherKindId(0), EstablisherKind.Company).success.value
        .set(CompanyDetailsId(0), CompanyDetails("test",false)).success.value
        .setOrException(HaveCompanyNumberId(0), true)
        .setOrException(CompanyNumberId(0), ReferenceValue("12345678"))
        .setOrException(AddressId(0), Data.address)
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
              target = controllers.trustees.individual.details.routes.WhatYouWillNeedController.onPageLoad(0).url,
              visuallyHiddenText = None
            ),
            isCompleted = None
          ),
          EntitySpoke(
            link = TaskListLink(
              text = "Add address for a b",
              target = controllers.trustees.individual.address.routes.WhatYouWillNeedController.onPageLoad(0).url,
              visuallyHiddenText = None
            ),
            isCompleted = None
          ),
          EntitySpoke(
            link = TaskListLink(
              text = "Add contact details for a b",
              target = controllers.trustees.individual.contact.routes.WhatYouWillNeedController.onPageLoad(0).url,
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
              target = controllers.trustees.individual.details.routes.CheckYourAnswersController.onPageLoad(0).url,
              visuallyHiddenText = None
            ),
            isCompleted = Some(true)
          ),
          EntitySpoke(
            link = TaskListLink(
              text = "Change address for a b",
              target = controllers.trustees.individual.address.routes.CheckYourAnswersController.onPageLoad(0).url,
              visuallyHiddenText = None
            ),
            isCompleted = Some(false)
          ),
          EntitySpoke(
            link = TaskListLink(
              text = "Change contact details for a b",
              target = controllers.trustees.individual.contact.routes.CheckYourAnswersController.onPageLoad(0).url,
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
          .set(trusteeCompany.CompanyDetailsId(0), CompanyDetails("test",false)).success.value

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

  "display all the spokes with appropriate links and incomplete status when data is returned from TPSS for trustee company spokes" in {
    val userAnswers =
      ua
        .set(TrusteeKindId(0), TrusteeKind.Company).success.value
        .set(trusteeCompany.CompanyDetailsId(0), CompanyDetails("test",false)).success.value
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
        .set(CompanyDetailsId(0), CompanyDetails("test",false)).success.value
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

