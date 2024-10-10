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

package controllers.establishers.partnership.partner

import controllers.ControllerSpecBase
import controllers.actions.MutableFakeDataRetrievalAction
import forms.establishers.ConfirmDeleteEstablisherFormProvider
import identifiers.establishers.partnership.partner.{ConfirmDeletePartnerId, PartnerNameId}
import matchers.JsonMatchers
import models.{Index, NormalMode, PersonName, Scheme}
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import play.api.Application
import play.api.data.Form
import play.api.libs.json.Json
import play.api.test.Helpers._
import uk.gov.hmrc.viewmodels.Radios
import utils.Data.{schemeName, ua}
import utils.{Enumerable, TwirlMigration, UserAnswers}
import views.html.DeleteView

import scala.concurrent.Future

class ConfirmDeletePartnerControllerSpec extends ControllerSpecBase with JsonMatchers with Enumerable.Implicits {
  private val mutableFakeDataRetrievalAction: MutableFakeDataRetrievalAction = new MutableFakeDataRetrievalAction()
  override def fakeApplication(): Application = applicationBuilderMutableRetrievalAction(mutableFakeDataRetrievalAction).build()

  private val partnerName: String = "Jane Doe"
  private val establisherIndex: Index = Index(0)
  private val dirIndex: Index = Index(0)
  private val userAnswersPartner: Option[UserAnswers] = ua.set(PartnerNameId(establisherIndex, dirIndex), PersonName("Jane", "Doe")).toOption
  private val form: Form[Boolean] = new ConfirmDeleteEstablisherFormProvider()(partnerName)

  private def httpPathGET(partnerIndex: Index): String = controllers.establishers.partnership.partner.routes.ConfirmDeletePartnerController
      .onPageLoad(establisherIndex,partnerIndex).url
  private def httpPathPOST(partnerIndex: Index): String =controllers.establishers.partnership.partner.routes.ConfirmDeletePartnerController
    .onSubmit(establisherIndex,partnerIndex).url

  private val valuesValid: Map[String, Seq[String]] = Map(
    "value" -> Seq("true")
  )

  private val valuesInvalid: Map[String, Seq[String]] = Map(
    "value" -> Seq.empty
  )

  override def beforeEach(): Unit = {
    super.beforeEach()
  }

  "ConfirmDeletePartnerController" must {

    "return OK and the correct view for a GET partner" in {
      mutableFakeDataRetrievalAction.setDataToReturn(userAnswersPartner)

      val request = httpGETRequest(httpPathGET(dirIndex))
      val result = route(app, request).value

      status(result) mustEqual OK

      val deleteView = app.injector.instanceOf[DeleteView].apply(
        form,
        messages("messages__confirmDeletePartners__title"),
        partnerName,
        None,
        TwirlMigration.toTwirlRadios(Radios.yesNo(form("value"))),
        schemeName,
        routes.ConfirmDeletePartnerController.onSubmit(establisherIndex, dirIndex)
      )(request, messages)
      compareResultAndView(result, deleteView)

    }

    "Save data to user answers and redirect to next page when valid data is submitted for partner" in {
      when(mockCompoundNavigator.nextPage(ArgumentMatchers.eq(ConfirmDeletePartnerId(dirIndex)), any(), any())(any()))
        .thenReturn(controllers.establishers.partnership.routes.AddPartnersController.onPageLoad(establisherIndex,NormalMode))
      when(mockUserAnswersCacheConnector.save(any(), any())(any(), any()))
        .thenReturn(Future.successful(Json.obj()))

      mutableFakeDataRetrievalAction.setDataToReturn(userAnswersPartner)

      val result = route(app, httpPOSTRequest(httpPathPOST(dirIndex), valuesValid)).value

      status(result) mustEqual SEE_OTHER
      redirectLocation(result) mustBe Some(controllers.establishers.partnership.routes.AddPartnersController.onPageLoad(establisherIndex,NormalMode).url)
    }


    "return a BAD REQUEST when invalid data is submitted" in {
      mutableFakeDataRetrievalAction.setDataToReturn(userAnswersPartner)

      val result = route(app, httpPOSTRequest(httpPathPOST(dirIndex), valuesInvalid)).value

      status(result) mustEqual BAD_REQUEST
      contentAsString(result) must include(messages("messages__confirmDeletePartners__title"))
      contentAsString(result) must include(messages("error.summary.title"))
      verify(mockUserAnswersCacheConnector, times(0)).save(any(), any())(any(), any())
    }

    "redirect back to list of schemes for a POST when there is no data" in {
      mutableFakeDataRetrievalAction.setDataToReturn(None)

      val result = route(app, httpPOSTRequest(httpPathPOST(dirIndex), valuesValid)).value

      status(result) mustEqual SEE_OTHER

      redirectLocation(result).value mustBe controllers.preMigration.routes.ListOfSchemesController.onPageLoad(Scheme).url
    }
  }
}
