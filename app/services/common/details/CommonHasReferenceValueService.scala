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

package services.common.details

import connectors.cache.UserAnswersCacheConnector
import identifiers.TypedIdentifier
import models.Mode
import models.requests.DataRequest
import navigators.CompoundNavigator
import play.api.data.Form
import play.api.data.FormBinding.Implicits._
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.json.{Json, OWrites}
import play.api.mvc.Results.{BadRequest, Ok, Redirect}
import play.api.mvc.{AnyContent, MessagesControllerComponents, Result}
import renderer.Renderer
import uk.gov.hmrc.nunjucks.NunjucksSupport
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendHeaderCarrierProvider
import uk.gov.hmrc.viewmodels.Radios
import uk.gov.hmrc.viewmodels.Radios.Item
import utils.UserAnswers

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

@Singleton
class CommonHasReferenceValueService @Inject()(val controllerComponents: MessagesControllerComponents,
                                               val renderer: Renderer,
                                               val userAnswersCacheConnector: UserAnswersCacheConnector,
                                               val navigator: CompoundNavigator,
                                               val messagesApi: MessagesApi
                                              ) extends NunjucksSupport with FrontendHeaderCarrierProvider with I18nSupport {

  protected def templateName(paragraphText: Seq[String]): String =
    if (paragraphText.nonEmpty) "hasReferenceValueWithHint.njk" else "hasReferenceValue.njk"

  private case class TemplateData(pageTitle: String,
                                  pageHeading: String,
                                  isPageHeading: Boolean,
                                  id: TypedIdentifier[Boolean],
                                  form: Form[Boolean],
                                  radios: Seq[Item],
                                  schemeName: String,
                                  paragraphs: Seq[String] = Seq(),
                                  legendClass: String = "govuk-fieldset__legend--s")

  implicit private def templateDataWrites(implicit request: DataRequest[AnyContent]): OWrites[TemplateData] = Json.writes[TemplateData]

  def get(
           pageTitle: String,
           pageHeading: String,
           isPageHeading: Boolean,
           id: TypedIdentifier[Boolean],
           form: Form[Boolean],
           schemeName: String,
           paragraphText: Seq[String] = Seq(),
           legendClass: String = "govuk-fieldset__legend--s"
         )(implicit request: DataRequest[AnyContent], ec: ExecutionContext): Future[Result] = {

    val preparedForm: Form[Boolean] =
      request.userAnswers.get[Boolean](id) match {
        case Some(value) => form.fill(value)
        case _ => form
      }
    renderer.render(
      template = templateName(paragraphText),
      getTemplateData(pageTitle, pageHeading, isPageHeading, id, preparedForm, schemeName, paragraphText, legendClass)
    ).map(Ok(_))
  }
  def post(pageTitle: String,
           pageHeading: String,
           isPageHeading: Boolean,
           id: TypedIdentifier[Boolean],
           form: Form[Boolean],
           schemeName: String,
           paragraphText: Seq[String] = Seq(),
           legendClass: String = "govuk-fieldset__legend--s",
           mode: Mode,
           optSetUserAnswers: Option[Boolean => Try[UserAnswers]] = None
          )(implicit request: DataRequest[AnyContent], ec: ExecutionContext): Future[Result] =

    form.bindFromRequest().fold(
      (formWithErrors: Form[Boolean]) =>
        renderer.render(
          template = templateName(paragraphText),
          getTemplateData(pageTitle, pageHeading, isPageHeading, id, formWithErrors, schemeName, paragraphText, legendClass))
          .map(BadRequest(_)),
      value => {
        def defaultSetUserAnswers = (value: Boolean) =>
          request.userAnswers.set(id, value)
        val setUserAnswers = optSetUserAnswers.getOrElse(defaultSetUserAnswers)

        for {
          updatedAnswers <- Future.fromTry(setUserAnswers(value))
          _ <- userAnswersCacheConnector.save(request.lock, updatedAnswers.data)
        } yield
          Redirect(navigator.nextPage(id, updatedAnswers, mode))
      }
    )

  private def getTemplateData(pageTitle: String,
                              pageHeading: String,
                              isPageHeading: Boolean,
                              id: TypedIdentifier[Boolean],
                              form: Form[Boolean],
                              schemeName: String,
                              paragraphText: Seq[String] = Seq(),
                              legendClass: String = "govuk-fieldset__legend--s"
                             ): TemplateData = {
    TemplateData(
      pageTitle,
      pageHeading,
      isPageHeading,
      id,
      form,
      Radios.yesNo(form("value")),
      schemeName,
      paragraphText,
      legendClass)
  }
}