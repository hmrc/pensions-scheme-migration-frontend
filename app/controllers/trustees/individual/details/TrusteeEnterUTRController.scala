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
import forms.UTRFormProvider
import identifiers.beforeYouStart.SchemeNameId
import identifiers.establishers.company.director.details.DirectorEnterUTRId
import identifiers.trustees.individual.TrusteeNameId
import identifiers.trustees.individual.details.TrusteeUTRId
import models.requests.DataRequest
import models.{CheckMode, Index, Mode, ReferenceValue}
import play.api.data.Form
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.mvc.{Action, AnyContent}
import services.DataUpdateService
import services.common.details.CommonEnterReferenceValueService
import utils.UserAnswers
import viewmodels.Message

import javax.inject.Inject
import scala.concurrent.ExecutionContext
import scala.util.Try

class TrusteeEnterUTRController @Inject()(val messagesApi: MessagesApi,
                                          authenticate: AuthAction,
                                          getData: DataRetrievalAction,
                                          requireData: DataRequiredAction,
                                          formProvider: UTRFormProvider,
                                          dataUpdateService: DataUpdateService,
                                          common: CommonEnterReferenceValueService
                                         )(implicit val executionContext: ExecutionContext)
  extends Retrievals with I18nSupport {

  private def name(index: Index)
                  (implicit request: DataRequest[AnyContent]): String =
    request
      .userAnswers
      .get(TrusteeNameId(index))
      .fold(Message("messages__trustee"))(_.fullName)

  private def form: Form[ReferenceValue] = formProvider()

  def onPageLoad(index: Index, mode: Mode): Action[AnyContent] =
    (authenticate andThen getData andThen requireData()).async {
      implicit request =>
        SchemeNameId.retrieve.map {
          schemeName =>
            common.get(
              pageTitle     = Message("messages__enterUTR", Message("messages__individual")),
              pageHeading     = Message("messages__enterUTR", name(index)),
              isPageHeading = true,
              id            = TrusteeUTRId(index),
              form          = form,
              schemeName    = schemeName,
              legendClass   = "govuk-visually-hidden",
              paragraphText = Seq(Message("messages__UTR__p1"), Messages("messages__UTR__p2"))
            )
        }
    }

  def onSubmit(index: Index, mode: Mode): Action[AnyContent] =
    (authenticate andThen getData andThen requireData()).async {
      implicit request =>
        SchemeNameId.retrieve.map {
          schemeName =>
            common.post(
              pageTitle = Message("messages__enterUTR", Message("messages__individual")),
              pageHeading = Message("messages__enterUTR", name(index)),
              isPageHeading = true,
              id = TrusteeUTRId(index),
              form = form,
              schemeName = schemeName,
              hintText = None,
              paragraphText = Seq(Message("messages__UTR__p1"), Message("messages__UTR__p2")),
              legendClass   = "govuk-visually-hidden",
              mode = mode,
              optSetUserAnswers = Some(value => setUpdatedAnswers(index, mode, value, request.userAnswers))
            )
        }
    }

  private def setUpdatedAnswers(index: Index, mode: Mode, value: ReferenceValue, ua: UserAnswers): Try[UserAnswers] = {
    val updatedUserAnswers =
      mode match {
        case CheckMode =>
          val directors = dataUpdateService.findMatchingDirectors(index)(ua)
          directors.foldLeft[UserAnswers](ua) { (acc, director) =>
            if (director.isDeleted)
              acc
            else
              acc.setOrException(DirectorEnterUTRId(director.mainIndex.get, director.index), value)
          }
        case _ => ua
      }
    val finalUpdatedUserAnswers = updatedUserAnswers.set(TrusteeUTRId(index), value)
    finalUpdatedUserAnswers
  }
}