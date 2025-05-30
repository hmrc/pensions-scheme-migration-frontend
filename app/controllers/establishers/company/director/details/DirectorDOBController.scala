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
import forms.DOBFormProvider
import identifiers.beforeYouStart.SchemeNameId
import identifiers.establishers.company.director.DirectorNameId
import identifiers.establishers.company.director.details.DirectorDOBId
import identifiers.trustees.individual.details.TrusteeDOBId
import models.{CheckMode, Index, Mode}
import play.api.data.Form
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.mvc.{Action, AnyContent}
import services.DataUpdateService
import services.common.details.CommonDateOfBirthService
import utils.UserAnswers

import java.time.LocalDate
import javax.inject.Inject
import scala.concurrent.ExecutionContext
import scala.util.Try

class DirectorDOBController @Inject()(val messagesApi: MessagesApi,
                                      authenticate: AuthAction,
                                      getData: DataRetrievalAction,
                                      requireData: DataRequiredAction,
                                      formProvider: DOBFormProvider,
                                      dataUpdateService: DataUpdateService,
                                      common: CommonDateOfBirthService
                                     )(implicit val executionContext: ExecutionContext)
  extends Retrievals with I18nSupport {

  val form: Form[LocalDate] = formProvider()

  def onPageLoad(establisherIndex: Index, directorIndex: Index, mode: Mode): Action[AnyContent] =
    (authenticate andThen getData andThen requireData()).async {
      implicit request =>

        SchemeNameId.retrieve.map {
          schemeName =>
            common.get(form,
              dobId = DirectorDOBId(establisherIndex, directorIndex),
              personNameId = DirectorNameId(establisherIndex, directorIndex),
              schemeName = schemeName,
              entityType = Messages("messages__director"),
              call = routes.DirectorDOBController.onSubmit(establisherIndex: Index, directorIndex: Index, mode: Mode)
            )
        }
    }

  def onSubmit(establisherIndex: Index, directorIndex: Index, mode: Mode): Action[AnyContent] =
    (authenticate andThen getData andThen requireData()).async {
      implicit request =>
        retrieve(SchemeNameId) {
          schemeName =>
            common.post(
              form,
              dobId = DirectorDOBId(establisherIndex, directorIndex),
              personNameId = DirectorNameId(establisherIndex, directorIndex),
              schemeName = schemeName,
              entityType = Messages("messages__director"),
              mode = mode,
              optSetUserAnswers = Some(value => setUpdatedAnswers(establisherIndex, directorIndex, mode, value, request.userAnswers)),
              call = routes.DirectorDOBController.onSubmit(establisherIndex, directorIndex, mode)
            )
        }
    }

  private def setUpdatedAnswers(establisherIndex: Index, directorIndex: Index, mode: Mode, value: LocalDate, ua: UserAnswers): Try[UserAnswers] = {
    val updatedUserAnswers =
    mode match {
      case CheckMode =>
        dataUpdateService.findMatchingTrustee(establisherIndex, directorIndex)(ua).map { trustee =>
          ua.setOrException(TrusteeDOBId(trustee.index), value)
        }.getOrElse(ua)
      case _ => ua
    }
    val finalUpdatedUserAnswers = updatedUserAnswers.set(DirectorDOBId(establisherIndex, directorIndex), value)
    finalUpdatedUserAnswers
  }
}
