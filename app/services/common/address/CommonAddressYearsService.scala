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

package services.common.address

import connectors.cache.UserAnswersCacheConnector
import identifiers.TypedIdentifier
import models.requests.DataRequest
import models.{Mode, NormalMode}
import navigators.CompoundNavigator
import play.api.data.Form
import play.api.data.FormBinding.Implicits._
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.json.{Json, OWrites}
import play.api.mvc.Results.{BadRequest, Ok, Redirect}
import play.api.mvc.{AnyContent, Call, Result}
import views.html.address.AddressYearsView
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendHeaderCarrierProvider
import uk.gov.hmrc.viewmodels.Radios
import utils.{TwirlMigration, UserAnswers}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

@Singleton
class CommonAddressYearsService @Inject()(
   userAnswersCacheConnector: UserAnswersCacheConnector,
   navigator: CompoundNavigator,
   val messagesApi: MessagesApi,
   addressYearsView: AddressYearsView
) extends FrontendHeaderCarrierProvider with I18nSupport {

  private case class TemplateData(
                                   schemeName: Option[String],
                                   entityName: String,
                                   entityType : String,
                                   form : Form[Boolean],
                                   radios: Seq[Radios.Item]
                                 )

  implicit private val formBooleanWrites: OWrites[Form[Boolean]] = OWrites[Form[Boolean]] { form =>
    Json.obj("value" -> form.value)
  }
  implicit private def templateDataWrites(implicit request: DataRequest[AnyContent]): OWrites[TemplateData] = Json.writes[TemplateData]

  def get(schemeName: Option[String],
          entityName: String,
          entityType : String,
          form : Form[Boolean],
          addressYearsId : TypedIdentifier[Boolean],
          submitUrl: Call
         )(implicit request: DataRequest[AnyContent], ec: ExecutionContext): Future[Result] = {
    val filledForm: Form[Boolean] = request.userAnswers.get(addressYearsId).fold(form)(form.fill)
    Future.successful(Ok(
      addressYearsView(
        filledForm,
        entityType,
        entityName,
        TwirlMigration.toTwirlRadios(Radios.yesNo(filledForm("value"))),
        schemeName,
        submitUrl = submitUrl
      )))
  }

  def post(schemeName: Option[String],
           entityName: String,
           entityType : String,
           form : Form[Boolean],
           addressYearsId : TypedIdentifier[Boolean],
           mode: Option[Mode] = None,
           optSetUserAnswers:Option[Boolean => Try[UserAnswers]] = None,
           submitUrl: Call
          )(implicit request: DataRequest[AnyContent], ec: ExecutionContext): Future[Result] = {
    form
      .bindFromRequest()
      .fold(
        formWithErrors => {
          Future.successful(BadRequest(
            addressYearsView(
              formWithErrors,
              entityType,
              entityName,
              TwirlMigration.toTwirlRadios(Radios.yesNo(formWithErrors("value"))),
              schemeName,
              submitUrl = submitUrl
            )))
        },
        value => {
          def defaultSetUserAnswers: Boolean => Try[UserAnswers] = (value: Boolean) => request.userAnswers.set(addressYearsId, value)
          val setUserAnswers = optSetUserAnswers.getOrElse(defaultSetUserAnswers)
          for {
            updatedAnswers <- Future.fromTry(setUserAnswers(value))
            _ <- userAnswersCacheConnector.save(request.lock,updatedAnswers.data)
          } yield {
            val finalMode = mode.getOrElse(NormalMode)
            Redirect(navigator.nextPage(addressYearsId, updatedAnswers, finalMode))
          }
        }
      )
  }

  private def getTemplateData(
                      schemeName: Option[String],
                      entityName: String,
                      entityType : String,
                      form : Form[Boolean]): TemplateData = {

    TemplateData(
      schemeName,
      entityName,
      entityType,
      form,
      Radios.yesNo(form("value"))
    )
  }
}
