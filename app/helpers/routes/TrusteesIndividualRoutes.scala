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

package helpers.routes

import models.{Mode, Index}
import play.api.mvc.Call
import controllers.trustees.individual.routes._

object TrusteesIndividualRoutes {

  def detailsRoute(index: Index, mode: Mode): Call = TrusteeIndividualController.onPageLoad(index, mode, "details")

  def nameRoute(index: Index, mode: Mode): Call = TrusteeIndividualController.onPageLoad(index, mode, "name")

  def namePOSTRoute(index: Index, mode: Mode): Call = TrusteeIndividualController.onSubmit(index, mode, "name")

  def enterUniqueTaxpayerReferenceRoute(index: Index, mode: Mode): Call = TrusteeIndividualController
    .onPageLoad(index, mode, "enter-unique-taxpayer-reference")

  def enterNationaInsuranceNumberRoute(index: Index, mode: Mode): Call = TrusteeIndividualController
    .onPageLoad(index, mode, "enter-national-insurance-number")

  def haveUniqueTaxpayerReferenceRoute(index: Index, mode: Mode): Call = TrusteeIndividualController
    .onPageLoad(index, mode, "have-unique-taxpayer-reference")

  def reasonForNoNationalInsuranceNumberRoute(index: Index, mode: Mode): Call = TrusteeIndividualController
    .onPageLoad(index, mode, "reason-for-no-national-insurance-number")

  def reasonForNoUniqueTaxpayerReferenceRoute(index: Index, mode: Mode): Call = TrusteeIndividualController
    .onPageLoad(index, mode, "reason-for-no-unique-taxpayer-reference")

  def haveNationalInsuranceNumberRoute(index: Index, mode: Mode): Call = TrusteeIndividualController
    .onPageLoad(index, mode, "have-national-insurance-number")

  def dateOfBirthRoute(index: Index, mode: Mode): Call = TrusteeIndividualController
    .onPageLoad(index, mode, "date-of-birth")

  def cyaDetailsRoute(index: Index, mode: Mode): Call = TrusteeIndividualController
    .onPageLoad(index, mode, "check-your-answers-details")

  def contactRoute(index: Index, mode: Mode): Call = TrusteeIndividualController
    .onPageLoad(index, mode, "contact-details")

  def emailRoute(index: Index, mode: Mode): Call = TrusteeIndividualController
    .onPageLoad(index, mode, "enter-email-address")

  def phoneNumberRoute(index: Index, mode: Mode): Call = TrusteeIndividualController
    .onPageLoad(index, mode, "enter-phone-number")

  def cyaContactRoute(index: Index, mode: Mode): Call = TrusteeIndividualController
    .onPageLoad(index, mode, "check-your-answers-contact-details")

  def wywnAddressRoute(index: Index, mode: Mode): Call = TrusteeIndividualController
    .onPageLoad(index, mode, "address")

  def enterPostcodeRoute(index: Index, mode: Mode): Call = TrusteeIndividualController
    .onPageLoad(index, mode, "enter-postcode")

  def enterPostcodePOSTRoute(index: Index, mode: Mode): Call = TrusteeIndividualController
    .onSubmit(index, mode, "enter-postcode")

  def selectAddressRoute(index: Index, mode: Mode): Call = TrusteeIndividualController
    .onPageLoad(index, mode, "address-results")

  def selectAddressPOSTRoute(index: Index, mode: Mode): Call = TrusteeIndividualController
    .onSubmit(index, mode, "address-results")

  def confirmAddressRoute(index: Index, mode: Mode): Call = TrusteeIndividualController
    .onPageLoad(index, mode, "confirm-address")

  def confirmAddressPOSTRoute(index: Index, mode: Mode): Call = TrusteeIndividualController
    .onSubmit(index, mode, "confirm-address")

  def cyaAddressRoute(index: Index, mode: Mode): Call = TrusteeIndividualController
    .onPageLoad(index, mode, "check-your-answers-address")

  def timeAtAddressRoute(index: Index, mode: Mode): Call = TrusteeIndividualController
    .onPageLoad(index, mode, "time-at-address")

  def timeAtAddressPOSTRoute(index: Index, mode: Mode): Call = TrusteeIndividualController
    .onSubmit(index, mode, "time-at-address")

  def enterPreviousPostcodeRoute(index: Index, mode: Mode): Call = TrusteeIndividualController
    .onPageLoad(index, mode, "enter-previous-postcode")

  def enterPreviousPostcodePOSTRoute(index: Index, mode: Mode): Call = TrusteeIndividualController
    .onSubmit(index, mode, "enter-previous-postcode")

  def previousAddressResultsRoute(index: Index, mode: Mode): Call = TrusteeIndividualController
    .onPageLoad(index, mode, "previous-address-results")

  def previousAddressResultsPOSTRoute(index: Index, mode: Mode): Call = TrusteeIndividualController
    .onSubmit(index, mode, "previous-address-results")

  def confirmPreviousAddressRoute(index: Index, mode: Mode): Call = TrusteeIndividualController
    .onPageLoad(index, mode, "confirm-previous-address")

  def confirmPreviousAddressPOSTRoute(index: Index, mode: Mode): Call = TrusteeIndividualController
    .onSubmit(index, mode, "confirm-previous-address")

  def taskListRoute(index: Index, mode: Mode): Call = TrusteeIndividualController
    .onPageLoad(index, mode, "task-list")

  def directorAlsoTrusteeRoute(index: Index, mode: Mode): Call = TrusteeIndividualController
    .onPageLoad(index, mode, "director-also-trustee")

  def directorsAlsoTrusteesRoute(index: Index, mode: Mode): Call = TrusteeIndividualController
    .onPageLoad(index, mode, "directors-also-trustees")
}
