/*
 * Copyright 2022 HM Revenue & Customs
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

package controllers.trustees.individual.address

import config.AppConfig
import connectors.AddressLookupConnector
import connectors.cache.UserAnswersCacheConnector
import controllers.actions._
import controllers.address.{AddressListController, AddressPages}
import forms.address.AddressListFormProvider
import identifiers.beforeYouStart.SchemeNameId
import identifiers.trustees.individual.TrusteeNameId
import identifiers.trustees.individual.address.{EnterPreviousPostCodeId, PreviousAddressId, PreviousAddressListId}
import identifiers.establishers.company.director.{address => Director}
import models.{CheckMode, Index, Mode, NormalMode, TolerantAddress}
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
                                                val renderer: Renderer)(implicit val ec: ExecutionContext)
  extends AddressListController with I18nSupport
  with NunjucksSupport with Retrievals {

  override def form: Form[Int] = formProvider("selectAddress.required")

  def onPageLoad(index: Index, mode: Mode): Action[AnyContent] = (authenticate andThen getData andThen requireData()).async { implicit request =>
    retrieve(SchemeNameId) { schemeName =>
      getFormToJson(schemeName, index, mode).retrieve.right.map(get)
    }
  }

  def onSubmit(index: Index, mode: Mode): Action[AnyContent] =
    (authenticate andThen getData andThen requireData()).async { implicit request =>
        val addressPages: AddressPages = AddressPages(EnterPreviousPostCodeId(index), PreviousAddressListId(index), PreviousAddressId(index))
      retrieve(SchemeNameId) { schemeName =>
        val json: Form[Int] => JsObject = getFormToJson(schemeName, index, mode).retrieve.right.get
        form.bindFromRequest().fold(
          formWithErrors =>
            renderer.render(viewTemplate, prepareJson(json(formWithErrors))).map(BadRequest(_)),
          value =>
            addressPages.postcodeId.retrieve.right.map { addresses =>
              val address = addresses(value).copy(country = Some("GB"))
              if (address.toAddress.nonEmpty) {
                for {
                  updatedAnswers <- Future.fromTry(
                    setUpdatedAnswersOne(index, mode, addressPages, address, request.userAnswers)
                  )
                  _ <- userAnswersCacheConnector.save(request.lock, updatedAnswers.data)
                } yield {
                  val finalMode = Some(mode).getOrElse(NormalMode)
                  Redirect(navigator.nextPage(addressPages.addressListPage, updatedAnswers, finalMode))
                }
              } else {
                for {
                  updatedAnswers <-

                    Future.fromTry(setUpdatedAnswersTwo(index, mode, addressPages, address, request.userAnswers)
                    )
                  _ <- userAnswersCacheConnector.save(request.lock, updatedAnswers.data)
                } yield {
                  Redirect(routes.ConfirmPreviousAddressController.onPageLoad(index, mode))
                }

              }
            }
        )
      }
    }

  private def setUpdatedAnswersOne(index: Index, mode: Mode, addressPages: AddressPages,
                                   address: TolerantAddress, ua: UserAnswers): Try[UserAnswers] = {
    var updatedUserAnswers: Try[UserAnswers] = Try(ua)
    if (mode == CheckMode) {
      val directors = dataUpdateService.findMatchingDirectors(index)(ua)
      for (director <- directors) {
        if (!director.isDeleted) {
          val directorAddressPages: AddressPages = AddressPages(Director.EnterPreviousPostCodeId(director.mainIndex.get, director.index),
            Director.PreviousAddressListId(director.mainIndex.get, director.index), Director.PreviousAddressId(director.mainIndex.get, director.index))
          updatedUserAnswers = updatedUserAnswers.get.remove(directorAddressPages.addressListPage).set(directorAddressPages.addressPage,
            address.toAddress.get)
        }
      }
    }
    val finalUpdatedUserAnswers = updatedUserAnswers.get.remove(addressPages.addressListPage).set(addressPages.addressPage,
      address.toAddress.get)
    finalUpdatedUserAnswers
  }

  private def setUpdatedAnswersTwo(index: Index, mode: Mode, addressPages: AddressPages,
                                   address: TolerantAddress, ua: UserAnswers): Try[UserAnswers] = {
    var updatedUserAnswers: Try[UserAnswers] = Try(ua)
    if (mode == CheckMode) {
      val directors = dataUpdateService.findMatchingDirectors(index)(ua)
      for (director <- directors) {
        if (!director.isDeleted) {
          val directorAddressPages: AddressPages = AddressPages(Director.EnterPreviousPostCodeId(director.mainIndex.get, director.index),
            Director.PreviousAddressListId(director.mainIndex.get, director.index), Director.PreviousAddressId(director.mainIndex.get, director.index))
          updatedUserAnswers = ua.remove(directorAddressPages.addressPage).set(directorAddressPages.addressListPage,
            address)
        }
      }
    }
    val finalUpdatedUserAnswers = updatedUserAnswers.get.remove(addressPages.addressPage).set(addressPages.addressListPage,
      address)
    finalUpdatedUserAnswers
  }


  def getFormToJson(schemeName:String, index: Index, mode: Mode) : Retrieval[Form[Int] => JsObject] =
    Retrieval(
      implicit request =>
        EnterPreviousPostCodeId(index).retrieve.right.map { addresses =>

          val msg = request2Messages(request)

          val name = request.userAnswers.get(TrusteeNameId(index)).map(_.fullName).getOrElse(msg("trusteeEntityTypeIndividual"))

          form => Json.obj(
            "form" -> form,
            "addresses" -> transformAddressesForTemplate(addresses, countryOptions),
            "entityType" -> msg("trusteeEntityTypeIndividual"),
            "entityName" -> name,
            "enterManuallyUrl" -> controllers.trustees.individual.address.routes.ConfirmPreviousAddressController.onPageLoad(index, mode).url,
            "schemeName" -> schemeName,
            "h1MessageKey" -> "previousAddressList.title"
          )
        }
    )
}
