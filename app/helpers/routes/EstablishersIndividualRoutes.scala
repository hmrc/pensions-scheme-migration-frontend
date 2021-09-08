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
import controllers.establishers.individual.routes._

object EstablishersIndividualRoutes {


  def wywnAddressRoute(index: Index, mode: Mode): Call =
    controllers.establishers.individual.address.routes.WhatYouWillNeedController.onPageLoad(index)
    //EstablisherIndividualController.onPageLoad(index, mode, "address")

  def enterPostcodeRoute(index: Index, mode: Mode): Call =
    controllers.establishers.individual.address.routes.EnterPostcodeController.onPageLoad(index)
    //EstablisherIndividualController.onPageLoad(index, mode, "enter-postcode")

  def enterPostcodePOSTRoute(index: Index, mode: Mode): Call =
    controllers.establishers.individual.address.routes.EnterPostcodeController.onSubmit(index: Index)
    //EstablisherIndividualController.onSubmit(index, mode, "enter-postcode")

  def selectAddressRoute(index: Index, mode: Mode): Call =
    controllers.establishers.individual.address.routes.SelectAddressController.onPageLoad(index)
    //EstablisherIndividualController.onPageLoad(index, mode, "address-results")

  def selectAddressPOSTRoute(index: Index, mode: Mode): Call =
    controllers.establishers.individual.address.routes.SelectAddressController.onSubmit(index: Index)
    //EstablisherIndividualController.onSubmit(index, mode, "address-results")

  def confirmAddressRoute(index: Index, mode: Mode): Call =
    controllers.establishers.individual.address.routes.ConfirmAddressController.onPageLoad(index)
    //EstablisherIndividualController.onPageLoad(index, mode, "confirm-address")

  def cyaAddressRoute(index: Index, mode: Mode): Call =
    controllers.establishers.individual.address.routes.CheckYourAnswersController.onPageLoad(index)
    //EstablisherIndividualController.onPageLoad(index, mode, "check-your-answers-address")

  def timeAtAddressRoute(index: Index, mode: Mode): Call =
    controllers.establishers.individual.address.routes.AddressYearsController.onPageLoad(index)
    //EstablisherIndividualController.onPageLoad(index, mode, "time-at-address")

  def timeAtAddressPOSTRoute(index: Index, mode: Mode): Call =
    controllers.establishers.individual.address.routes.AddressYearsController.onSubmit(index: Index)
    //EstablisherIndividualController.onSubmit(index, mode, "time-at-address")

  def enterPreviousPostcodeRoute(index: Index, mode: Mode): Call =
    controllers.establishers.individual.address.routes.EnterPreviousPostcodeController.onPageLoad(index)
    //EstablisherIndividualController.onPageLoad(index, mode, "enter-previous-postcode")

  def enterPreviousPostcodePOSTRoute(index: Index, mode: Mode): Call =
    controllers.establishers.individual.address.routes.EnterPreviousPostcodeController.onSubmit(index: Index)
    //EstablisherIndividualController.onSubmit(index, mode, "enter-previous-postcode")

  def previousAddressResultsRoute(index: Index, mode: Mode): Call =
    controllers.establishers.individual.address.routes.SelectPreviousAddressController.onPageLoad(index)
    //EstablisherIndividualController.onPageLoad(index, mode, "previous-address-results")

  def previousAddressResultsPOSTRoute(index: Index, mode: Mode): Call =
    controllers.establishers.individual.address.routes.SelectPreviousAddressController.onSubmit(index: Index)
    //EstablisherIndividualController.onSubmit(index, mode, "previous-address-results")

  def confirmPreviousAddressRoute(index: Index, mode: Mode): Call =
    controllers.establishers.individual.address.routes.ConfirmPreviousAddressController.onPageLoad(index)
    //EstablisherIndividualController.onPageLoad(index, mode, "confirm-previous-address")

  def confirmPreviousAddressPOSTRoute(index: Index, mode: Mode): Call =
    controllers.establishers.individual.address.routes.ConfirmPreviousAddressController.onSubmit(index: Index)
    //EstablisherIndividualController.onSubmit(index, mode, "confirm-previous-address")
}
