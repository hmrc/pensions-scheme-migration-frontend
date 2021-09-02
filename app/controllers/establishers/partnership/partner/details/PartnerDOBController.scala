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
//package controllers.establishers.partnership.partner.details
//
//import connectors.cache.UserAnswersCacheConnector
//import controllers.actions._
//import controllers.dateOfBirth.DateOfBirthController
//import forms.DOBFormProvider
//import identifiers.beforeYouStart.SchemeNameId
//import identifiers.establishers.partnership.partner.PartnerNameId
//import identifiers.establishers.partnership.partner.details.PartnerDOBId
//import models.{Index, Mode}
//import navigators.CompoundNavigator
//import play.api.data.Form
//import play.api.i18n.{Messages, MessagesApi}
//import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
//import renderer.Renderer
//
//import java.time.LocalDate
//import javax.inject.Inject
//import scala.concurrent.ExecutionContext
//
//class PartnerDOBController @Inject()(
//                                       override val messagesApi: MessagesApi,
//                                       val navigator: CompoundNavigator,
//                                       authenticate: AuthAction,
//                                       getData: DataRetrievalAction,
//                                       requireData: DataRequiredAction,
//                                       formProvider: DOBFormProvider,
//                                       val controllerComponents: MessagesControllerComponents,
//                                       val userAnswersCacheConnector: UserAnswersCacheConnector,
//                                       val renderer: Renderer
//                                     )(implicit val executionContext: ExecutionContext) extends DateOfBirthController {
//
//  val form: Form[LocalDate] = formProvider()
//
//  def onPageLoad(establisherIndex: Index, partnerIndex: Index, mode: Mode): Action[AnyContent] =
//    (authenticate andThen getData andThen requireData).async {
//      implicit request =>
//
//        SchemeNameId.retrieve.right.map {
//          schemeName =>
//            get(
//              dobId        = PartnerDOBId(establisherIndex, partnerIndex),
//              personNameId = PartnerNameId(establisherIndex, partnerIndex),
//              schemeName   = schemeName,
//              entityType   = Messages("messages__partner")
//            )
//        }
//    }
//
//  def onSubmit(establisherIndex: Index, partnerIndex: Index, mode: Mode): Action[AnyContent] =
//    (authenticate andThen getData andThen requireData).async {
//      implicit request =>
//        SchemeNameId.retrieve.right.map {
//          schemeName =>
//            post(
//              dobId        = PartnerDOBId(establisherIndex, partnerIndex),
//              personNameId = PartnerNameId(establisherIndex, partnerIndex),
//              schemeName   = schemeName,
//              entityType   = Messages("messages__partner"),
//              mode         = mode
//            )
//        }
//    }
//}
