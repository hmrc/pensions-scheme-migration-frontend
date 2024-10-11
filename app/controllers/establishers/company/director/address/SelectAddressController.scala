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
import controllers.actions._
import models.establishers.AddressPages
import forms.address.AddressListFormProvider
import identifiers.beforeYouStart.SchemeNameId
import identifiers.establishers.company.director.DirectorNameId
import identifiers.establishers.company.director.address.{AddressId, AddressListId, EnterPostCodeId}
import identifiers.trustees.individual.{address => trusteeAddress}
import models._
import navigators.CompoundNavigator
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent}
import services.DataUpdateService
import controllers.Retrievals
import play.api.data.FormBinding.Implicits.formBinding
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import utils.UserAnswers
import play.api.mvc.Results.Redirect
import services.common.address.{CommonAddressListService, CommonAddressListTemplateData}
import viewmodels.Message

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class SelectAddressController @Inject()(
    val messagesApi: MessagesApi,
    userAnswersCacheConnector: UserAnswersCacheConnector,
    navigator: CompoundNavigator,
    authenticate: AuthAction,
    getData: DataRetrievalAction,
    requireData: DataRequiredAction,
    formProvider: AddressListFormProvider,
    dataUpdateService: DataUpdateService,
    common:CommonAddressListService
 )(implicit val ec: ExecutionContext) extends I18nSupport with Retrievals {

  private def form: Form[Int] = formProvider("selectAddress.required")

  def onPageLoad(establisherIndex: Index, directorIndex: Index, mode: Mode): Action[AnyContent] =
    (authenticate andThen getData andThen requireData()).async { implicit request =>
      retrieve(SchemeNameId) { schemeName =>
        getFormToTemplate(schemeName, establisherIndex, directorIndex, mode).retrieve.map(formToTemplate =>
          common.get(
            formToTemplate(form),
            form,
            submitUrl = routes.SelectAddressController.onSubmit(establisherIndex, directorIndex, mode)
          )
        )
      }
    }

  def getFormToTemplate(schemeName: String, establisherIndex: Index, directorIndex: Index, mode: Mode): Retrieval[Form[Int] => CommonAddressListTemplateData] =
    Retrieval(
      implicit request =>
        EnterPostCodeId(establisherIndex, directorIndex).retrieve.map { addresses =>
          val name: String = request.userAnswers.get(DirectorNameId(establisherIndex, directorIndex))
            .map(_.fullName).getOrElse(Message("messages__director"))

          form =>
            CommonAddressListTemplateData(
              form,
              addresses,
              Message("messages__director"),
              name,
              routes.ConfirmAddressController.onPageLoad(establisherIndex, directorIndex, mode).url,
              schemeName,
              h1MessageKey = "addressList.title"
            )
        }
    )

  def onSubmit(establisherIndex: Index, directorIndex: Index, mode: Mode): Action[AnyContent] =
    (authenticate andThen getData andThen requireData()).async { implicit request =>
      val addressPages: AddressPages = AddressPages(EnterPostCodeId(establisherIndex, directorIndex),
        AddressListId(establisherIndex, directorIndex), AddressId(establisherIndex, directorIndex))
      implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)

      retrieve(SchemeNameId) { schemeName =>
        val formToTempalte: Form[Int] => CommonAddressListTemplateData =
          getFormToTemplate(schemeName, establisherIndex, directorIndex, mode).retrieve.toOption.get
        form.bindFromRequest().fold(
          formWithErrors =>
            common.post(
              formToTempalte,
              addressPages,
              Some(mode),
              manualUrlCall = routes.SelectAddressController.onPageLoad(establisherIndex, directorIndex, mode),
              formWithErrors,
              submitUrl = routes.SelectAddressController.onSubmit(establisherIndex, directorIndex, mode)
            ),
          value =>
            addressPages.postcodeId.retrieve.map { addresses =>
              val address = addresses(value).copy(country = Some("GB"))
              if (address.toAddress.nonEmpty) {
                for {
                  updatedAnswers <- Future.fromTry(
                    setUpdatedAnswersForUkAddr(establisherIndex, directorIndex, mode, addressPages, address, request.userAnswers)
                  )
                  _ <- userAnswersCacheConnector.save(request.lock, updatedAnswers.data)
                } yield {
                  val finalMode = Some(mode).getOrElse(NormalMode)
                  Redirect(navigator.nextPage(addressPages.addressListPage, updatedAnswers, finalMode))
                }
              } else {
                for {
                  updatedAnswers <- Future.fromTry(
                    setUpdatedAnswersForNonUkAddr(establisherIndex, directorIndex, mode, addressPages, address, request.userAnswers)
                    )
                  _ <- userAnswersCacheConnector.save(request.lock, updatedAnswers.data)
                } yield {
                  Redirect(routes.ConfirmAddressController.onPageLoad(establisherIndex, directorIndex, mode))
                }

              }
            }
        )
      }
    }

  private def setUpdatedAnswersForUkAddr(establisherIndex: Index, directorIndex: Index,
                                         mode: Mode, addressPages: AddressPages,
                                         address: TolerantAddress, ua: UserAnswers): Try[UserAnswers] = {
    val updatedUserAnswers =
      mode match {
        case CheckMode =>
          dataUpdateService.findMatchingTrustee(establisherIndex, directorIndex)(ua).map { trustee =>
            val trusteeAddressPages: AddressPages = AddressPages(trusteeAddress.EnterPostCodeId(trustee.index),
              trusteeAddress.AddressListId(trustee.index), trusteeAddress.AddressId(trustee.index))
            ua.remove(trusteeAddressPages.addressListPage).setOrException(trusteeAddressPages.addressPage,
              address.toAddress.get)
          }.getOrElse(ua)
        case _ => ua
      }
    val finalUpdatedUserAnswers = updatedUserAnswers.remove(addressPages.addressListPage)
      .set(addressPages.addressPage, address.toAddress.get)
    finalUpdatedUserAnswers
  }

  private def setUpdatedAnswersForNonUkAddr(establisherIndex: Index,
                                            directorIndex: Index,
                                            mode: Mode,
                                            addressPages: AddressPages,
                                            address: TolerantAddress,
                                            ua: UserAnswers): Try[UserAnswers] = {
    val updatedUserAnswers =
      mode match {
        case CheckMode =>
          dataUpdateService.findMatchingTrustee(establisherIndex, directorIndex)(ua).map { trustee =>
           val trusteeAddressPages: AddressPages = AddressPages(trusteeAddress.EnterPostCodeId(trustee.index),
            trusteeAddress.AddressListId(trustee.index), trusteeAddress.AddressId(trustee.index))
            ua.remove(trusteeAddressPages.addressPage)
              .setOrException(trusteeAddressPages.addressListPage, address)
            }.getOrElse(ua)
        case _ => ua
    }
    val finalUpdatedUserAnswers = updatedUserAnswers.remove(addressPages.addressPage)
      .set(addressPages.addressListPage, address)
    finalUpdatedUserAnswers
  }
}
