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

import config.AppConfig
import connectors.AddressLookupConnector
import connectors.cache.UserAnswersCacheConnector
import controllers.actions._
import controllers.address.{AddressListController, AddressPages}
import forms.address.AddressListFormProvider
import identifiers.beforeYouStart.SchemeNameId
import identifiers.establishers.company.director.DirectorNameId
import identifiers.establishers.company.director.address.{EnterPreviousPostCodeId, PreviousAddressId, PreviousAddressListId}
import identifiers.trustees.individual.{address => trusteeAddress}
import models._
import navigators.CompoundNavigator
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import renderer.Renderer
import services.DataUpdateService
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.nunjucks.NunjucksSupport
import utils.{CountryOptions, UserAnswers}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class SelectPreviousAddressController @Inject()(val appConfig: AppConfig,
                                                override val messagesApi: MessagesApi,
                                                val userAnswersCacheConnector: UserAnswersCacheConnector,
                                                val addressLookupConnector: AddressLookupConnector,
                                                val navigator: CompoundNavigator,
                                                authenticate: AuthAction,
                                                getData: DataRetrievalAction,
                                                requireData: DataRequiredAction,
                                                formProvider: AddressListFormProvider,
                                                dataUpdateService: DataUpdateService,
                                                countryOptions: CountryOptions,
                                                val controllerComponents: MessagesControllerComponents,
                                                val renderer: Renderer)(implicit val ec: ExecutionContext) extends AddressListController with I18nSupport
  with NunjucksSupport with Retrievals {

  def onPageLoad(establisherIndex: Index, directorIndex: Index, mode: Mode): Action[AnyContent] =
    (authenticate andThen getData andThen requireData()).async {
      implicit request =>
        retrieve(SchemeNameId) { schemeName =>
          getFormToJson(schemeName, establisherIndex, directorIndex, mode).retrieve.map(get)
        }
    }

  def getFormToJson(schemeName: String,
                    establisherIndex: Index,
                    directorIndex: Index,
                    mode: Mode): Retrieval[Form[Int] => JsObject] =
    Retrieval(
      implicit request =>
        EnterPreviousPostCodeId(establisherIndex, directorIndex).retrieve.map { addresses =>

          val msg = request2Messages(request)

          val name = request.userAnswers.get(DirectorNameId(establisherIndex, directorIndex)).map(_.fullName).getOrElse(msg("messages__director"))

          form =>
            Json.obj(
              "form" -> form,
              "addresses" -> transformAddressesForTemplate(addresses, countryOptions),
              "entityType" -> msg("messages__director"),
              "entityName" -> name,
              "enterManuallyUrl" -> routes.ConfirmPreviousAddressController.onPageLoad(establisherIndex, directorIndex, mode).url,
              "schemeName" -> schemeName,
              "h1MessageKey" -> "previousAddressList.title"
            )
        }
    )

  def onSubmit(establisherIndex: Index, directorIndex: Index, mode: Mode): Action[AnyContent] =
    (authenticate andThen getData andThen requireData()).async { implicit request =>

      val addressPages: AddressPages = AddressPages(
        EnterPreviousPostCodeId(establisherIndex, directorIndex),
        PreviousAddressListId(establisherIndex, directorIndex),
        PreviousAddressId(establisherIndex, directorIndex))

      retrieve(SchemeNameId) { schemeName =>
        val json: Form[Int] => JsObject = getFormToJson(schemeName, establisherIndex, directorIndex, mode).retrieve.toOption.get
        form.bindFromRequest().fold(
          formWithErrors =>
            renderer.render(viewTemplate, prepareJson(json(formWithErrors))).map(BadRequest(_)),
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
                  updatedAnswers <-

                    Future.fromTry(setUpdatedAnswersForNonUkAddr(establisherIndex, directorIndex, mode, addressPages, address, request.userAnswers)
                    )
                  _ <- userAnswersCacheConnector.save(request.lock, updatedAnswers.data)
                } yield {
                  Redirect(routes.ConfirmPreviousAddressController.onPageLoad(establisherIndex, directorIndex, mode))
                }

              }
            }
        )
      }
    }

  override def form: Form[Int] = formProvider("selectAddress.required")

  private def setUpdatedAnswersForUkAddr(establisherIndex: Index, directorIndex: Index, mode: Mode, addressPages: AddressPages,
                                   address: TolerantAddress, ua: UserAnswers): Try[UserAnswers] = {
    val updatedUserAnswers =
    mode match {
      case CheckMode =>
        dataUpdateService.findMatchingTrustee(establisherIndex, directorIndex)(ua).map { trustee =>
          val trusteeAddressPages: AddressPages = AddressPages(trusteeAddress.EnterPreviousPostCodeId(trustee.index),
            trusteeAddress.PreviousAddressListId(trustee.index), trusteeAddress.PreviousAddressId(trustee.index))
          ua.remove(trusteeAddressPages.addressListPage).setOrException(trusteeAddressPages.addressPage,
            address.toAddress.get)
        }.getOrElse(ua)
      case _ => ua
    }
    val finalUpdatedUserAnswers = updatedUserAnswers.remove(addressPages.addressListPage).set(addressPages.addressPage,
      address.toAddress.get)
    finalUpdatedUserAnswers
  }

  private def setUpdatedAnswersForNonUkAddr(establisherIndex: Index, directorIndex: Index, mode: Mode, addressPages: AddressPages,
                                   address: TolerantAddress, ua: UserAnswers): Try[UserAnswers] = {
    val updatedUserAnswers =
      mode match {
        case CheckMode =>
          dataUpdateService.findMatchingTrustee(establisherIndex, directorIndex)(ua).map { trustee =>
            val trusteeAddressPages: AddressPages = AddressPages(trusteeAddress.EnterPreviousPostCodeId(trustee.index),
              trusteeAddress.PreviousAddressListId(trustee.index), trusteeAddress.PreviousAddressId(trustee.index))
            ua.remove(trusteeAddressPages.addressPage).setOrException(trusteeAddressPages.addressListPage,
              address)
          }.getOrElse(ua)
        case _ => ua
    }
    val finalUpdatedUserAnswers = updatedUserAnswers.remove(addressPages.addressPage).set(addressPages.addressListPage,
      address)
    finalUpdatedUserAnswers
  }
}
