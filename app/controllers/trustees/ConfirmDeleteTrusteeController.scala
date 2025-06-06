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

package controllers.trustees

import connectors.cache.UserAnswersCacheConnector
import controllers.Retrievals
import controllers.actions._
import forms.trustees.ConfirmDeleteTrusteeFormProvider
import identifiers.trustees.company.CompanyDetailsId
import identifiers.trustees.individual.TrusteeNameId
import identifiers.trustees.partnership.PartnershipDetailsId
import identifiers.trustees.{AnyTrusteesId, ConfirmDeleteTrusteeId, OtherTrusteesId}
import models._
import models.requests.DataRequest
import models.trustees.TrusteeKind
import models.trustees.TrusteeKind.{Company, Individual, Partnership}
import navigators.CompoundNavigator
import play.api.data.Form
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.UserAnswers
import views.html.DeleteView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class ConfirmDeleteTrusteeController @Inject()(override val messagesApi: MessagesApi,
                                                    navigator: CompoundNavigator,
                                                    authenticate: AuthAction,
                                                    getData: DataRetrievalAction,
                                                    requireData: DataRequiredAction,
                                                    formProvider: ConfirmDeleteTrusteeFormProvider,
                                                    val controllerComponents: MessagesControllerComponents,
                                                    userAnswersCacheConnector: UserAnswersCacheConnector,
                                               deleteView: DeleteView
                                                  )(implicit val executionContext: ExecutionContext) extends
  FrontendBaseController with I18nSupport with Retrievals {

  def onPageLoad(index: Index, trusteeKind: TrusteeKind): Action[AnyContent] =
    (authenticate andThen getData andThen requireData()).async {
      implicit request =>
        getDeletableTrustee(index, trusteeKind, request.userAnswers) map {
          trustee =>
            if (trustee.isDeleted) {
              Future.successful(Redirect(routes.AlreadyDeletedController.onPageLoad(index, trusteeKind)))
            } else {
              Future.successful(Ok(
                deleteView(
                  form(trustee.name),
                  Messages("messages__confirmDeleteTrustee__title"),
                  trustee.name,
                  getHintText(trusteeKind),
                  utils.Radios.yesNo(formProvider(trustee.name)(implicitly)("value")),
                  existingSchemeName.getOrElse(""),
                  routes.ConfirmDeleteTrusteeController.onSubmit(index, trusteeKind)
                )
              ))
            }
        } getOrElse {
          throw new RuntimeException("index page unavailable")
        }
    }

  private def getDeletableTrustee(index: Index, trusteeKind: TrusteeKind, userAnswers: UserAnswers)
  : Option[DeletableTrustee] = {
    trusteeKind match {
      case Individual => userAnswers.get(TrusteeNameId(index)).map(details =>
        DeletableTrustee(details.fullName, details.isDeleted))
      case Company => userAnswers.get(CompanyDetailsId(index)).map(details =>
        DeletableTrustee(details.companyName, details.isDeleted))
      case Partnership => userAnswers.get(PartnershipDetailsId(index)).map(details =>
        DeletableTrustee(details.partnershipName, details.isDeleted))
      case null => None
    }
  }

  def onSubmit(trusteeIndex: Index, trusteeKind: TrusteeKind)
  : Action[AnyContent] =
    (authenticate andThen getData andThen requireData()).async {
      implicit request =>

        trusteeKind match {
          case Individual =>
            TrusteeNameId(trusteeIndex).retrieve.map { trusteeDetails =>
              updateTrusteeKind(trusteeDetails.fullName, trusteeKind, trusteeIndex, Some(trusteeDetails), None, None)
            }
          case Company =>
            CompanyDetailsId(trusteeIndex).retrieve.map { trusteeDetails =>
              updateTrusteeKind(trusteeDetails.companyName, trusteeKind, trusteeIndex, None, Some(trusteeDetails), None)
            }
          case Partnership =>
            PartnershipDetailsId(trusteeIndex).retrieve.map { trusteeDetails =>
              updateTrusteeKind(trusteeDetails.partnershipName, trusteeKind, trusteeIndex, None, None, Some(trusteeDetails))
            }
          case null =>
            throw new RuntimeException("index page unavailable")
        }
    }

  private def updateTrusteeKind(name: String,
                                    trusteeKind: TrusteeKind,
                                    trusteeIndex: Index,
                                    trusteeName: Option[PersonName],
                                    companyDetails: Option[CompanyDetails],
                                    partnershipDetails: Option[PartnershipDetails])(implicit request: DataRequest[AnyContent])
  : Future[Result] = {
    form(name).bindFromRequest().fold(
      (formWithErrors: Form[?]) => {
        Future.successful(BadRequest(deleteView(
            formWithErrors,
            Messages("messages__confirmDeleteTrustee__title"),
            name,
            getHintText(trusteeKind),
            utils.Radios.yesNo(formProvider(name)(implicitly)("value")),
            existingSchemeName.getOrElse(""),
            routes.ConfirmDeleteTrusteeController.onSubmit(trusteeIndex, trusteeKind)
          )))
      },
      value => {
        val deletionResult: Try[UserAnswers] = if (value) {
          trusteeKind match {
            case Individual => trusteeName.fold(Try(request.userAnswers))(
              individual => request.userAnswers.set(TrusteeNameId(trusteeIndex),
                individual.copy (isDeleted = true)))
            case Company => companyDetails.fold(Try(request.userAnswers))(
              company => request.userAnswers.set(CompanyDetailsId(trusteeIndex), company.copy(isDeleted = true)))
            case Partnership => partnershipDetails.fold(Try(request.userAnswers))(
              partnership => request.userAnswers.set(PartnershipDetailsId(trusteeIndex), partnership.copy(isDeleted = true)))
            case null => Try(request.userAnswers)
          }
        } else {
          Try(request.userAnswers)
        }
        Future.fromTry(deletionResult).flatMap { answers =>
          val updatedUA = answers.removeAll(Set(OtherTrusteesId))
          if(updatedUA.allTrusteesAfterDelete.isEmpty) {
            val updatedFinalUA = updatedUA.removeAll(Set(AnyTrusteesId))
            userAnswersCacheConnector.save(request.lock, updatedFinalUA.data).map { _ =>
              Redirect(navigator.nextPage(ConfirmDeleteTrusteeId, updatedFinalUA))
            }
          }
          else {
            userAnswersCacheConnector.save(request.lock, updatedUA.data).map { _ =>
              Redirect(navigator.nextPage(ConfirmDeleteTrusteeId, updatedUA))
            }
          }
        }
      }
    )
  }

  private def form(name: String)(implicit messages: Messages): Form[Boolean] = formProvider(name)

  private def getHintText(trusteeKind: TrusteeKind)(implicit request: DataRequest[AnyContent]): Option[String] =
    trusteeKind match {
      case TrusteeKind.Company =>
        Some(Messages("messages__confirmDeleteTrustee__companyHint"))
      case TrusteeKind.Partnership =>
        Some(Messages("messages__confirmDeleteTrustee__partnershipHint"))
      case TrusteeKind.Individual =>
        Some(Messages("messages__confirmDeleteTrustee__individualHint"))
      case null => None
    }

  private case class DeletableTrustee(name: String, isDeleted: Boolean)

}
