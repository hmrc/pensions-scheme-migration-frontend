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

package controllers.benefitsAndInsurance

import config.AppConfig
import connectors.cache.UserAnswersCacheConnector
import controllers.Retrievals
import controllers.actions._
import forms.benefitsAndInsurance.IsInvestmentRegulatedFormProvider
import identifiers.beforeYouStart.SchemeNameId
import navigators.CompoundNavigator
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import renderer.Renderer
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import uk.gov.hmrc.viewmodels.NunjucksSupport
import utils.Enumerable

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class IsInvestmentRegulatedController @Inject()(override val messagesApi: MessagesApi,
                                       userAnswersCacheConnector: UserAnswersCacheConnector,
                                       authenticate: AuthAction,
                                       getData: DataRetrievalAction,
                                       requireData: DataRequiredAction,
                                       navigator: CompoundNavigator,
                                       formProvider: IsInvestmentRegulatedFormProvider,
                                       val controllerComponents: MessagesControllerComponents,
                                       config: AppConfig,
                                       renderer: Renderer)(implicit ec: ExecutionContext)
  extends FrontendBaseController  with I18nSupport with Retrievals with Enumerable.Implicits with NunjucksSupport {

//  private def form(memberName: String)(implicit messages: Messages): Form[Boolean] =
//    formProvider(messages("isInvestmentRegulated.error.required", memberName))

  def onPageLoad: Action[AnyContent] =
    (authenticate andThen getData andThen requireData).async { implicit request =>
      SchemeNameId.retrieve.right.map { schemeName =>
        val json = Json.obj(
          "schemeName" -> schemeName,
          "returnUrl" -> controllers.routes.TaskListController.onPageLoad().url
        )
            renderer.render("benefitsAndInsurance/isInvestmentRegulated.njk", json).map(Ok(_))
        }
      }

//  def onSubmit(srn: String, startDate: LocalDate, accessType: AccessType, version: Int, index: Index): Action[AnyContent] =
//    (identify andThen getData(srn, startDate) andThen requireData).async { implicit request =>
//      DataRetrievals.retrieveSchemeName { schemeName =>
//        request.userAnswers.get(MemberDetailsPage(index)) match {
//          case Some(memberDetails) =>
//            form(memberDetails.fullName)
//              .bindFromRequest()
//              .fold(
//                formWithErrors => {
//
//                  val viewModel = GenericViewModel(
//                    submitUrl = routes.DeleteMemberController.onSubmit(srn, startDate, accessType, version, index).url,
//                    returnUrl = controllers.routes.ReturnToSchemeDetailsController.returnToSchemeDetails(srn, startDate, accessType, version).url,
//                    schemeName = schemeName
//                  )
//
//                  val json = Json.obj(
//                    "srn" -> srn,
//                    "startDate" -> Some(startDate),
//                    "form" -> formWithErrors,
//                    "viewModel" -> viewModel,
//                    "radios" -> Radios.yesNo(formWithErrors("value")),
//                    "memberName" -> memberDetails.fullName
//                  )
//
//                  renderer.render("chargeD/deleteMember.njk", json).map(BadRequest(_))
//
//                },
//                value =>
//                  if (value) {
//                    DataRetrievals.retrievePSTR {
//                      pstr =>
//                        for {
//                          updatedAnswers <- Future.fromTry(userAnswersService
//                            .removeMemberBasedCharge(MemberDetailsPage(index), totalAmount(srn, startDate, accessType, version)))
//
//                          _ <- deleteAFTChargeService.deleteAndFileAFTReturn(pstr, updatedAnswers)
//                        } yield Redirect(navigator.nextPage(DeleteMemberPage, NormalMode, updatedAnswers, srn, startDate, accessType, version))
//                    }
//                  } else {
//                    Future.successful(Redirect(navigator.nextPage(DeleteMemberPage, NormalMode, request.userAnswers, srn, startDate, accessType, version)))
//                  }
//              )
//          case _ => Future.successful(Redirect(controllers.routes.SessionExpiredController.onPageLoad()))
//        }
//      }
//    }
//
//  private def totalAmount(srn: String, startDate: LocalDate, accessType: AccessType, version: Int)(implicit request: DataRequest[AnyContent]): UserAnswers => BigDecimal =
//    chargeDHelper.getLifetimeAllowanceMembers(_, srn, startDate, accessType, version).map(_.amount).sum
}
