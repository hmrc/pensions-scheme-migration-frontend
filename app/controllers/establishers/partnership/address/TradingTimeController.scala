/*
 * Copyright 2021 HM Revenue & Customs
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

package controllers.establishers.partnership.address

import connectors.cache.UserAnswersCacheConnector
import controllers.Retrievals
import controllers.actions._
import forms.establishers.partnership.address.PartnershipTradingTimeFormProvider
import identifiers.beforeYouStart.SchemeNameId
import identifiers.establishers.partnership.PartnershipDetailsId
import identifiers.establishers.partnership.address.TradingTimeId
import models.Index
import navigators.CompoundNavigator
import play.api.data.Form
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import renderer.Renderer
import uk.gov.hmrc.nunjucks.NunjucksSupport
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import uk.gov.hmrc.viewmodels.Radios
import utils.Enumerable

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class TradingTimeController @Inject()(override val messagesApi: MessagesApi,
                                       userAnswersCacheConnector: UserAnswersCacheConnector,
                                       authenticate: AuthAction,
                                       getData: DataRetrievalAction,
                                       requireData: DataRequiredAction,
                                       navigator: CompoundNavigator,
                                       formProvider: PartnershipTradingTimeFormProvider,
                                       val controllerComponents: MessagesControllerComponents,
                                       renderer: Renderer)(implicit ec: ExecutionContext)
  extends FrontendBaseController  with I18nSupport with Retrievals with Enumerable.Implicits with NunjucksSupport {

  private def form: Form[Boolean] =
    formProvider()

  def onPageLoad(index: Index): Action[AnyContent] =
    (authenticate andThen getData andThen requireData).async { implicit request =>
      (PartnershipDetailsId(index) and SchemeNameId).retrieve.right.map { case partnershipDetails ~ schemeName =>
        val preparedForm = request.userAnswers.get(TradingTimeId(index)) match {
          case Some(value) => form.fill(value)
          case None        => form
        }
        val json = Json.obj(
          "schemeName" -> schemeName,
          "entityName" -> partnershipDetails.partnershipName,
          "entityType" -> Messages("establisherEntityTypePartnership"),
          "form" -> preparedForm,
          "radios" -> Radios.yesNo (preparedForm("value"))
        )
        renderer.render("address/tradingTime.njk", json).map(Ok(_))
      }
    }

  def onSubmit(index: Index): Action[AnyContent] =
    (authenticate andThen getData andThen requireData).async { implicit request =>
      (PartnershipDetailsId(index) and SchemeNameId).retrieve.right.map { case partnershipDetails ~ schemeName =>
        form
          .bindFromRequest()
          .fold(
            formWithErrors => {
              val json = Json.obj(
                "schemeName" -> schemeName,
                "entityName" -> partnershipDetails.partnershipName,
                "entityType" -> Messages("establisherEntityTypePartnership"),
                "form" -> formWithErrors,
                "radios" -> Radios.yesNo(form("value"))
              )

              renderer.render("address/tradingTime.njk", json).map(BadRequest(_))
            },
            value => {
              val updatedUA = request.userAnswers.setOrException(TradingTimeId(index), value)
              userAnswersCacheConnector.save(request.lock, updatedUA.data).map { _ =>
                Redirect(navigator.nextPage(TradingTimeId(index), updatedUA))
              }
            }
          )
        }
    }

}
