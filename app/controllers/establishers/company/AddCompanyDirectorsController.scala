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
//package controllers.establishers.company
//
//import controllers.Retrievals
//import controllers.actions._
//import forms.establishers.company.director.AddCompanyDirectorsFormProvider
//import navigators.CompoundNavigator
//import play.api.i18n.{I18nSupport, MessagesApi}
//import play.api.libs.json.Json
//import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
//import renderer.Renderer
//import uk.gov.hmrc.nunjucks.NunjucksSupport
//import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
//import uk.gov.hmrc.viewmodels.Radios
//import views.html.helper
//
//import javax.inject.Inject
//import scala.concurrent.{ExecutionContext, Future}
//
//class AddCompanyDirectorsController @Inject()(
//                                               override val messagesApi: MessagesApi,
//                                               //@EstablishersCompany
//                                               navigator: CompoundNavigator,
//                                               authenticate: AuthAction,
//                                               getData: DataRetrievalAction,
//                                               requireData: DataRequiredAction,
//                                               formProvider: AddCompanyDirectorsFormProvider,
//                                               val controllerComponents: MessagesControllerComponents,
//                                               renderer: Renderer
//                                             )(implicit val executionContext: ExecutionContext)
//  extends FrontendBaseController
//    with Retrievals
//    with I18nSupport
//    with NunjucksSupport {
//
//  def onPageLoad: Action[AnyContent] =
//    (authenticate andThen getData andThen requireData).async {
//      implicit request =>
//        val directors = request.userAnswers.allDirectorsAfterDelete
//        val table        = helper.mapEstablishersToTable(establishers)
//
//        renderer.render(
//          template = "establishers/addDirector.njk",
//          ctx = Json.obj(
//            "form"       -> formProvider(establishers),
//            "table"      -> table,
//            "radios"     -> Radios.yesNo(formProvider(establishers)("value")),
//            "schemeName" -> existingSchemeName
//          )
//        ).map(Ok(_))
//    }
//
//  def onSubmit: Action[AnyContent] =
//    (authenticate andThen getData andThen requireData).async {
//      implicit request =>
//        val establishers = request.userAnswers.allEstablishersAfterDelete
//        val table        = helper.mapEstablishersToTable(establishers)
//
//        formProvider(establishers).bindFromRequest().fold(
//          formWithErrors =>
//            renderer.render(
//              template = "establishers/addEstablisher.njk",
//              ctx = Json.obj(
//                "form"       -> formWithErrors,
//                "table"      -> table,
//                "radios"     -> Radios.yesNo(formWithErrors("value")),
//                "schemeName" -> existingSchemeName
//              )
//            ).map(BadRequest(_)),
//          value =>
//            Future.successful(Redirect(
//              navigator.nextPage(
//                id          = AddEstablisherId(value),
//                userAnswers = request.userAnswers
//              )
//            ))
//        )
//    }
//}
