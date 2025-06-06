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

package controllers.establishers.individual.details

import controllers.Retrievals
import controllers.actions.{AuthAction, DataRequiredAction, DataRetrievalAction}
import forms.HasReferenceNumberFormProvider
import identifiers.beforeYouStart.SchemeNameId
import identifiers.establishers.individual.EstablisherNameId
import identifiers.establishers.individual.details.EstablisherHasUTRId
import models.requests.DataRequest
import models.{Index, Mode}
import play.api.data.Form
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.mvc.{Action, AnyContent}
import services.common.details.CommonHasReferenceValueService

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class EstablisherHasUTRController @Inject()(val messagesApi: MessagesApi,
                                            authenticate: AuthAction,
                                            getData: DataRetrievalAction,
                                            requireData: DataRequiredAction,
                                            formProvider: HasReferenceNumberFormProvider,
                                            common: CommonHasReferenceValueService
                                           )(implicit val executionContext: ExecutionContext)
  extends Retrievals with I18nSupport {

  private def name(index: Index)
                  (implicit request: DataRequest[AnyContent]): String =
    request
      .userAnswers
      .get(EstablisherNameId(index))
      .fold("the establisher")(_.fullName)

  private def form(index: Index)
                  (implicit request: DataRequest[AnyContent]): Form[Boolean] =
    formProvider(
      errorMsg = Messages("messages__genericHasUtr__error__required", name(index))
    )

  def onPageLoad(index: Index, mode: Mode): Action[AnyContent] =
    (authenticate andThen getData andThen requireData()).async {
      implicit request =>

        SchemeNameId.retrieve.map {
          schemeName =>
            common.get(
              pageTitle     = Messages("messages__hasUTR", Messages("messages__individual")),
              pageHeading     = Messages("messages__hasUTR", name(index)),
              isPageHeading = true,
              id            = EstablisherHasUTRId(index),
              form          = form(index),
              schemeName    = schemeName,
              paragraphText = Seq(Messages("messages__UTR__p1"), Messages("messages__UTR__p2")),
              legendClass   = "govuk-visually-hidden",
              submitCall    = routes.EstablisherHasUTRController.onSubmit(index, mode)
            )
        }
    }

  def onSubmit(index: Index, mode: Mode): Action[AnyContent] =
    (authenticate andThen getData andThen requireData()).async {
      implicit request =>

        SchemeNameId.retrieve.map {
          schemeName =>
            common.post(
              pageTitle     = Messages("messages__hasUTR", Messages("messages__individual")),
              pageHeading     = Messages("messages__hasUTR", name(index)),
              isPageHeading = true,
              id            = EstablisherHasUTRId(index),
              form          = form(index),
              schemeName    = schemeName,
              paragraphText = Seq(Messages("messages__UTR__p1"), Messages("messages__UTR__p2")),
              legendClass   = "govuk-visually-hidden",
              mode          = mode,
              submitCall    = routes.EstablisherHasUTRController.onSubmit(index, mode)
            )
        }
    }
}
