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

package controllers.racdac.bulk

import connectors.cache.CurrentPstrCacheConnector
import connectors.{AncillaryPsaException, ListOfSchemes5xxException, ListOfSchemesConnector, MinimalDetailsConnector}
import controllers.ControllerSpecBase
import controllers.actions.FakeAuthAction
import forms.YesNoFormProvider
import matchers.JsonMatchers
import models.{Items, ListOfLegacySchemes}
import org.mockito.ArgumentMatchers.any
import org.scalatest.{BeforeAndAfterEach, TryValues}
import org.mockito.Mockito._
import play.api.data.Form
import play.api.mvc.Results.Ok
import play.api.mvc.{AnyContentAsFormUrlEncoded, Request, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers.{status, _}
import uk.gov.hmrc.govukfrontend.views.viewmodels.radios.RadioItem
import utils.Data.ua

import scala.concurrent.Future
class TransferAllControllerSpec extends ControllerSpecBase with JsonMatchers with TryValues with BeforeAndAfterEach{

  private val psaName: String = "Psa Name"
  private val formProvider: YesNoFormProvider = new YesNoFormProvider()
  private val mockMinDetailsConnector: MinimalDetailsConnector = mock[MinimalDetailsConnector]
  private val mockListOfSchemesConnector: ListOfSchemesConnector = mock[ListOfSchemesConnector]
  private val mockCurrentPstrCacheConnector: CurrentPstrCacheConnector = mock[CurrentPstrCacheConnector]

  private val schemeDetail = Items("10000678RE", "2020-10-10", racDac = false, "abcdefghi", "2020-12-12", None)
  private val racDacDetail = Items("10000678RF", "2020-10-10", racDac = true, "abcdefghi", "2020-12-12", Some("12345678"))
  private val expectedResponseWithScheme = ListOfLegacySchemes(1, Some(List(schemeDetail)))
  private val expectedResponse = ListOfLegacySchemes(1, Some(List( racDacDetail)))
  private val expectedResponseWithEmpty = ListOfLegacySchemes(0, None)


  private val form: Form[Boolean] = formProvider(messages("messages__transferAll__error"))

  private def getView(req: Request[_], radios: Seq[RadioItem], form: Form[_]) = {
    app.injector.instanceOf[views.html.racdac.TransferAllView].apply(
      form,
      routes.TransferAllController.onSubmit,
      appConfig.psaOverviewUrl,
      psaName,
      radios
    )(req, implicitly)
  }
  override def beforeEach(): Unit = {
    reset(mockUserAnswersCacheConnector)
    when(mockMinDetailsConnector.getPSAName(any(), any())) thenReturn Future.successful(psaName)
  }

  private def controller: TransferAllController =
    new TransferAllController(appConfig, messagesApi, new FakeAuthAction(), formProvider, mockMinDetailsConnector,
      mockListOfSchemesConnector, mockCurrentPstrCacheConnector, controllerComponents, app.injector.instanceOf[views.html.racdac.TransferAllView])

  "TransferAllController" must {

    "redirect to the cannot migrate page when AncillaryPSAException is thrown" in {
      reset(mockListOfSchemesConnector)
      when(mockListOfSchemesConnector.getListOfSchemes(any())(any(),any())).thenReturn(Future.failed(AncillaryPsaException()))
      val result = controller.onPageLoad(fakeDataRequest(ua))
      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(controllers.preMigration.routes.CannotMigrateController.onPageLoad.url)
    }

    "redirect to the 'There is a problem' page when ListOfSchemes5xxException is thrown" in {
      reset(mockListOfSchemesConnector)
      when(mockListOfSchemesConnector.getListOfSchemes(any())(any(),any())).thenReturn(Future.failed(ListOfSchemes5xxException()))
      val result = controller.onPageLoad(fakeDataRequest(ua))
      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(controllers.preMigration.routes.ThereIsAProblemController.onPageLoad.url)
    }

    "return OK and the correct view for a GET" in{
      when(mockListOfSchemesConnector.getListOfSchemes(any())(any(),any())).thenReturn(Future.successful(Right(expectedResponse)))
      val req = fakeDataRequest(ua)
      val result: Future[Result] = controller.onPageLoad(req)

      status(result) mustBe OK
      compareResultAndView(result,
        getView(
          req,
          utils.Radios.yesNo(form("value")),
          form
        )
      )
    }

    "return OK and the correct view for a GET for scheme with Scheme Only" in {

      when(mockListOfSchemesConnector.getListOfSchemes(any())(any(),any())).thenReturn(Future.successful(Right(expectedResponseWithScheme)))
      val result: Future[Result] = controller.onPageLoad(fakeDataRequest())
      status(result) mustEqual SEE_OTHER
      redirectLocation(result) mustBe Some(controllers.preMigration.routes.NoSchemeToAddController.onPageLoadRacDac.url)
    }
    "return OK and the correct view for a GET for scheme with returning empty list " in {

      when(mockListOfSchemesConnector.getListOfSchemes(any())(any(),any())).thenReturn(Future.successful(Right(expectedResponseWithEmpty)))
      val result: Future[Result] = controller.onPageLoad(fakeDataRequest())
      status(result) mustEqual SEE_OTHER
      redirectLocation(result) mustBe Some(controllers.preMigration.routes.NoSchemeToAddController.onPageLoadRacDac.url)
    }
    "remove the existing cached data and redirect to the next page when valid data is submitted" in {
      val request: FakeRequest[AnyContentAsFormUrlEncoded] = fakeRequest.withFormUrlEncodedBody(("value", "true"))
      when(mockCurrentPstrCacheConnector.remove(any(), any())).thenReturn(Future.successful(Ok("")))
      val result: Future[Result] = controller.onSubmit(request)

      status(result) mustBe SEE_OTHER

      redirectLocation(result) mustBe Some(routes.BulkListController.onPageLoad.url)
      verify(mockCurrentPstrCacheConnector, times(1)).remove(any(), any())
    }

    "return a Bad Request and errors when invalid data is submitted" in {
      val request = fakeRequest.withFormUrlEncodedBody(("value", "invalid value"))

      val result: Future[Result] = controller.onSubmit(request)

      val boundForm = form.bind(Map("value" -> "invalid value"))

      status(result) mustBe BAD_REQUEST
      compareResultAndView(result,
        getView(
          request,
          utils.Radios.yesNo(boundForm.apply("value")),
          boundForm
        )
      )
    }

  }

}
