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

import config.AppConfig
import connectors.cache.UserAnswersCacheConnector
import controllers.Retrievals
import controllers.actions.*
import forms.trustees.AddTrusteeFormProvider
import identifiers.beforeYouStart.SchemeTypeId
import identifiers.trustees.{AddTrusteeId, IsTrusteeNewId, TrusteeKindId, TrusteesId}
import models.requests.DataRequest
import models.{SchemeType, Trustee}
import navigators.CompoundNavigator
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.json.JsObject
import play.api.mvc.*
import play.twirl.api.Html
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.UserAnswers

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class AddTrusteeController @Inject()(override val messagesApi: MessagesApi,
                                     navigator: CompoundNavigator,
                                     authenticate: AuthAction,
                                     getData: DataRetrievalAction,
                                     requireData: DataRequiredAction,
                                     formProvider: AddTrusteeFormProvider,
                                     config: AppConfig,
                                     userAnswersCacheConnector: UserAnswersCacheConnector,
                                     val controllerComponents: MessagesControllerComponents,
                                     view: views.html.trustees.AddTrusteeView
                                    )(implicit val ec: ExecutionContext)
  extends FrontendBaseController with Retrievals with I18nSupport {

  def onPageLoad: Action[AnyContent] =
    (authenticate andThen getData andThen requireData()).async {
      implicit request =>
        val userAnswersWithCleanedTrustees: JsObject =
          request.userAnswers.removeEmptyObjectsAndIncompleteEntities(
            collectionKey = TrusteesId.toString,
            keySet = Set(IsTrusteeNewId.toString, TrusteeKindId.toString)
          )

        userAnswersCacheConnector.save(request.lock, userAnswersWithCleanedTrustees).map { _ =>
          val trustees: Seq[Trustee[?]] =
            UserAnswers(userAnswersWithCleanedTrustees).allTrusteesAfterDelete
          val isSingleTrust: Boolean =
            request.userAnswers.get(SchemeTypeId).contains(SchemeType.SingleTrust)

          (trustees, isSingleTrust) match {
            case (Nil, false) =>
              Redirect(routes.AnyTrusteesController.onPageLoad)
            case (Nil, true) =>
              Redirect(routes.NoTrusteesController.onPageLoad)
            case _ =>
              Ok(getView(formProvider(trustees), trustees))
          }
        }
    }

  def onSubmit: Action[AnyContent] =
    (authenticate andThen getData andThen requireData()) {
      implicit request =>
        def navNextPage(v: Option[Boolean]): Result =
          Redirect(navigator.nextPage(AddTrusteeId(v), request.userAnswers))

        val trustees: Seq[Trustee[?]] =
          request.userAnswers.allTrusteesAfterDelete

        formProvider(trustees).bindFromRequest().fold(
          formWithErrors =>
            BadRequest(getView(formWithErrors, trustees)),
          value =>
            if (trustees.length >= config.maxTrustees) navNextPage(None) else navNextPage(value)
        )
    }

  private def getView(form: Form[?], trustees: Seq[Trustee[?]])
                     (implicit request: DataRequest[AnyContent]): Html =
    view(
      form               = form,
      submitCall         = controllers.trustees.routes.AddTrusteeController.onSubmit,
      schemeName         = existingSchemeName.getOrElse(throw new RuntimeException("Scheme name not available")),
      itemListIncomplete = trustees.filterNot(_.isCompleted),
      itemListComplete   = trustees.filter(_.isCompleted),
      trusteeSize        = trustees.size,
      maxTrustees        = config.maxTrustees,
      radios             = utils.Radios.yesNo(form("value"))
    )
}
