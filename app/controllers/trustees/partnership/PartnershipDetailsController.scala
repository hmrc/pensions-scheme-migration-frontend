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

package controllers.trustees.partnership

import connectors.cache.UserAnswersCacheConnector
import controllers.Retrievals
import controllers.actions.{AuthAction, DataRequiredAction, DataRetrievalAction}
import forms.PartnershipDetailsFormProvider
import identifiers.trustees.partnership.PartnershipDetailsId
import identifiers.trustees.{IsTrusteeNewId, TrusteeKindId}
import models.trustees.TrusteeKind
import models.{Index, PartnershipDetails}
import navigators.CompoundNavigator
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.Enumerable
import views.html.PartnershipDetailsView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class PartnershipDetailsController @Inject()(
                                              override val messagesApi: MessagesApi,
                                              val navigator: CompoundNavigator,
                                              authenticate: AuthAction,
                                              getData: DataRetrievalAction,
                                              requireData: DataRequiredAction,
                                              formProvider: PartnershipDetailsFormProvider,
                                              val controllerComponents: MessagesControllerComponents,
                                              userAnswersCacheConnector: UserAnswersCacheConnector,
                                              partnershipDetailsView: PartnershipDetailsView
                                        )(implicit val executionContext: ExecutionContext)
  extends FrontendBaseController
    with I18nSupport
    with Retrievals
    with Enumerable.Implicits
    {

  private val form = formProvider()

  def onPageLoad(index: Index): Action[AnyContent] =
    (authenticate andThen getData andThen requireData()).async {
      implicit request =>
        Future.successful(Ok(
          partnershipDetailsView(
            request.userAnswers.get[PartnershipDetails](PartnershipDetailsId(index)).fold(form)(form.fill),
            existingSchemeName.getOrElse(""),
            routes.PartnershipDetailsController.onSubmit(index)
          )
        ))
    }

  def onSubmit(index: Index): Action[AnyContent] =
    (authenticate andThen getData andThen requireData()).async {
      implicit request =>
        form.bindFromRequest().fold(
          (formWithErrors: Form[_]) =>
          Future.successful(BadRequest(
            partnershipDetailsView(
            formWithErrors,
              existingSchemeName.getOrElse(""),
              routes.PartnershipDetailsController.onSubmit(index)
            ))),
          value =>
            for {
              updatedAnswers <- Future.fromTry(request.userAnswers.set(PartnershipDetailsId(index), value)
                .flatMap(_.set(IsTrusteeNewId(index), value = true))
                .flatMap(_.set(TrusteeKindId(index, TrusteeKind.Partnership), TrusteeKind.Partnership))
              )
              _ <- userAnswersCacheConnector.save(request.lock, updatedAnswers.data)
            } yield
              Redirect(navigator.nextPage(PartnershipDetailsId(index), updatedAnswers))
        )
    }
}
