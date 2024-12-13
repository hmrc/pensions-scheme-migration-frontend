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

import connectors.cache.UserAnswersCacheConnector
import controllers.Retrievals
import controllers.actions._
import forms.address.AddressListFormProvider
import identifiers.beforeYouStart.SchemeNameId
import identifiers.establishers.company.director.{address => Director}
import identifiers.trustees.individual.TrusteeNameId
import identifiers.trustees.individual.address.{EnterPreviousPostCodeId, PreviousAddressId, PreviousAddressListId}
import models._
import models.establishers.AddressPages
import navigators.CompoundNavigator
import play.api.data.Form
import play.api.data.FormBinding.Implicits.formBinding
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.mvc.Results.Redirect
import play.api.mvc.{Action, AnyContent}
import services.DataUpdateService
import services.common.address.{CommonAddressListService, CommonAddressListTemplateData}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import utils.UserAnswers

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class SelectPreviousAddressController @Inject()(
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

  def onPageLoad(index: Index, mode: Mode): Action[AnyContent] =
    (authenticate andThen getData andThen requireData()).async { implicit request =>
    retrieve(SchemeNameId) { schemeName =>
      getFormToTemplate(schemeName, index, mode).retrieve.map(formToTemplate =>
        common.get(
          formToTemplate(form),
          form,
          submitUrl = routes.SelectPreviousAddressController.onSubmit(index, mode)
        ))
    }
  }

  def onSubmit(index: Index, mode: Mode): Action[AnyContent] =
    (authenticate andThen getData andThen requireData()).async { implicit request =>
      val addressPages: AddressPages = AddressPages(EnterPreviousPostCodeId(index), PreviousAddressListId(index), PreviousAddressId(index))

      retrieve(SchemeNameId) { schemeName =>
        val formToTemplate: Form[Int] => CommonAddressListTemplateData = getFormToTemplate(schemeName, index, mode).retrieve.toOption.get
        implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)

        form.bindFromRequest().fold(
          formWithErrors =>
            common.post(
              formToTemplate,
              addressPages,
              Some(mode),
              manualUrlCall = routes.ConfirmPreviousAddressController.onPageLoad(index, mode),
              form = formWithErrors,
              submitUrl = routes.SelectPreviousAddressController.onSubmit(index, mode)
            ),
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
                  Redirect(controllers.trustees.individual.address.routes.ConfirmPreviousAddressController.onPageLoad(index, mode))
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
                Director.EnterPreviousPostCodeId(director.mainIndex.get, director.index),
                Director.PreviousAddressListId(director.mainIndex.get, director.index),
                Director.PreviousAddressId(director.mainIndex.get, director.index)
              )
              acc.remove(directorAddressPages.addressListPage)
                .setOrException(directorAddressPages.addressPage, address.toAddress.get)
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
                Director.EnterPreviousPostCodeId(director.mainIndex.get, director.index),
                Director.PreviousAddressListId(director.mainIndex.get, director.index),
                Director.PreviousAddressId(director.mainIndex.get, director.index)
              )
              acc.remove(directorAddressPages.addressPage)
                .setOrException(directorAddressPages.addressListPage, address)
            }
          }
        case _ => ua
      }
    val finalUpdatedUserAnswers = updatedUserAnswers.remove(addressPages.addressPage)
      .set(addressPages.addressListPage, address)
    finalUpdatedUserAnswers
  }

  def getFormToTemplate(schemeName:String, index: Index, mode: Mode) : Retrieval[Form[Int] => CommonAddressListTemplateData] =
    Retrieval(
      implicit request =>
        EnterPreviousPostCodeId(index).retrieve.map { addresses =>
          val name: String = request.userAnswers.get(TrusteeNameId(index))
            .map(_.fullName).getOrElse(Messages("trusteeEntityTypeIndividual"))

          form =>
            CommonAddressListTemplateData(
              form,
              addresses,
              Messages("trusteeEntityTypeIndividual"),
              name,
              routes.ConfirmPreviousAddressController.onPageLoad(index, mode).url,
              schemeName,
              "previousAddressList.title"
            )
        }
    )
}
