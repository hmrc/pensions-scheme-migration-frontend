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
import forms.HasReferenceNumberFormProvider
import identifiers.beforeYouStart.SchemeNameId
import identifiers.establishers.company.director.DirectorNameId
import identifiers.establishers.company.director.details.DirectorHasNINOId
import identifiers.trustees.individual.details.TrusteeHasNINOId
import models.requests.DataRequest
import models.{CheckMode, Index, Mode}
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent}
import services.DataUpdateService
import services.common.details.CommonHasReferenceValueService
import utils.UserAnswers
import viewmodels.Message

import javax.inject.Inject
import scala.concurrent.ExecutionContext
import scala.util.Try

class DirectorHasNINOController @Inject()(val messagesApi: MessagesApi,
                                          authenticate: AuthAction,
                                          getData: DataRetrievalAction,
                                          requireData: DataRequiredAction,
                                          formProvider: HasReferenceNumberFormProvider,
                                          dataUpdateService: DataUpdateService,
                                          common: CommonHasReferenceValueService
                                         )(implicit val executionContext: ExecutionContext)
  extends Retrievals with I18nSupport {

  def onPageLoad(establisherIndex: Index, directorIndex: Index, mode: Mode): Action[AnyContent] =
    (authenticate andThen getData andThen requireData()).async {
      implicit request =>

        SchemeNameId.retrieve.map {
          schemeName =>
            common.get(
              pageTitle = Message("messages__hasNINO", Message("messages__director")),
              pageHeading = Message("messages__hasNINO", name(establisherIndex, directorIndex)),
              isPageHeading = true,
              id = DirectorHasNINOId(establisherIndex, directorIndex),
              form = form(establisherIndex, directorIndex),
              schemeName = schemeName,
              legendClass = "govuk-label--xl"
            )
        }
    }

  private def name(establisherIndex: Index, directorIndex: Index)
                  (implicit request: DataRequest[AnyContent]): String =
    request
      .userAnswers
      .get(DirectorNameId(establisherIndex, directorIndex))
      .fold(Message("messages__director"))(_.fullName)

  private def form(establisherIndex: Index, directorIndex: Index)
                  (implicit request: DataRequest[AnyContent]): Form[Boolean] = {
    formProvider(
      errorMsg = Message("messages__genericHasNino__error__required", name(establisherIndex, directorIndex))
    )
  }

  def onSubmit(establisherIndex: Index, directorIndex: Index, mode: Mode): Action[AnyContent] =
    (authenticate andThen getData andThen requireData()).async {
      implicit request =>
        SchemeNameId.retrieve.map {
          schemeName =>
            common.post(
              pageTitle = Message("messages__hasNINO", Message("messages__director")),
              pageHeading = Message("messages__hasNINO", name(establisherIndex, directorIndex)),
              isPageHeading = true,
              id = DirectorHasNINOId(establisherIndex, directorIndex),
              form = form(establisherIndex, directorIndex),
              schemeName = schemeName,
              paragraphText = Seq(),
              legendClass = "govuk-label--xl",
              mode = mode,
              optSetUserAnswers = Some(value => setUpdatedAnswers(establisherIndex, directorIndex, mode, value, request.userAnswers))
            )
        }
    }

  private def setUpdatedAnswers(establisherIndex: Index, directorIndex: Index, mode: Mode, value: Boolean, ua: UserAnswers): Try[UserAnswers] = {
    val updatedUserAnswers =
      mode match {
        case CheckMode =>
          dataUpdateService.findMatchingTrustee(establisherIndex, directorIndex)(ua).map { trustee =>
            ua.setOrException(TrusteeHasNINOId(trustee.index), value)
          }.getOrElse(ua)
        case _ => ua
    }
    val finalUpdatedUserAnswers = updatedUserAnswers.set(DirectorHasNINOId(establisherIndex, directorIndex), value)
    finalUpdatedUserAnswers
  }
}
