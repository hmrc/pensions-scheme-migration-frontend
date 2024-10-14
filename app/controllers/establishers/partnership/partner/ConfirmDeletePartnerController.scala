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

package controllers.establishers.partnership.partner

import connectors.cache.UserAnswersCacheConnector
import controllers.Retrievals
import controllers.actions._
import forms.establishers.ConfirmDeleteEstablisherFormProvider
import identifiers.establishers.partnership.partner.{ConfirmDeletePartnerId, PartnerNameId}
import models.Index
import navigators.CompoundNavigator
import play.api.data.Form
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import uk.gov.hmrc.viewmodels.{MessageInterpolators, Radios}
import utils.{TwirlMigration, UserAnswers}
import views.html.DeleteView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class ConfirmDeletePartnerController @Inject()(override val messagesApi: MessagesApi,
                                                navigator: CompoundNavigator,
                                                 authenticate: AuthAction,
                                                 getData: DataRetrievalAction,
                                                 requireData: DataRequiredAction,
                                                 formProvider: ConfirmDeleteEstablisherFormProvider,
                                                 val controllerComponents: MessagesControllerComponents,
                                                 userAnswersCacheConnector: UserAnswersCacheConnector,
                                                 deleteView: DeleteView
                                               )(implicit val executionContext: ExecutionContext
                                               )
  extends FrontendBaseController with I18nSupport with Retrievals {

  def onPageLoad(establisherIndex: Index, partnerIndex: Index): Action[AnyContent] =
    (authenticate andThen getData andThen requireData()).async {
      implicit request =>
        PartnerNameId(establisherIndex, partnerIndex).retrieve.map { partner =>
          if (partner.isDeleted) {
            Future.successful(Redirect(routes.AlreadyDeletedController.onPageLoad(establisherIndex, partnerIndex)))
          } else {
            Future.successful(Ok(
              deleteView(
                form(partner.fullName),
                msg"messages__confirmDeletePartners__title".resolve,
                partner.fullName,
                Some(Messages(s"messages__confirmDeletePartners__partnershipHint")),
                utils.Radios.yesNo(formProvider(partner.fullName)(implicitly)("value")),
                existingSchemeName.getOrElse(""),
                routes.ConfirmDeletePartnerController.onSubmit(establisherIndex, partnerIndex)
              )
            ))
          }
        } getOrElse {
          throw new RuntimeException("index page unavailable")
        }
    }

  private def form(name: String)(implicit messages: Messages): Form[Boolean] = formProvider(name)

  def onSubmit(establisherIndex: Index, partnerIndex: Index): Action[AnyContent] =
    (authenticate andThen getData andThen requireData()).async {
      implicit request =>

        PartnerNameId(establisherIndex, partnerIndex).retrieve.map { partner =>

          form(partner.fullName).bindFromRequest().fold(
            (formWithErrors: Form[_]) => {
              Future.successful(BadRequest(
                deleteView(
                  formWithErrors,
                  msg"messages__confirmDeletePartners__title".resolve,
                  partner.fullName,
                  Some(Messages(s"messages__confirmDeletePartners__partnershipHint")),
                  utils.Radios.yesNo(formProvider(partner.fullName)(implicitly)("value")),
                  existingSchemeName.getOrElse(""),
                  routes.ConfirmDeletePartnerController.onSubmit(establisherIndex, partnerIndex)
                )
              ))
            },
            value => {
              val deletionResult: Try[UserAnswers] = if (value) {
                request.userAnswers.set(PartnerNameId(establisherIndex, partnerIndex),
                  partner.copy (isDeleted = true))
              } else {
                Try(request.userAnswers)
              }
              Future.fromTry(deletionResult).flatMap { answers =>
                userAnswersCacheConnector.save(request.lock, answers.data).map { _ =>
                  Redirect(navigator.nextPage(ConfirmDeletePartnerId(establisherIndex), answers))
                }
              }
            }

          )
        }
    }
}

