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
import connectors.cache.UserAnswersCacheConnector
import controllers.Retrievals
import controllers.actions._
import controllers.address.ManualAddressController
import forms.address.AddressFormProvider
import identifiers.beforeYouStart.SchemeNameId
import identifiers.establishers.company.director.{address => Director}
import identifiers.trustees.individual.TrusteeNameId
import identifiers.trustees.individual.address.{AddressId, AddressListId}
import models._
import navigators.CompoundNavigator
import play.api.data.Form
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import renderer.Renderer
import services.DataUpdateService
import uk.gov.hmrc.nunjucks.NunjucksSupport
import utils.UserAnswers

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class ConfirmAddressController @Inject()( override val messagesApi: MessagesApi,
                                          val userAnswersCacheConnector: UserAnswersCacheConnector,
                                          val navigator: CompoundNavigator,
                                          authenticate: AuthAction,
                                          getData: DataRetrievalAction,
                                          requireData: DataRequiredAction,
                                          formProvider: AddressFormProvider,
                                          dataUpdateService: DataUpdateService,
                                          val controllerComponents: MessagesControllerComponents,
                                          val config: AppConfig,
                                          val renderer: Renderer
)(implicit ec: ExecutionContext) extends ManualAddressController
  with Retrievals with I18nSupport with NunjucksSupport {

  def form(implicit messages: Messages): Form[Address] = formProvider()

  override protected val pageTitleEntityTypeMessageKey: Option[String] = Some("messages__individual")

  def onPageLoad(index: Index, mode: Mode): Action[AnyContent] =
    (authenticate andThen getData andThen requireData()).async { implicit request =>
      (TrusteeNameId(index) and SchemeNameId).retrieve.map { case trusteeName ~ schemeName =>
          get(Some(schemeName), trusteeName.fullName, AddressId(index),AddressListId(index), AddressConfiguration.PostcodeFirst)
      }
    }

  def onSubmit(index: Index, mode: Mode): Action[AnyContent] =
    (authenticate andThen getData andThen requireData()).async { implicit request =>
      (TrusteeNameId(index) and SchemeNameId).retrieve.map { case trusteeName ~ schemeName =>
        form
          .bindFromRequest()
          .fold(
            formWithErrors => {
              renderer.render(viewTemplate, json(Some(schemeName), trusteeName.fullName, formWithErrors, AddressConfiguration.PostcodeFirst)).map(BadRequest(_))
            },
            value =>
              for {
                updatedAnswers <- Future.fromTry(setUpdatedAnswers(index, value, mode, request.userAnswers))
                _ <- userAnswersCacheConnector.save(request.lock, updatedAnswers.data)
              } yield {
                Redirect(navigator.nextPage(AddressId(index), updatedAnswers, mode))
              }
          )
      }
    }

  private def setUpdatedAnswers(index: Index, value: Address, mode: Mode, ua: UserAnswers): Try[UserAnswers] = {
    val updatedUserAnswers =
      mode match {
        case CheckMode =>
          val directors = dataUpdateService.findMatchingDirectors(index)(ua)
          directors.foldLeft[UserAnswers](ua) { (acc, director) =>
            if (director.isDeleted)
              acc
            else
              acc.setOrException(Director.AddressId(director.mainIndex.get, director.index), value)
          }
        case _ => ua
      }
  val finalUpdatedUserAnswers = updatedUserAnswers.set(AddressId(index), value)
  finalUpdatedUserAnswers
  }
}
