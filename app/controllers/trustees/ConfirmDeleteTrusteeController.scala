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

package controllers.trustees

import connectors.cache.UserAnswersCacheConnector
import controllers.Retrievals
import controllers.actions._
import forms.trustees.ConfirmDeleteTrusteeFormProvider
import identifiers.trustees.{AnyTrusteesId, ConfirmDeleteTrusteeId, OtherTrusteesId}
import identifiers.trustees.company.CompanyDetailsId
import identifiers.trustees.individual.TrusteeNameId
import identifiers.trustees.partnership.PartnershipDetailsId
import models._
import models.requests.DataRequest
import models.trustees.TrusteeKind
import models.trustees.TrusteeKind.{Company, Individual, Partnership}
import navigators.CompoundNavigator
import play.api.data.Form
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import renderer.Renderer
import uk.gov.hmrc.nunjucks.NunjucksSupport
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import uk.gov.hmrc.viewmodels.{MessageInterpolators, Radios}
import utils.UserAnswers

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
                                                    renderer: Renderer
                                                  )(implicit val executionContext: ExecutionContext) extends
  FrontendBaseController with I18nSupport with Retrievals with NunjucksSupport {

  def onPageLoad(index: Index, trusteeKind: TrusteeKind): Action[AnyContent] =
    (authenticate andThen getData andThen requireData()).async {
      implicit request =>
        getDeletableTrustee(index, trusteeKind, request.userAnswers) map {
          trustee =>
            if (trustee.isDeleted) {
              Future.successful(Redirect(routes.AlreadyDeletedController.onPageLoad(index, trusteeKind)))
            } else {
              val json = Json.obj(
                "form" -> form(trustee.name),
                "titleMessage" -> msg"messages__confirmDeleteTrustee__title".resolve,
                "name" -> trustee.name,
                "hint" -> getHintText(trusteeKind),
                "radios" -> Radios.yesNo(formProvider(trustee.name)(implicitly)("value")),
                "submitUrl" -> routes.ConfirmDeleteTrusteeController.onSubmit(index, trusteeKind).url,
                "schemeName" -> existingSchemeName
              )
              renderer.render("delete.njk", json).map(Ok(_))
            }
        } getOrElse Future.successful(Redirect(controllers.routes.IndexController.onPageLoad()))
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
      case _ => None
    }
  }

  def onSubmit(trusteeIndex: Index, trusteeKind: TrusteeKind)
  : Action[AnyContent] =
    (authenticate andThen getData andThen requireData()).async {
      implicit request =>

        trusteeKind match {
          case Individual =>
            TrusteeNameId(trusteeIndex).retrieve.right.map { trusteeDetails =>
              updateTrusteeKind(trusteeDetails.fullName, trusteeKind, trusteeIndex, Some(trusteeDetails), None, None)
            }
          case Company =>
            CompanyDetailsId(trusteeIndex).retrieve.right.map { trusteeDetails =>
              updateTrusteeKind(trusteeDetails.companyName, trusteeKind, trusteeIndex, None, Some(trusteeDetails), None)
            }
          case Partnership =>
            PartnershipDetailsId(trusteeIndex).retrieve.right.map { trusteeDetails =>
              updateTrusteeKind(trusteeDetails.partnershipName, trusteeKind, trusteeIndex, None, None, Some(trusteeDetails))
            }
          case _ =>
            Future.successful(Redirect(controllers.routes.IndexController.onPageLoad()))
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
      (formWithErrors: Form[_]) => {
        val json = Json.obj(
          "form" -> formWithErrors,
          "titleMessage" -> msg"messages__confirmDeleteTrustee__title".resolve,
          "name" -> name,
          "hint" -> getHintText(trusteeKind),
          "radios" -> Radios.yesNo(formProvider(name)(implicitly)("value")),
          "submitUrl" -> routes.ConfirmDeleteTrusteeController.onSubmit(trusteeIndex, trusteeKind).url,
          "schemeName" -> existingSchemeName
        )
        renderer.render("delete.njk", json).map(BadRequest(_))
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
            case _ => Try(request.userAnswers)
          }
        } else {
          Try(request.userAnswers)
        }
        Future.fromTry(deletionResult).flatMap { answers =>
          val updatedUA = answers.removeAll(Set(OtherTrusteesId, AnyTrusteesId))
          userAnswersCacheConnector.save(request.lock, updatedUA.data).map { _ =>
            Redirect(navigator.nextPage(ConfirmDeleteTrusteeId, updatedUA))
          }
        }
      }
    )
  }

  private def form(name: String)(implicit messages: Messages): Form[Boolean] = formProvider(name)

  private def getHintText(trusteeKind: TrusteeKind)(implicit request: DataRequest[AnyContent])
  : Option[String] =
    trusteeKind match {
      case TrusteeKind.Company =>
        Some(Messages(s"messages__confirmDeleteTrustee__companyHint"))
      case TrusteeKind.Partnership =>
        Some(Messages(s"messages__confirmDeleteTrustee__partnershipHint"))
      case _ => None
    }

  private case class DeletableTrustee(name: String, isDeleted: Boolean)

}
