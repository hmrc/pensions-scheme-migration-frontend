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

///*
// * Copyright 2021 HM Revenue & Customs
// *
// * Licensed under the Apache License, Version 2.0 (the "License");
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// *
// *     http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// */
//
//package controllers.establishers.partnership.partner
//
//import connectors.cache.UserAnswersCacheConnector
//import controllers.Retrievals
//import controllers.actions._
//import forms.establishers.ConfirmDeleteEstablisherFormProvider
//import identifiers.establishers.partnership.partner.{ConfirmDeletePartnerId, PartnerNameId}
//import models.Index
//import navigators.CompoundNavigator
//import play.api.data.Form
//import play.api.i18n.{I18nSupport, Messages, MessagesApi}
//import play.api.libs.json.Json
//import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
//import renderer.Renderer
//import uk.gov.hmrc.nunjucks.NunjucksSupport
//import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
//import uk.gov.hmrc.viewmodels.{MessageInterpolators, Radios}
//import utils.UserAnswers
//
//import javax.inject.Inject
//import scala.concurrent.{ExecutionContext, Future}
//import scala.util.Try
//
//class ConfirmDeletePartnerController @Inject()(override val messagesApi: MessagesApi,
//                                                navigator: CompoundNavigator,
//                                                 authenticate: AuthAction,
//                                                 getData: DataRetrievalAction,
//                                                 requireData: DataRequiredAction,
//                                                 formProvider: ConfirmDeleteEstablisherFormProvider,
//                                                 val controllerComponents: MessagesControllerComponents,
//                                                 userAnswersCacheConnector: UserAnswersCacheConnector,
//                                                 renderer: Renderer
//                                               )(implicit val executionContext: ExecutionContext
//                                               )
//  extends FrontendBaseController with I18nSupport with Retrievals with NunjucksSupport {
//
//  def onPageLoad(establisherIndex: Index, partnerIndex: Index): Action[AnyContent] =
//    (authenticate andThen getData andThen requireData).async {
//      implicit request =>
//        PartnerNameId(establisherIndex, partnerIndex).retrieve.right.map { partner =>
//          if (partner.isDeleted) {
//            Future.successful(Redirect(routes.AlreadyDeletedController.onPageLoad(establisherIndex, partnerIndex)))
//          } else {
//            val json = Json.obj(
//              "form" -> form(partner.fullName),
//              "titleMessage" -> msg"messages__confirmDeletePartners__title".resolve,
//              "name" -> partner.fullName,
//              "hint" ->  Some(Messages(s"messages__confirmDeletePartners__partnershipHint")),
//              "radios" -> Radios.yesNo(formProvider(partner.fullName)(implicitly)("value")),
//              "submitUrl" -> routes.ConfirmDeletePartnerController.onSubmit(establisherIndex, partnerIndex).url,
//              "schemeName" -> existingSchemeName
//            )
//            renderer.render("delete.njk", json).map(Ok(_))
//          }
//        }getOrElse Future.successful(Redirect(controllers.routes.IndexController.onPageLoad()))
//    }
//
//  private def form(name: String)(implicit messages: Messages): Form[Boolean] = formProvider(name)
//
//  def onSubmit(establisherIndex: Index, partnerIndex: Index): Action[AnyContent] =
//    (authenticate andThen getData andThen requireData).async {
//      implicit request =>
//
//        PartnerNameId(establisherIndex, partnerIndex).retrieve.right.map { partner =>
//
//          form(partner.fullName).bindFromRequest().fold(
//            (formWithErrors: Form[_]) => {
//              val json = Json.obj(
//                "form" -> formWithErrors,
//                "titleMessage" -> msg"messages__confirmDeletePartners__title".resolve,
//                "name" ->  partner.fullName,
//                "hint" -> Some(Messages(s"messages__confirmDeletePartners__partnershipHint")),
//                "radios" -> Radios.yesNo(formProvider(partner.fullName)(implicitly)("value")),
//                "submitUrl" -> routes.ConfirmDeletePartnerController.onSubmit(establisherIndex, partnerIndex).url,
//                "schemeName" -> existingSchemeName
//              )
//              renderer.render("delete.njk", json).map(BadRequest(_))
//            },
//            value => {
//              val deletionResult: Try[UserAnswers] = if (value) {
//                request.userAnswers.set(PartnerNameId(establisherIndex, partnerIndex),
//                  partner.copy (isDeleted = true))
//              } else {
//                Try(request.userAnswers)
//              }
//              Future.fromTry(deletionResult).flatMap { answers =>
//                userAnswersCacheConnector.save(request.lock, answers.data).map { _ =>
//                  Redirect(navigator.nextPage(ConfirmDeletePartnerId(establisherIndex), answers))
//                }
//              }
//            }
//
//          )
//        }
//    }
//}
//
