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

package controllers.trustees

import controllers.Retrievals
import controllers.actions._
import forms.HasReferenceNumberFormProvider
import identifiers.beforeYouStart.SchemeNameId
import identifiers.trustees.OtherTrusteesId
import models.NormalMode
import models.requests.DataRequest
import play.api.data.Form
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.mvc.{Action, AnyContent}
import services.common.details.CommonHasReferenceValueService

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class OtherTrusteesController @Inject()(val messagesApi: MessagesApi,
                                        authenticate: AuthAction,
                                        getData: DataRetrievalAction,
                                        requireData: DataRequiredAction,
                                        formProvider: HasReferenceNumberFormProvider,
                                        common: CommonHasReferenceValueService
                                       )(implicit val executionContext: ExecutionContext)
  extends Retrievals with I18nSupport {

  private def form()(implicit request: DataRequest[AnyContent]): Form[Boolean] =
    formProvider(errorMsg = Messages("messages__otherTrustees__error__required"))

  def onPageLoad: Action[AnyContent] =
    (authenticate andThen getData andThen requireData()).async {
      implicit request =>
        SchemeNameId.retrieve.map {
          schemeName =>
            common.get(
              pageTitle = Messages("messages__otherTrustees__title"),
              pageHeading = Messages("messages__otherTrustees__heading"),
              isPageHeading = true,
              id = OtherTrusteesId,
              form = form(),
              schemeName = schemeName,
              paragraphText = Seq(Messages("messages__otherTrustees__lede")),
              legendClass = "govuk-visually-hidden",
              submitCall = routes.OtherTrusteesController.onSubmit
            )
        }
    }

  def onSubmit: Action[AnyContent] =
    (authenticate andThen getData andThen requireData()).async {
      implicit request =>
        SchemeNameId.retrieve.map {
          schemeName =>
            common.post(
              pageTitle = Messages("messages__otherTrustees__title"),
              pageHeading = Messages("messages__otherTrustees__heading"),
              isPageHeading = true,
              id = OtherTrusteesId,
              form = form(),
              schemeName = schemeName,
              paragraphText = Seq(Messages("messages__otherTrustees__lede")),
              legendClass = "govuk-visually-hidden",
              mode = NormalMode,
              submitCall = routes.OtherTrusteesController.onSubmit
            )
        }
    }

}
