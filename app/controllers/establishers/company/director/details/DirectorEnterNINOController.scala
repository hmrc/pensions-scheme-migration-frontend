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

package controllers.establishers.company.director.details

import controllers.Retrievals
import controllers.actions._
import forms.NINOFormProvider
import identifiers.beforeYouStart.SchemeNameId
import identifiers.establishers.company.director.DirectorNameId
import identifiers.establishers.company.director.details.DirectorNINOId
import identifiers.trustees.individual.details.TrusteeNINOId
import models.requests.DataRequest
import models.{CheckMode, Index, Mode, ReferenceValue}
import play.api.data.Form
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.mvc.{Action, AnyContent}
import services.DataUpdateService
import services.common.details.CommonEnterReferenceValueService
import utils.UserAnswers

import javax.inject.Inject
import scala.concurrent.ExecutionContext
import scala.util.Try

class DirectorEnterNINOController @Inject()( val messagesApi: MessagesApi,
                                             authenticate: AuthAction,
                                             getData: DataRetrievalAction,
                                             requireData: DataRequiredAction,
                                             formProvider: NINOFormProvider,
                                             dataUpdateService: DataUpdateService,
                                             common: CommonEnterReferenceValueService
                                           )(implicit val executionContext: ExecutionContext)
  extends Retrievals with I18nSupport {

  def onPageLoad(establisherIndex: Index, directorIndex: Index, mode: Mode): Action[AnyContent] =
    (authenticate andThen getData andThen requireData()).async {
      implicit request =>
        SchemeNameId.retrieve.map {
          schemeName =>
            common.get(
              pageTitle = Messages("messages__enterNINO_title", Messages("messages__director")),
              pageHeading = Messages("messages__enterNINO_title", name(establisherIndex, directorIndex)),
              isPageHeading = true,
              id = DirectorNINOId(establisherIndex, directorIndex),
              form = form(establisherIndex, directorIndex),
              schemeName = schemeName,
              hintText = Some(Messages("messages__enterNINO__hint")),
              legendClass = "govuk-label--xl",
              submitCall = routes.DirectorEnterNINOController.onSubmit(establisherIndex, directorIndex, mode)
            )
        }
    }

  private def form(establisherIndex: Index, directorIndex: Index)
                  (implicit request: DataRequest[AnyContent]): Form[ReferenceValue] =
    formProvider(name(establisherIndex, directorIndex))

  private def name(establisherIndex: Index, directorIndex: Index)
                  (implicit request: DataRequest[AnyContent]): String =
    request
      .userAnswers
      .get(DirectorNameId(establisherIndex, directorIndex))
      .fold(Messages("messages__director"))(_.fullName)

  def onSubmit(establisherIndex: Index, directorIndex: Index, mode: Mode): Action[AnyContent] =
    (authenticate andThen getData andThen requireData()).async {
      implicit request =>
        retrieve(SchemeNameId) {
          schemeName =>
            common.post(
              pageTitle     = Messages("messages__enterNINO_title", Messages("messages__director")),
              pageHeading     = Messages("messages__enterNINO_title", name(establisherIndex, directorIndex)),
              isPageHeading = true,
              id            = DirectorNINOId(establisherIndex, directorIndex),
              form          = form(establisherIndex,directorIndex),
              schemeName    = schemeName,
              hintText      = Some(Messages("messages__enterNINO__hint")),
              legendClass   = "govuk-label--xl",
              mode          = mode,
              optSetUserAnswers = Some(value => setUpdatedAnswers(establisherIndex, directorIndex, mode, value, request.userAnswers)),
              submitCall = routes.DirectorEnterNINOController.onSubmit(establisherIndex, directorIndex, mode)
            )
        }
    }

  private def setUpdatedAnswers(establisherIndex: Index, directorIndex: Index, mode: Mode, value: ReferenceValue, ua: UserAnswers): Try[UserAnswers] = {
    val updatedUserAnswers =
      mode match {
        case CheckMode =>
          dataUpdateService.findMatchingTrustee(establisherIndex, directorIndex)(ua).map { trustee =>
            ua.setOrException(TrusteeNINOId(trustee.index), value)
          }.getOrElse(ua)
        case _ => ua
    }
    val finalUpdatedUserAnswers = updatedUserAnswers.set(DirectorNINOId(establisherIndex, directorIndex), value)
    finalUpdatedUserAnswers
  }
}
