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
//package controllers.establishers.partnership
//
//import config.AppConfig
//import controllers.Retrievals
//import controllers.actions._
//import forms.establishers.partnership.partner.AddPartnersFormProvider
//import helpers.AddToListHelper
//import identifiers.establishers.partnership.AddPartnersId
//import models.Mode
//import navigators.CompoundNavigator
//import play.api.data.Form
//import play.api.i18n.{I18nSupport, MessagesApi}
//import play.api.libs.json.Json
//import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
//import renderer.Renderer
//import uk.gov.hmrc.nunjucks.NunjucksSupport
//import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
//import uk.gov.hmrc.viewmodels.Radios
//
//import javax.inject.Inject
//import scala.concurrent.{ExecutionContext, Future}
//
//class AddPartnersController @Inject()(
//                                               override val messagesApi: MessagesApi,
//                                               navigator: CompoundNavigator,
//                                               authenticate: AuthAction,
//                                               getData: DataRetrievalAction,
//                                               requireData: DataRequiredAction,
//                                               formProvider: AddPartnersFormProvider,
//                                               helper: AddToListHelper,
//                                               config: AppConfig,
//                                               val controllerComponents: MessagesControllerComponents,
//                                               renderer: Renderer
//                                             )(implicit val executionContext: ExecutionContext)
//  extends FrontendBaseController
//    with Retrievals
//    with I18nSupport
//    with NunjucksSupport {
//
//  private val form: Form[Boolean] = formProvider()
//
//  def onPageLoad(index: Int,mode: Mode): Action[AnyContent] =
//    (authenticate andThen getData andThen requireData).async {
//      implicit request =>
//
//        val partners = request.userAnswers.allPartnersAfterDelete(index)
//        val itemList=   helper.directorsOrPartnersItemList(partners)
//        renderer.render(
//          template = "establishers/partnership/addPartner.njk",
//          ctx = Json.obj(
//            "form"       -> form,
//            "itemList"      -> itemList,
//            "radios"     -> Radios.yesNo(form("value")),
//            "schemeName" -> existingSchemeName,
//            "partnerSize" -> partners.size ,
//            "maxPartners" -> config.maxPartners
//
//          )
//        ).map(Ok(_))
//    }
//
//  def onSubmit(index: Int,mode: Mode): Action[AnyContent] =
//    (authenticate andThen getData andThen requireData).async {
//      implicit request =>
//        val partners = request.userAnswers.allPartnersAfterDelete(index)
//        val itemList=   helper.directorsOrPartnersItemList(partners)
//        if (partners.isEmpty || partners.lengthCompare(config.maxPartners) >= 0) {
//          Future.successful(Redirect(
//            navigator.nextPage(
//              id          = AddPartnersId(index),
//              userAnswers = request.userAnswers
//            )
//          ))
//        }
//        else {
//          form.bindFromRequest().fold(
//            formWithErrors =>
//              renderer.render(
//                template = "establishers/partnership/addPartner.njk",
//                ctx = Json.obj(
//                  "form"       -> formWithErrors,
//                  "itemList"      -> itemList,
//                  "radios"     -> Radios.yesNo(formWithErrors("value")),
//                  "schemeName" -> existingSchemeName,
//                  "partnerSize" -> partners.size ,
//                  "maxPartners" -> config.maxPartners
//                )
//              ).map(BadRequest(_)),
//            value => {
//
//              val ua = request.userAnswers.set(AddPartnersId(index),value).getOrElse(request.userAnswers)
//              Future.successful(Redirect(
//                navigator.nextPage(
//                  id          = AddPartnersId(index),
//                  userAnswers = ua
//                )
//              ))
//            }
//          )
//        }
//    }
//}
