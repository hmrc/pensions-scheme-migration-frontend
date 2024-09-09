/*
 * Copyright 2024 HM Revenue & Customs
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

package controllers.trustees.company.address

import controllers.Retrievals
import controllers.actions._
import forms.address.AddressYearsFormProvider
import identifiers.beforeYouStart.SchemeNameId
import identifiers.trustees.company.CompanyDetailsId
import identifiers.trustees.company.address.AddressYearsId
import models.{Index, Mode}
import play.api.data.Form
import play.api.mvc.{Action, AnyContent}
import services.common.CommonAddressYearsService
import viewmodels.Message

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class AddressYearsController @Inject()(
                                       authenticate: AuthAction,
                                       getData: DataRetrievalAction,
                                       requireData: DataRequiredAction,
                                       formProvider: AddressYearsFormProvider,
                                       common: CommonAddressYearsService)(implicit ec: ExecutionContext)
  extends Retrievals {

  private def form: Form[Boolean] =
    formProvider("companyAddressYears.error.required")

  def onPageLoad(index: Index, mode: Mode): Action[AnyContent] =
    (authenticate andThen getData andThen requireData()).async { implicit request =>
      (CompanyDetailsId(index) and SchemeNameId).retrieve.map {
        case companyDetails ~ schemeName =>
          common.get(Some(schemeName), companyDetails.companyName, Message("messages__company"), form, AddressYearsId(index))
      }
    }

  def onSubmit(index: Index, mode: Mode): Action[AnyContent] =
    (authenticate andThen getData andThen requireData()).async { implicit request =>
      (CompanyDetailsId(index) and SchemeNameId).retrieve.map {
        case companyDetails ~ schemeName =>
          common.post(Some(schemeName), companyDetails.companyName, Message("messages__company"), form, AddressYearsId(index),Some(mode))
      }
    }
}
