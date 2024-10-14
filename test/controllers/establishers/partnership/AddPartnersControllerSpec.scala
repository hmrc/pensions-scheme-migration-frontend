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

package controllers.establishers.partnership

import controllers.ControllerSpecBase
import controllers.actions.MutableFakeDataRetrievalAction
import forms.establishers.partnership.partner.AddPartnersFormProvider
import identifiers.establishers.EstablisherKindId
import identifiers.establishers.partnership.partner.{IsNewPartnerId, PartnerNameId}
import identifiers.establishers.partnership.{AddPartnersId, PartnershipDetailsId}
import matchers.JsonMatchers
import models.establishers.EstablisherKind
import models.{NormalMode, PartnerEntity, PersonName, Scheme}
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import play.api.Application
import play.api.test.Helpers._
import uk.gov.hmrc.viewmodels.Radios
import utils.Data.{partnershipDetails, schemeName, ua}
import utils.{Enumerable, TwirlMigration, UserAnswers}
import views.html.establishers.partnership.AddPartnerView

class AddPartnersControllerSpec extends ControllerSpecBase with JsonMatchers with Enumerable.Implicits {
  private val partnerName: PersonName =
    PersonName("Jane", "Doe")
  private val userAnswers: Option[UserAnswers] =
   ua.set(EstablisherKindId(0), EstablisherKind.Partnership).flatMap(
     _.set(PartnershipDetailsId(0), partnershipDetails).flatMap(
        _.set(PartnerNameId(0,0), partnerName).flatMap(
          _.set(IsNewPartnerId(0,0), true)
     ))).toOption

  private def validData = {
    ua.set(EstablisherKindId(1), EstablisherKind.Partnership).flatMap(
      _.set(PartnershipDetailsId(1), partnershipDetails).flatMap(
        _.set(PartnerNameId(1,1), partnerName).flatMap(
        _.set(PartnerNameId(1,2), partnerName).flatMap(
        _.set(PartnerNameId(1,3), partnerName).flatMap(
        _.set(PartnerNameId(1,4), partnerName).flatMap(
        _.set(PartnerNameId(1,5), partnerName).flatMap(
        _.set(PartnerNameId(1,6), partnerName).flatMap(
        _.set(PartnerNameId(1,7), partnerName).flatMap(
        _.set(PartnerNameId(1,8), partnerName).flatMap(
        _.set(PartnerNameId(1,9), partnerName).flatMap(
          _.set(PartnerNameId(1,10), partnerName).flatMap(
          _.set(IsNewPartnerId(1,1), false)
        )))))))))))).toOption
  }

  private val formProvider = new AddPartnersFormProvider()
  private val form         = formProvider()

  private val mutableFakeDataRetrievalAction: MutableFakeDataRetrievalAction = new MutableFakeDataRetrievalAction()
  private val application: Application = applicationBuilderMutableRetrievalAction(mutableFakeDataRetrievalAction).build()

  private def httpPathGET: String = controllers.establishers.partnership.routes.AddPartnersController.onPageLoad(0,NormalMode).url
  private def httpPathPOST: String = controllers.establishers.partnership.routes.AddPartnersController.onSubmit(0,NormalMode).url

  private val valuesValid: Map[String, Seq[String]] = Map(
    "value" -> Seq("true")
  )

  private val valuesInvalid: Map[String, Seq[String]] = Map(
    "value" -> Seq("invalid")
  )

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockAppConfig)
    when(mockAppConfig.maxPartners).thenReturn(10)
  }


  "AddPartnersController" must {

    "return OK and the correct view for a GET" in {
      mutableFakeDataRetrievalAction.setDataToReturn(userAnswers)

      val request = httpGETRequest(httpPathGET)
      val result = route(application, request).value

      status(result) mustEqual OK

      val view = application.injector.instanceOf[AddPartnerView].apply(
        form,
        schemeName,
        1,
        mockAppConfig.maxPartners,
        Seq(PartnerEntity(PartnerNameId(0,0), partnerName.fullName, false, false, true, 1)),
        utils.Radios.yesNo(form("value")),
        controllers.establishers.partnership.routes.AddPartnersController.onSubmit(0,NormalMode)
      )(request, messages)

      compareResultAndView(result, view)
    }

    "redirect back to list of schemes for a GET when there is no data" in {
      mutableFakeDataRetrievalAction.setDataToReturn(None)

      val result = route(application, httpGETRequest(httpPathGET)).value

      status(result) mustEqual SEE_OTHER

      redirectLocation(result).value mustBe controllers.preMigration.routes.ListOfSchemesController.onPageLoad(Scheme).url
    }

    "Save data to user answers and redirect to next page when valid data is submitted" in {

      when(mockCompoundNavigator.nextPage(ArgumentMatchers.eq(AddPartnersId(0)), any(), any())(any()))
        .thenReturn(routes.AddPartnersController.onPageLoad(0,NormalMode))

      mutableFakeDataRetrievalAction.setDataToReturn(userAnswers)

      val result = route(application, httpPOSTRequest(httpPathPOST, valuesValid)).value

      status(result) mustEqual SEE_OTHER

      redirectLocation(result) mustBe Some(routes.AddPartnersController.onPageLoad(0,NormalMode).url)
    }

    "redirect to the next page when maximum partners exist and the user submits" in {
      mutableFakeDataRetrievalAction.setDataToReturn(validData)
      val result = route(application, httpPOSTRequest(httpPathPOST, valuesValid)).value

      status(result) mustEqual SEE_OTHER

      redirectLocation(result).value mustBe controllers.preMigration.routes.ListOfSchemesController.onPageLoad(Scheme).url
    }

    "return a BAD REQUEST when invalid data is submitted" in {

      mutableFakeDataRetrievalAction.setDataToReturn(userAnswers)

      val result = route(application, httpPOSTRequest(httpPathPOST, valuesInvalid)).value

      status(result) mustEqual BAD_REQUEST

      verify(mockUserAnswersCacheConnector, times(0)).save(any(), any())(any(), any())
    }

    "redirect back to list of schemes for a POST when there is no data" in {
      mutableFakeDataRetrievalAction.setDataToReturn(None)

      val result = route(application, httpPOSTRequest(httpPathPOST, valuesValid)).value

      status(result) mustEqual SEE_OTHER

      redirectLocation(result).value mustBe controllers.preMigration.routes.ListOfSchemesController.onPageLoad(Scheme).url
    }
  }
}
