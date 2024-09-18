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

package controllers.trustees.individual.address

import config.AppConfig
import connectors.AddressLookupConnector
import connectors.cache.UserAnswersCacheConnector
import controllers.actions._
import models.establishers.AddressPages
import forms.address.AddressListFormProvider
import identifiers.beforeYouStart.SchemeNameId
import identifiers.establishers.company.director.{address => Director}
import identifiers.trustees.individual.TrusteeNameId
import identifiers.trustees.individual.address.{AddressId, AddressListId, EnterPostCodeId}
import models._
import navigators.CompoundNavigator
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import renderer.Renderer
import services.DataUpdateService
import controllers.Retrievals
import play.api.data.FormBinding.Implicits.formBinding
import services.common.address.{CommonAddressListService, CommonAddressListTemplateData}
import viewmodels.Message
import uk.gov.hmrc.nunjucks.NunjucksSupport
import utils.UserAnswers
import play.api.mvc.Results.{BadRequest, Redirect}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class SelectAddressController @Inject()(
    val appConfig: AppConfig,
    override val messagesApi: MessagesApi,
    val userAnswersCacheConnector: UserAnswersCacheConnector,
    val addressLookupConnector: AddressLookupConnector,
    val navigator: CompoundNavigator,
    authenticate: AuthAction,
    getData: DataRetrievalAction,
    requireData: DataRequiredAction,
    formProvider: AddressListFormProvider,
    dataUpdateService: DataUpdateService,
    val controllerComponents: MessagesControllerComponents,
    val renderer: Renderer,
    common:CommonAddressListService
)(implicit val ec: ExecutionContext) extends I18nSupport with NunjucksSupport with Retrievals {

  private def form: Form[Int] = formProvider("selectAddress.required")

  def onPageLoad(index: Index, mode: Mode): Action[AnyContent] =
    (authenticate andThen getData andThen requireData()).async { implicit request =>
      retrieve(SchemeNameId) { schemeName =>
        getFormToJson(schemeName, index, mode).retrieve.map(formToTemplate => common.getNew(formToTemplate(form)))
      }
    }

  // TODO try to use postNew method from CommonAddressListService
  def onSubmit(index: Index, mode: Mode): Action[AnyContent] =
    (authenticate andThen getData andThen requireData()).async { implicit request =>
      val addressPages: AddressPages = AddressPages(EnterPostCodeId(index), AddressListId(index), AddressId(index))

      retrieve(SchemeNameId) { schemeName =>
        val json: Form[Int] => JsObject = getFormToJson(schemeName, index, mode).retrieve.toOption.get
        implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)

        form.bindFromRequest().fold(
          formWithErrors =>
            renderer.render(common.viewTemplate, common.prepareJson(json(formWithErrors))).map(BadRequest(_)),
          value =>
            addressPages.postcodeId.retrieve.map { addresses =>
              val address = addresses(value).copy(country = Some("GB"))
              if (address.toAddress.nonEmpty) {
                for {
                  updatedAnswers <- Future.fromTry(
                    setUpdatedAnswersForUkAddr(index, mode, addressPages, address, request.userAnswers)
                  )
                  _ <- userAnswersCacheConnector.save(request.lock, updatedAnswers.data)
                } yield {
                  val finalMode = Some(mode).getOrElse(NormalMode)
                  Redirect(navigator.nextPage(addressPages.addressListPage, updatedAnswers, finalMode))
                }
              } else {
                for {
                  updatedAnswers <- Future.fromTry(setUpdatedAnswersForNonUkAddr(
                    index,
                    mode,
                    addressPages,
                    address,
                    request.userAnswers
                  ))
                  _ <- userAnswersCacheConnector.save(request.lock, updatedAnswers.data)
                } yield {
                  Redirect(routes.ConfirmAddressController.onPageLoad(index, mode))
                }
              }
            }
        )
      }
    }

  private def setUpdatedAnswersForUkAddr(index: Index,
                                         mode: Mode,
                                         addressPages: AddressPages,
                                         address: TolerantAddress,
                                         ua: UserAnswers): Try[UserAnswers] = {
    val updatedUserAnswers =
      mode match {
        case CheckMode =>
          val directors = dataUpdateService.findMatchingDirectors(index)(ua)
          directors.foldLeft[UserAnswers](ua) { (acc, director) =>
            if (director.isDeleted)
              acc
            else
            {
              val directorAddressPages: AddressPages = AddressPages(
                Director.EnterPostCodeId(director.mainIndex.get, director.index),
                Director.AddressListId(director.mainIndex.get, director.index),
                Director.AddressId(director.mainIndex.get, director.index)
              )
              acc.remove(directorAddressPages.addressListPage).setOrException(directorAddressPages.addressPage,
                address.toAddress.get)
            }
         }
        case _ => ua
      }
    val finalUpdatedUserAnswers = updatedUserAnswers.remove(addressPages.addressListPage)
      .set(addressPages.addressPage, address.toAddress.get)
    finalUpdatedUserAnswers
  }

  private def setUpdatedAnswersForNonUkAddr(index: Index,
                                            mode: Mode,
                                            addressPages: AddressPages,
                                            address: TolerantAddress,
                                            ua: UserAnswers): Try[UserAnswers] = {
    val updatedUserAnswers =
      mode match {
        case CheckMode =>
          val directors = dataUpdateService.findMatchingDirectors(index)(ua)
          directors.foldLeft[UserAnswers](ua) { (acc, director) =>
            if (director.isDeleted)
              acc
            else
            {
              val directorAddressPages: AddressPages = AddressPages(
                Director.EnterPostCodeId(director.mainIndex.get, director.index),
                Director.AddressListId(director.mainIndex.get, director.index),
                Director.AddressId(director.mainIndex.get, director.index)
              )
              acc.remove(directorAddressPages.addressPage).setOrException(directorAddressPages.addressListPage,
                address)
            }
          }
        case _ => ua
      }
    val finalUpdatedUserAnswers = updatedUserAnswers.remove(addressPages.addressPage)
      .set(addressPages.addressListPage, address)
    finalUpdatedUserAnswers
  }

  def getFormToJson(schemeName:String, index: Index, mode: Mode) : Retrieval[Form[Int] => CommonAddressListTemplateData] =
    Retrieval(
      implicit request =>
        EnterPostCodeId(index).retrieve.map { addresses =>
          val name = request.userAnswers.get(TrusteeNameId(index))
            .map(_.fullName).getOrElse(Message("trusteeEntityTypeIndividual").resolve)

          form =>
            CommonAddressListTemplateData(
              form,
              common.transformAddressesForTemplate(addresses),
              Message("trusteeEntityTypeIndividual"),
              name,
              controllers.trustees.individual.address.routes.ConfirmAddressController.onPageLoad(index, mode).url,
              schemeName
            )
        }
    )
}
