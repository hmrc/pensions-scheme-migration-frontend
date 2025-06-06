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

package controllers.trustees.company.details

import controllers.Retrievals
import controllers.actions.{AuthAction, DataRequiredAction, DataRetrievalAction}
import forms.HasReferenceNumberFormProvider
import identifiers.beforeYouStart.SchemeNameId
import identifiers.trustees.company.CompanyDetailsId
import identifiers.trustees.company.details.HaveCompanyNumberId
import models.requests.DataRequest
import models.{Index, Mode}
import play.api.data.Form
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.mvc.{Action, AnyContent}
import services.common.details.CommonHasReferenceValueService

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class HaveCompanyNumberController @Inject()(val messagesApi: MessagesApi,
                                            authenticate: AuthAction,
                                            getData: DataRetrievalAction,
                                            requireData: DataRequiredAction,
                                            formProvider: HasReferenceNumberFormProvider,
                                            common: CommonHasReferenceValueService
                                           )(implicit val executionContext: ExecutionContext)
  extends Retrievals with I18nSupport {

  private def name(index: Index)
                  (implicit request: DataRequest[AnyContent]): String =
    request.userAnswers.get(CompanyDetailsId(index)).fold(Messages("messages__company"))(_.companyName)

  private def form(index: Index)
                  (implicit request: DataRequest[AnyContent]): Form[Boolean] =
    formProvider(errorMsg = Messages("messages__haveCompanyNumber__error", name(index)))

  def onPageLoad(index: Index, mode: Mode): Action[AnyContent] =
    (authenticate andThen getData andThen requireData()).async {
      implicit request =>

        SchemeNameId.retrieve.map {
          schemeName =>
            common.get(
              pageTitle     = Messages("messages__haveCompanyNumber", Messages("messages__company")),
              pageHeading   = Messages("messages__haveCompanyNumber", name(index)),
              isPageHeading = true,
              id            = HaveCompanyNumberId(index),
              form          = form(index),
              schemeName    = schemeName,
              legendClass   = "govuk-label--l",
              paragraphText = Seq(Messages("messages__haveCompanyNumber__p")),
              submitCall    = routes.HaveCompanyNumberController.onSubmit(index, mode)
            )
        }
    }

  def onSubmit(index: Index, mode: Mode): Action[AnyContent] =
    (authenticate andThen getData andThen requireData()).async {
      implicit request =>
        SchemeNameId.retrieve.map {
          schemeName =>
            common.post(
              pageTitle     = Messages("messages__haveCompanyNumber", Messages("messages__company")),
              pageHeading   = Messages("messages__haveCompanyNumber", name(index)),
              isPageHeading = true,
              id            = HaveCompanyNumberId(index),
              form          = form(index),
              schemeName    = schemeName,
              legendClass   = "govuk-label--l",
              mode          = mode,
              submitCall    = routes.HaveCompanyNumberController.onSubmit(index, mode),
              paragraphText = Seq(Messages("messages__haveCompanyNumber__p"))
            )
        }
    }
}
