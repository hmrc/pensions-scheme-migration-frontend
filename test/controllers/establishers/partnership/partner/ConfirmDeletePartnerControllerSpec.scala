/*
 * Copyright 2022 HM Revenue & Customs
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

package controllers.establishers.partnership.partner

import controllers.ControllerSpecBase
import controllers.actions.MutableFakeDataRetrievalAction
import forms.establishers.ConfirmDeleteEstablisherFormProvider
import identifiers.establishers.partnership.partner.{ConfirmDeletePartnerId, PartnerNameId}
import matchers.JsonMatchers
import models.{Index, NormalMode, PersonName, Scheme}
import org.mockito.ArgumentMatchers.any
import org.mockito.{ArgumentCaptor, ArgumentMatchers}
import play.api.Application
import play.api.data.Form
import play.api.libs.json.{JsObject, Json}
import play.api.test.Helpers._
import play.twirl.api.Html
import uk.gov.hmrc.nunjucks.NunjucksSupport
import uk.gov.hmrc.viewmodels.Radios
import utils.Data.{schemeName, ua}
import utils.{Enumerable, UserAnswers}

import scala.concurrent.Future

class ConfirmDeletePartnerControllerSpec extends ControllerSpecBase with NunjucksSupport with JsonMatchers with Enumerable.Implicits {
  private val partnerName: String = "Jane Doe"
  private val establisherIndex: Index = Index(0)
  private val dirIndex: Index = Index(0)
  private val userAnswersPartner: Option[UserAnswers] = ua.set(PartnerNameId(establisherIndex,dirIndex), PersonName("Jane", "Doe")).toOption
  private val templateToBeRendered = "delete.njk"
  private val form: Form[Boolean] = new ConfirmDeleteEstablisherFormProvider()(partnerName)

  private val mutableFakeDataRetrievalAction: MutableFakeDataRetrievalAction = new MutableFakeDataRetrievalAction()

  private val application: Application = applicationBuilderMutableRetrievalAction(mutableFakeDataRetrievalAction).build()

  private def httpPathGET(partnerIndex: Index): String = controllers.establishers.partnership.partner.routes.ConfirmDeletePartnerController.onPageLoad(establisherIndex,partnerIndex).url
  private def httpPathPOST(partnerIndex: Index): String =controllers.establishers.partnership.partner.routes.ConfirmDeletePartnerController.onSubmit(establisherIndex,partnerIndex).url

  private val valuesValid: Map[String, Seq[String]] = Map(
    "value" -> Seq("true")
  )

  private val valuesInvalid: Map[String, Seq[String]] = Map(
    "value" -> Seq.empty
  )

  private def submitUrl(partnerIndex:Index) = routes.ConfirmDeletePartnerController.onSubmit(establisherIndex, partnerIndex).url

  private def jsonToPassToTemplate(partnerIndex:Index, partnerName: String): Form[Boolean] => JsObject = form =>
  Json.obj(
    "form" -> form,
    "titleMessage" -> messages("messages__confirmDeletePartners__title"),
    "name" -> partnerName,
    "radios" -> Radios.yesNo(form("value")),
    "submitUrl" -> submitUrl(partnerIndex),
    "schemeName" -> schemeName
  )

  override def beforeEach: Unit = {
    super.beforeEach
    when(mockRenderer.render(any(), any())(any())).thenReturn(Future.successful(Html("")))
  }

  private val templateCaptor : ArgumentCaptor[String] = ArgumentCaptor.forClass(classOf[String])
  private val jsonCaptor: ArgumentCaptor[JsObject] = ArgumentCaptor.forClass(classOf[JsObject])

  "ConfirmDeletePartnerController" must {

    "return OK and the correct view for a GET partner" in {
      mutableFakeDataRetrievalAction.setDataToReturn(userAnswersPartner)
      val result = route(application, httpGETRequest(httpPathGET(dirIndex))).value

      status(result) mustEqual OK

      verify(mockRenderer, times(1)).render(templateCaptor.capture(), jsonCaptor.capture())(any())

      templateCaptor.getValue mustEqual templateToBeRendered
      jsonCaptor.getValue must containJson(jsonToPassToTemplate(dirIndex, partnerName)(form))
    }

    "Save data to user answers and redirect to next page when valid data is submitted for partner" in {
      val expectedJson = Json.obj()

      when(mockCompoundNavigator.nextPage(ArgumentMatchers.eq(ConfirmDeletePartnerId(dirIndex)), any(), any())(any()))
        .thenReturn(controllers.establishers.partnership.routes.AddPartnersController.onPageLoad(establisherIndex,NormalMode))
      when(mockUserAnswersCacheConnector.save(any(), any())(any(), any()))
        .thenReturn(Future.successful(Json.obj()))

      mutableFakeDataRetrievalAction.setDataToReturn(userAnswersPartner)

      val result = route(application, httpPOSTRequest(httpPathPOST(dirIndex), valuesValid)).value

      status(result) mustEqual SEE_OTHER
      verify(mockUserAnswersCacheConnector, times(1)).save(any(), jsonCaptor.capture)(any(), any())
      jsonCaptor.getValue must containJson(expectedJson)
      redirectLocation(result) mustBe Some(controllers.establishers.partnership.routes.AddPartnersController.onPageLoad(establisherIndex,NormalMode).url)
    }


    "return a BAD REQUEST when invalid data is submitted" in {
      mutableFakeDataRetrievalAction.setDataToReturn(userAnswersPartner)

      val result = route(application, httpPOSTRequest(httpPathPOST(dirIndex), valuesInvalid)).value

      status(result) mustEqual BAD_REQUEST

      verify(mockUserAnswersCacheConnector, times(0)).save(any(), any())(any(), any())
    }

    "redirect back to list of schemes for a POST when there is no data" in {
      mutableFakeDataRetrievalAction.setDataToReturn(None)

      val result = route(application, httpPOSTRequest(httpPathPOST(dirIndex), valuesValid)).value

      status(result) mustEqual SEE_OTHER

      redirectLocation(result).value mustBe controllers.preMigration.routes.ListOfSchemesController.onPageLoad(Scheme).url
    }
  }
}
