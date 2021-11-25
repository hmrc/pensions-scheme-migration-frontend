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

package controllers.trustees.individual

import controllers.trustees.individual.details._
import models.{Mode, Index}
import play.api.mvc.{Action, AnyContent}

import javax.inject.Inject
import scala.concurrent.ExecutionContext

/*
  This controller is a short-term fix for an issue the Plat UI team are looking into
  which means that our code is running out of routes.
  A lot of controllers are injected into this controller. If this fix becomes a longer-term
  fix then we should probably split this into separate services to reduce the number.
 */

class TrusteeIndividualController @Inject()(
  trusteeNameController: TrusteeNameController,
  whatYouWillNeedController: WhatYouWillNeedController, trusteeDOBController: TrusteeDOBController,
  trusteeHasNINOController: TrusteeHasNINOController, trusteeHasUTRController: TrusteeHasUTRController,
  trusteeEnterUTRController: TrusteeEnterUTRController,
  trusteeEnterNINOController: TrusteeEnterNINOController,
  trusteeNoNINOReasonController: TrusteeNoNINOReasonController,
  trusteeNoUTRReasonController: TrusteeNoUTRReasonController,
  checkYourAnswersController: CheckYourAnswersController,
  whatYouWillNeedContactController: contact.WhatYouWillNeedController,
  enterEmailController: contact.EnterEmailController, enterPhoneController: contact.EnterPhoneController,
  checkYourAnswersContactController: contact.CheckYourAnswersController,
  whatYouWillNeedAddressController: address.WhatYouWillNeedController,
  enterPostcodeController: address.EnterPostcodeController, selectAddressController: address.SelectAddressController,
  confirmAddressController: address.ConfirmAddressController,
  checkYourAnswersAddressController: address.CheckYourAnswersController,
  addressYearsController: address.AddressYearsController,
  enterPreviousPostcodeController: address.EnterPreviousPostcodeController,
  selectPreviousAddressController: address.SelectPreviousAddressController,
  confirmPreviousAddressController: address.ConfirmPreviousAddressController,
  directorsAlsoTrusteesController: DirectorsAlsoTrusteesController,
  directorAlsoTrusteeController: DirectorAlsoTrusteeController,
  taskListController: SpokeTaskListController)
  (implicit val executionContext: ExecutionContext) {

  //scalastyle:off cyclomatic.complexity
  def onPageLoad(index: Index, mode: Mode, page: String): Action[AnyContent] = {
    page match {
      case "name" => trusteeNameController.onPageLoad(index)
      case "details" => whatYouWillNeedController.onPageLoad(index)
      case "date-of-birth" => trusteeDOBController.onPageLoad(index, mode)
      case "have-national-insurance-number" => trusteeHasNINOController.onPageLoad(index, mode)
      case "have-unique-taxpayer-reference" => trusteeHasUTRController.onPageLoad(index, mode)
      case "enter-unique-taxpayer-reference" => trusteeEnterUTRController.onPageLoad(index, mode)
      case "enter-national-insurance-number" => trusteeEnterNINOController.onPageLoad(index, mode)
      case "reason-for-no-national-insurance-number" => trusteeNoNINOReasonController.onPageLoad(index, mode)
      case "reason-for-no-unique-taxpayer-reference" => trusteeNoUTRReasonController.onPageLoad(index, mode)
      case "check-your-answers-details" => checkYourAnswersController.onPageLoad(index)
      case "contact-details" => whatYouWillNeedContactController.onPageLoad(index)
      case "enter-email-address" => enterEmailController.onPageLoad(index, mode)
      case "enter-phone-number" => enterPhoneController.onPageLoad(index, mode)
      case "check-your-answers-contact-details" => checkYourAnswersContactController.onPageLoad(index)
      case "address" => whatYouWillNeedAddressController.onPageLoad(index)
      case "enter-postcode" => enterPostcodeController.onPageLoad(index)
      case "address-results" => selectAddressController.onPageLoad(index)
      case "confirm-address" => confirmAddressController.onPageLoad(index)
      case "check-your-answers-address" => checkYourAnswersAddressController.onPageLoad(index)
      case "time-at-address" => addressYearsController.onPageLoad(index)
      case "enter-previous-postcode" => enterPreviousPostcodeController.onPageLoad(index)
      case "previous-address-results" => selectPreviousAddressController.onPageLoad(index)
      case "confirm-previous-address" => confirmPreviousAddressController.onPageLoad(index)
      case "task-list" => taskListController.onPageLoad(index)
      case "director-also-trustee" => directorAlsoTrusteeController.onPageLoad(index)
      case "directors-also-trustees" => directorsAlsoTrusteesController.onPageLoad(index)
      case _ => throw new RuntimeException("No route")
    }
  }

  //scalastyle:off cyclomatic.complexity
  def onSubmit(index: Index, mode: Mode, page: String): Action[AnyContent] = {
    page match {
      case "name" => trusteeNameController.onSubmit(index)
      case "date-of-birth" => trusteeDOBController.onSubmit(index, mode)
      case "have-national-insurance-number" => trusteeHasNINOController.onSubmit(index, mode)
      case "have-unique-taxpayer-reference" => trusteeHasUTRController.onSubmit(index, mode)
      case "enter-unique-taxpayer-reference" => trusteeEnterUTRController.onSubmit(index, mode)
      case "enter-national-insurance-number" => trusteeEnterNINOController.onSubmit(index, mode)
      case "reason-for-no-national-insurance-number" => trusteeNoNINOReasonController.onSubmit(index, mode)
      case "reason-for-no-unique-taxpayer-reference" => trusteeNoUTRReasonController.onSubmit(index, mode)
      case "enter-email-address" => enterEmailController.onSubmit(index, mode)
      case "enter-phone-number" => enterPhoneController.onSubmit(index, mode)
      case "enter-postcode" => enterPostcodeController.onSubmit(index)
      case "address-results" => selectAddressController.onSubmit(index)
      case "confirm-address" => confirmAddressController.onSubmit(index)
      case "time-at-address" => addressYearsController.onSubmit(index)
      case "enter-previous-postcode" => enterPreviousPostcodeController.onSubmit(index)
      case "previous-address-results" => selectPreviousAddressController.onSubmit(index)
      case "confirm-previous-address" => confirmPreviousAddressController.onSubmit(index)
      case "director-also-trustee" => directorAlsoTrusteeController.onSubmit(index)
      case "directors-also-trustees" => directorsAlsoTrusteesController.onSubmit(index)
      case _ => throw new RuntimeException("No route")
    }
  }

}
