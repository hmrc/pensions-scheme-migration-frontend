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
import forms.UTRFormProvider
import identifiers.beforeYouStart.SchemeNameId
import identifiers.establishers.company.director.DirectorNameId
import identifiers.establishers.company.director.details.DirectorEnterUTRId
import identifiers.trustees.individual.details.TrusteeUTRId
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

class DirectorEnterUTRController @Inject()(val messagesApi: MessagesApi,
                                           authenticate: AuthAction,
                                           getData: DataRetrievalAction,
                                           requireData: DataRequiredAction,
                                           formProvider: UTRFormProvider,
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
              pageTitle = Messages("messages__enterUTR", Messages("messages__director")),
              pageHeading = Messages("messages__enterUTR", name(establisherIndex, directorIndex)),
              isPageHeading = true,
              id = DirectorEnterUTRId(establisherIndex, directorIndex),
              form = form,
              schemeName = schemeName,
              legendClass = "govuk-visually-hidden",
              paragraphText = Seq(Messages("messages__UTR__p1"), Messages("messages__UTR__p2")),
              submitCall = routes.DirectorEnterUTRController.onSubmit(establisherIndex, directorIndex, mode)
            )
        }
    }

  private def name(establisherIndex: Index, directorIndex: Index)
                  (implicit request: DataRequest[AnyContent]): String =
    request
      .userAnswers
      .get(DirectorNameId(establisherIndex, directorIndex))
      .fold("the director")(_.fullName)

  private def form: Form[ReferenceValue] = formProvider()

  def onSubmit(establisherIndex: Index, directorIndex: Index, mode: Mode): Action[AnyContent] =
    (authenticate andThen getData andThen requireData()).async {
      implicit request =>
        SchemeNameId.retrieve.map {
          schemeName =>
            common.post(
              pageTitle     = Messages("messages__enterUTR", Messages("messages__director")),
              pageHeading     = Messages("messages__enterUTR", name(establisherIndex, directorIndex)),
              isPageHeading = true,
              id            = DirectorEnterUTRId(establisherIndex, directorIndex),
              form          = form,
              schemeName    = schemeName,
              hintText      = None,
              paragraphText = Seq(Messages("messages__UTR__p1"), Messages("messages__UTR__p2")),
              legendClass = "govuk-visually-hidden",
              mode          = mode,
              optSetUserAnswers = Some(value => setUpdatedAnswers(establisherIndex, directorIndex, mode, value, request.userAnswers)),
              submitCall = routes.DirectorEnterUTRController.onSubmit(establisherIndex, directorIndex, mode)
            )
        }
    }

  private def setUpdatedAnswers(establisherIndex: Index, directorIndex: Index, mode: Mode, value: ReferenceValue, ua: UserAnswers): Try[UserAnswers] = {
    val updatedUserAnswers =
      mode match {
        case CheckMode =>
          dataUpdateService.findMatchingTrustee(establisherIndex, directorIndex)(ua).map { trustee =>
            ua.setOrException(TrusteeUTRId(trustee.index), value)
          }.getOrElse(ua)
        case _ => ua
    }
    val finalUpdatedUserAnswers = updatedUserAnswers.set(DirectorEnterUTRId(establisherIndex, directorIndex), value)
    finalUpdatedUserAnswers
  }
}
