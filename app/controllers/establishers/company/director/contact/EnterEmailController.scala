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

package controllers.establishers.company.director.contact

import controllers.Retrievals
import controllers.actions._
import forms.EmailFormProvider
import identifiers.beforeYouStart.SchemeNameId
import identifiers.establishers.company.director.DirectorNameId
import identifiers.establishers.company.director.contact.EnterEmailId
import identifiers.trustees.individual.contact.{EnterEmailId => trusteeEnterEmailId}
import models.requests.DataRequest
import models.{CheckMode, Index, Mode}
import play.api.data.Form
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.mvc.{Action, AnyContent}
import services.DataUpdateService
import services.common.contact.CommonEmailAddressService
import utils.UserAnswers

import javax.inject.Inject
import scala.concurrent.ExecutionContext
import scala.util.Try

class EnterEmailController @Inject()(
                                      val messagesApi: MessagesApi,
                                      authenticate: AuthAction,
                                      getData: DataRetrievalAction,
                                      requireData: DataRequiredAction,
                                      formProvider: EmailFormProvider,
                                      dataUpdateService: DataUpdateService,
                                      common: CommonEmailAddressService
                                    )(implicit val executionContext: ExecutionContext)
  extends Retrievals with I18nSupport {

  def onPageLoad(establisherIndex: Index, directorIndex: Index, mode: Mode): Action[AnyContent] =
    (authenticate andThen getData andThen requireData()).async {
      implicit request =>
        SchemeNameId.retrieve.map {
          schemeName =>
            common.get(
              entityName = name(establisherIndex, directorIndex),
              entityType = Messages("messages__director"),
              emailId = EnterEmailId(establisherIndex, directorIndex),
              form = form(establisherIndex, directorIndex),
              schemeName = schemeName,
              paragraphText = Seq(Messages("messages__contact_details__hint", name(establisherIndex, directorIndex))),
              routes.EnterEmailController.onSubmit(establisherIndex, directorIndex, mode)
            )
        }
    }

  def onSubmit(establisherIndex: Index, directorIndex: Index, mode: Mode): Action[AnyContent] =
    (authenticate andThen getData andThen requireData()).async {
      implicit request =>
        SchemeNameId.retrieve.map {
          schemeName =>
            common.post(
              entityName = name(establisherIndex, directorIndex),
              entityType = Messages("messages__director"),
              emailId = EnterEmailId(establisherIndex, directorIndex),
              form = form(establisherIndex, directorIndex),
              schemeName = schemeName,
              paragraphText = Seq(Messages("messages__contact_details__hint", name(establisherIndex, directorIndex))),
              mode = Some(mode),
              routes.EnterEmailController.onSubmit(establisherIndex, directorIndex, mode),
              Some(value => setUpdatedAnswers(establisherIndex, directorIndex, mode, value, request.userAnswers))
            )
        }
    }

  private def form(establisherIndex: Index, directorIndex: Index)
                  (implicit request: DataRequest[AnyContent]): Form[String] =
    formProvider(Messages("messages__enterEmail__error_required", name(establisherIndex, directorIndex)))

  private def name(establisherIndex: Index, directorIndex: Index)
                  (implicit request: DataRequest[AnyContent]): String =
    request
      .userAnswers
      .get(DirectorNameId(establisherIndex, directorIndex))
      .fold("the director")(_.fullName)

  private def setUpdatedAnswers(establisherIndex: Index, directorIndex: Index, mode: Mode, value: String, ua: UserAnswers): Try[UserAnswers] = {
    val updatedUserAnswers =
      mode match {
        case CheckMode =>
          dataUpdateService.findMatchingTrustee(establisherIndex, directorIndex)(ua).map { trustee =>
            ua.setOrException(trusteeEnterEmailId(trustee.index), value)
          }.getOrElse(ua)
        case _ => ua
      }
    val finalUpdatedUserAnswers = updatedUserAnswers.set(EnterEmailId(establisherIndex, directorIndex), value)
    finalUpdatedUserAnswers
  }
}
