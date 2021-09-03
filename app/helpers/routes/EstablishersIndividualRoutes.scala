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

  def detailsRoute(index:Index, mode:Mode):Call =
    EstablisherIndividualController.onPageLoad(index, mode, "details")

  def nameRoute(index:Index, mode:Mode):Call =
    EstablisherIndividualController.onPageLoad(index, mode, "name")

  def namePOSTRoute(index:Index, mode:Mode):Call =
    EstablisherIndividualController.onSubmit(index, mode, "name")

  def enterUniqueTaxpayerReferenceRoute(index:Index, mode:Mode):Call =
    EstablisherIndividualController.onPageLoad(index, mode, "enter-unique-taxpayer-reference")

  def enterNationaInsuranceNumberRoute(index:Index, mode:Mode):Call =
    EstablisherIndividualController.onPageLoad(index, mode, "enter-national-insurance-number")

  def haveUniqueTaxpayerReferenceRoute(index:Index, mode:Mode):Call =
    EstablisherIndividualController.onPageLoad(index, mode, "have-unique-taxpayer-reference")

  def reasonForNoNationalInsuranceNumberRoute(index:Index, mode:Mode):Call =
    EstablisherIndividualController.onPageLoad(index, mode, "reason-for-no-national-insurance-number")

  def reasonForNoUniqueTaxpayerReferenceRoute(index:Index, mode:Mode):Call =
    EstablisherIndividualController.onPageLoad(index, mode, "reason-for-no-unique-taxpayer-reference")

  def haveNationalInsuranceNumberRoute(index:Index, mode:Mode):Call =
    EstablisherIndividualController.onPageLoad(index, mode, "have-national-insurance-number")

  def dateOfBirthRoute(index:Index, mode:Mode):Call =
    EstablisherIndividualController.onPageLoad(index, mode, "date-of-birth")

  def cyaRoute(index:Index, mode:Mode):Call =
    EstablisherIndividualController.onPageLoad(index, mode, "check-your-answers-details")
}
