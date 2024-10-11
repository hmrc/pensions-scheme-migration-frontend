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

package controllers.establishers.company.director.address

import connectors.cache.UserAnswersCacheConnector
import controllers.Retrievals
import controllers.actions._
import forms.address.AddressFormProvider
import identifiers.beforeYouStart.SchemeNameId
import identifiers.establishers.company.director.DirectorNameId
import identifiers.establishers.company.director.address.{PreviousAddressId, PreviousAddressListId}
import identifiers.trustees.individual.address.{PreviousAddressId => trusteePreviousAddressId}
import models._
import navigators.CompoundNavigator
import play.api.data.Form
import play.api.data.FormBinding.Implicits.formBinding
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.Results.Redirect
import play.api.mvc.{Action, AnyContent}
import services.DataUpdateService
import services.common.address.CommonManualAddressService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import utils.UserAnswers

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class ConfirmPreviousAddressController @Inject()(
   val messagesApi: MessagesApi,
   userAnswersCacheConnector: UserAnswersCacheConnector,
   navigator: CompoundNavigator,
   authenticate: AuthAction,
   getData: DataRetrievalAction,
   requireData: DataRequiredAction,
   formProvider: AddressFormProvider,
   dataUpdateService: DataUpdateService,
   common: CommonManualAddressService
)(implicit ec: ExecutionContext) extends Retrievals with I18nSupport {

  private val pageTitleEntityTypeMessageKey: Option[String] = Some("messages__director")
  private val pageTitleMessageKey: String = "previousAddress.title"
  private def form: Form[Address] = formProvider()

  def onPageLoad(establisherIndex: Index, directorIndex: Index, mode: Mode): Action[AnyContent] =
    (authenticate andThen getData andThen requireData()).async { implicit request =>
      (DirectorNameId(establisherIndex, directorIndex) and SchemeNameId).retrieve.map {
        case directorName ~ schemeName =>
          common.get(
            Some(schemeName),
            directorName.fullName,
            PreviousAddressId(establisherIndex, directorIndex),
            PreviousAddressListId(establisherIndex, directorIndex),
            AddressConfiguration.PostcodeFirst,
            form,
            pageTitleEntityTypeMessageKey,
            pageTitleMessageKey,
            submitUrl = routes.ConfirmPreviousAddressController.onSubmit(establisherIndex, directorIndex, mode)
          )
      }
    }

  def onSubmit(establisherIndex: Index, directorIndex: Index, mode: Mode): Action[AnyContent] =
    (authenticate andThen getData andThen requireData()).async { implicit request =>
      implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)

      (DirectorNameId(establisherIndex, directorIndex) and SchemeNameId).retrieve.map {
        case directorName ~ schemeName =>
          form
            .bindFromRequest()
            .fold(
              formWithErrors => {
                common.post(
                  Some(schemeName),
                  directorName.fullName,
                  PreviousAddressId(establisherIndex, directorIndex),
                  AddressConfiguration.PostcodeFirst,
                  Some(mode),
                  formWithErrors,
                  pageTitleEntityTypeMessageKey,
                  pageTitleMessageKey,
                  submitUrl = routes.ConfirmPreviousAddressController.onSubmit(establisherIndex, directorIndex, mode)
                )
              },
              value =>
                for {
                  updatedAnswers <- Future.fromTry(setUpdatedAnswers(establisherIndex, directorIndex, mode, value, request.userAnswers))
                  _ <- userAnswersCacheConnector.save(request.lock, updatedAnswers.data)
                } yield {
                  val finalMode = Some(mode).getOrElse(NormalMode)
                  Redirect(navigator.nextPage(PreviousAddressId(establisherIndex, directorIndex), updatedAnswers, finalMode))
                }
            )
      }
    }

  private def setUpdatedAnswers(establisherIndex: Index, directorIndex: Index, mode: Mode, value: Address, ua: UserAnswers): Try[UserAnswers] = {
    val updatedUserAnswers =
    mode match {
      case CheckMode =>
        dataUpdateService.findMatchingTrustee(establisherIndex, directorIndex)(ua).map { trustee =>
          ua.setOrException(trusteePreviousAddressId(trustee.index), value)
        }.getOrElse(ua)
      case _ => ua
    }
    val finalUpdatedUserAnswers = updatedUserAnswers.set(PreviousAddressId(establisherIndex, directorIndex), value)
    finalUpdatedUserAnswers
  }
}
