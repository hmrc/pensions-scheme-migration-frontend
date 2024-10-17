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

package controllers.trustees.individual.details

import controllers.Retrievals
import controllers.actions.{AuthAction, DataRequiredAction, DataRetrievalAction}
import forms.HasReferenceNumberFormProvider
import identifiers.beforeYouStart.SchemeNameId
import identifiers.establishers.company.director.details.DirectorHasNINOId
import identifiers.trustees.individual.TrusteeNameId
import identifiers.trustees.individual.details.TrusteeHasNINOId
import models.requests.DataRequest
import models.{CheckMode, Index, Mode}
import play.api.data.Form
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.mvc.{Action, AnyContent}
import services.DataUpdateService
import services.common.details.CommonHasReferenceValueService
import utils.UserAnswers

import javax.inject.Inject
import scala.concurrent.ExecutionContext
import scala.util.Try

class TrusteeHasNINOController @Inject()(val messagesApi: MessagesApi,
                                         authenticate: AuthAction,
                                         getData: DataRetrievalAction,
                                         requireData: DataRequiredAction,
                                         formProvider: HasReferenceNumberFormProvider,
                                         dataUpdateService: DataUpdateService,
                                         common: CommonHasReferenceValueService
                                        )(implicit val executionContext: ExecutionContext)
  extends Retrievals with I18nSupport {

  private def name(index: Index)
                  (implicit request: DataRequest[AnyContent]): String =
    request
      .userAnswers
      .get(TrusteeNameId(index))
      .fold(Messages("messages__trustee"))(_.fullName)

  private def form(index: Index)
                  (implicit request: DataRequest[AnyContent]): Form[Boolean] = {
    formProvider(
      errorMsg = Messages("messages__genericHasNino__error__required", name(index))
    )
  }

  def onPageLoad(index: Index, mode: Mode): Action[AnyContent] =
    (authenticate andThen getData andThen requireData()).async {
      implicit request =>

        SchemeNameId.retrieve.map {
          schemeName =>
            common.get(
              pageTitle     = Messages("messages__hasNINO", Messages("messages__individual")),
              pageHeading     = Messages("messages__hasNINO", name(index)),
              isPageHeading = true,
              id            = TrusteeHasNINOId(index),
              form          = form(index),
              schemeName    = schemeName,
              legendClass   = "govuk-label--xl",
              submitCall    = routes.TrusteeHasNINOController.onSubmit(index, mode)
            )
        }
    }

  def onSubmit(index: Index, mode: Mode): Action[AnyContent] =
    (authenticate andThen getData andThen requireData()).async {
      implicit request =>
        SchemeNameId.retrieve.map {
          schemeName =>
            common.post(
              pageTitle = Messages("messages__hasNINO", Messages("messages__individual")),
              pageHeading = Messages("messages__hasNINO", name(index)),
              isPageHeading = true,
              id = TrusteeHasNINOId(index),
              form = form(index),
              schemeName = schemeName,
              legendClass = "govuk-label--xl",
              mode = mode,
              submitCall    = routes.TrusteeHasNINOController.onSubmit(index, mode),
              optSetUserAnswers = Some(value => setUpdatedAnswers(index, mode, value, request.userAnswers))
            )
        }
    }

  private def setUpdatedAnswers(index: Index, mode: Mode, value: Boolean, ua: UserAnswers): Try[UserAnswers] = {
    val updatedUserAnswers =
      mode match {
        case CheckMode =>
          val directors = dataUpdateService.findMatchingDirectors(index)(ua)
          directors.foldLeft[UserAnswers](ua) { (acc, director) =>
            if (director.isDeleted)
              acc
            else
              acc.setOrException(DirectorHasNINOId(director.mainIndex.get, director.index), value)
          }
        case _ => ua
      }
    val finalUpdatedUserAnswers = updatedUserAnswers.set(TrusteeHasNINOId(index), value)
    finalUpdatedUserAnswers
  }
}
