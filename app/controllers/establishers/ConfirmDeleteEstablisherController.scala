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

package controllers.establishers

import connectors.cache.UserAnswersCacheConnector
import controllers.Retrievals
import controllers.actions._
import forms.establishers.ConfirmDeleteEstablisherFormProvider
import identifiers.establishers.ConfirmDeleteEstablisherId
import identifiers.establishers.company.CompanyDetailsId
import identifiers.establishers.individual.EstablisherNameId
import identifiers.establishers.partnership.PartnershipDetailsId
import models._
import models.establishers.EstablisherKind
import models.establishers.EstablisherKind.{Company, Individual, Partnership}
import models.requests.DataRequest
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

class ConfirmDeleteEstablisherController @Inject()(override val messagesApi: MessagesApi,
                                                    navigator: CompoundNavigator,
                                                    authenticate: AuthAction,
                                                    getData: DataRetrievalAction,
                                                    requireData: DataRequiredAction,
                                                    formProvider: ConfirmDeleteEstablisherFormProvider,
                                                    val controllerComponents: MessagesControllerComponents,
                                                    userAnswersCacheConnector: UserAnswersCacheConnector,
                                                   deleteView: DeleteView
                                                  )(implicit val executionContext: ExecutionContext) extends
  FrontendBaseController with I18nSupport with Retrievals {

  def onPageLoad(index: Index, establisherKind: EstablisherKind): Action[AnyContent] =
    (authenticate andThen getData andThen requireData()).async {
      implicit request =>
        getDeletableEstablisher(index, establisherKind, request.userAnswers) map {
          establisher =>
            if (establisher.isDeleted) {
              Future.successful(Redirect(routes.AlreadyDeletedController.onPageLoad(index, establisherKind)))
            } else {
              Future.successful(Ok(
                deleteView(
                  form(establisher.name),
                  Messages("messages__confirmDeleteEstablisher__title"),
                  establisher.name,
                  getHintText(establisherKind),
                  utils.Radios.yesNo(formProvider(establisher.name)(implicitly)("value")),
                  existingSchemeName.getOrElse(""),
                  routes.ConfirmDeleteEstablisherController.onSubmit(index, establisherKind)
                )
              ))
            }
        } getOrElse {
          throw new RuntimeException("index page unavailable")
        }
    }

  private def getDeletableEstablisher(index: Index, establisherKind: EstablisherKind, userAnswers: UserAnswers)
  : Option[DeletableEstablisher] = {
    establisherKind match {
      case Individual => userAnswers.get(EstablisherNameId(index)).map(details =>
        DeletableEstablisher(details.fullName, details.isDeleted))
      case Company => userAnswers.get(CompanyDetailsId(index)).map(details =>
        DeletableEstablisher(details.companyName, details.isDeleted))
      case Partnership => userAnswers.get(PartnershipDetailsId(index)).map(details =>
        DeletableEstablisher(details.partnershipName, details.isDeleted))
      case null => None
    }
  }

  def onSubmit(establisherIndex: Index, establisherKind: EstablisherKind)
  : Action[AnyContent] =
    (authenticate andThen getData andThen requireData()).async {
      implicit request =>

        establisherKind match {
          case Individual =>
            EstablisherNameId(establisherIndex).retrieve.map { establisherDetails =>
              updateEstablisherKind(establisherDetails.fullName, establisherKind, establisherIndex, Some(establisherDetails), None,None)
            }
          case Company =>
            CompanyDetailsId(establisherIndex).retrieve.map { establisherDetails =>
              updateEstablisherKind(establisherDetails.companyName, establisherKind, establisherIndex, None, Some(establisherDetails),None)
            }
          case Partnership =>
            PartnershipDetailsId(establisherIndex).retrieve.map { partnershipDetails =>
              updateEstablisherKind(partnershipDetails.partnershipName, establisherKind, establisherIndex, None, None,Some(partnershipDetails))
            }
          case null =>
            throw new RuntimeException("index page unavailable")
        }
    }

  private def updateEstablisherKind(name: String,
                                    establisherKind: EstablisherKind,
                                    establisherIndex: Index,
                                    establisherName: Option[PersonName],
                                    companyDetails: Option[CompanyDetails],
                                    partnershipDetails: Option[PartnershipDetails])(implicit request: DataRequest[AnyContent])
  : Future[Result] = {
    form(name).bindFromRequest().fold(
      (formWithErrors: Form[?]) => {
        Future.successful(BadRequest(
          deleteView(
            formWithErrors,
            Messages("messages__confirmDeleteEstablisher__title"),
            name,
            getHintText(establisherKind),
            utils.Radios.yesNo(formProvider(name)(implicitly)("value")),
            existingSchemeName.getOrElse(""),
            routes.ConfirmDeleteEstablisherController.onSubmit(establisherIndex, establisherKind)
          )
        ))
      },
      value => {
        val deletionResult: Try[UserAnswers] = if (value) {
          establisherKind match {
            case Individual => establisherName.fold(Try(request.userAnswers))(
              individual => request.userAnswers.set(EstablisherNameId(establisherIndex),
                individual.copy (isDeleted = true)))
            case Company => companyDetails.fold(Try(request.userAnswers))(
              company => request.userAnswers.set(CompanyDetailsId(establisherIndex),
                company.copy (isDeleted = true)))
            case Partnership => partnershipDetails.fold(Try(request.userAnswers))(
              partnership => request.userAnswers.set(PartnershipDetailsId(establisherIndex),
                partnership.copy (isDeleted = true)))
            case null => Try(request.userAnswers)
          }
        } else {
          Try(request.userAnswers)
        }
        Future.fromTry(deletionResult).flatMap { answers =>
          userAnswersCacheConnector.save(request.lock, answers.data).map { _ =>
            Redirect(navigator.nextPage(ConfirmDeleteEstablisherId, answers))
          }
        }
      }
    )
  }

  private def form(name: String)(implicit messages: Messages): Form[Boolean] = formProvider(name)

  private def getHintText(establisherKind: EstablisherKind)(implicit request: DataRequest[AnyContent])
  : Option[String] =
    establisherKind match {
      case EstablisherKind.Company =>
        Some(Messages(s"messages__confirmDeleteEstablisher__companyHint"))
      case EstablisherKind.Partnership =>
        Some(Messages(s"messages__confirmDeleteEstablisher__partnershipHint"))
      case _ => None
    }

  private case class DeletableEstablisher(name: String, isDeleted: Boolean)

}
