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

import controllers.ControllerSpecBase
import controllers.actions.MutableFakeDataRetrievalAction
import forms.trustees.ConfirmDeleteTrusteeFormProvider
import identifiers.trustees.individual.TrusteeNameId
import identifiers.trustees.{AnyTrusteesId, ConfirmDeleteTrusteeId, OtherTrusteesId, TrusteeKindId}
import matchers.JsonMatchers
import models.trustees.TrusteeKind
import models.{Index, PersonName, Scheme}
import org.mockito.ArgumentMatchers.any
import org.mockito.{ArgumentCaptor, ArgumentMatchers}
import play.api.Application
import play.api.data.Form
import play.api.libs.json.{JsObject, JsValue, Json}
import play.api.test.Helpers._
import uk.gov.hmrc.viewmodels.Radios
import utils.Data.{schemeName, ua}
import utils.{Enumerable, TwirlMigration, UserAnswers}
import views.html.DeleteView

import scala.concurrent.Future
class ConfirmDeleteTrusteeControllerSpec extends ControllerSpecBase with JsonMatchers with Enumerable.Implicits {
  private val mutableFakeDataRetrievalAction: MutableFakeDataRetrievalAction = new MutableFakeDataRetrievalAction()

  override def fakeApplication(): Application = applicationBuilderMutableRetrievalAction(mutableFakeDataRetrievalAction).build()

  private val trusteeName: String = "Jane Doe"
  private val index: Index = Index(0)
  private val kind: TrusteeKind = TrusteeKind.Individual
  private val userAnswers: Option[UserAnswers] = ua.set(TrusteeNameId(0), PersonName("Jane", "Doe"))
    .flatMap(_.set(TrusteeNameId(1), PersonName("John", "Bose")))
      .flatMap(_.set(TrusteeKindId(0, kind), kind))
        .flatMap(_.set(TrusteeKindId(1, kind), kind)).toOption

  private val userAnswers1: Option[UserAnswers] = ua.set(TrusteeNameId(0), PersonName("Jane", "Doe"))
    .flatMap(_.set(TrusteeKindId(0, kind), kind)).toOption

  private val form: Form[Boolean] = new ConfirmDeleteTrusteeFormProvider()(trusteeName)

  private def httpPathGET: String = controllers.trustees.routes.ConfirmDeleteTrusteeController.onPageLoad(index, kind).url
  private def httpPathPOST: String = controllers.trustees.routes.ConfirmDeleteTrusteeController.onSubmit(index, kind).url

  private val valuesValid: Map[String, Seq[String]] = Map(
    "value" -> Seq("true")
  )

  private val valuesInvalid: Map[String, Seq[String]] = Map(
    "value" -> Seq.empty
  )

  override def beforeEach(): Unit = {
    super.beforeEach()
  }

  "ConfirmDeleteTrusteeController" must {

    "return OK and the correct view for a GET" in {
      mutableFakeDataRetrievalAction.setDataToReturn(userAnswers)
      val request = httpGETRequest(httpPathGET)
      val result = route(app, request).value

      status(result) mustEqual OK
      val deleteView = app.injector.instanceOf[DeleteView].apply(
        form,
        messages("messages__confirmDeleteTrustee__title"),
        trusteeName,
        None,
        utils.Radios.yesNo(form("value")),
        schemeName,
        routes.ConfirmDeleteTrusteeController.onSubmit(index, kind)
      )(request, messages)
      compareResultAndView(result, deleteView)
    }

    "redirect to Session Expired page for a GET when there is no data" in {
      mutableFakeDataRetrievalAction.setDataToReturn(None)

      val result = route(app, httpGETRequest(httpPathGET)).value

      status(result) mustEqual SEE_OTHER

      redirectLocation(result).value mustBe controllers.preMigration.routes.ListOfSchemesController.onPageLoad(Scheme).url
    }

    "Save data to user answers and redirect to next page when valid data is submitted and remove other trustee id" in {

      val expectedJson = Json.obj()

      when(mockCompoundNavigator.nextPage(ArgumentMatchers.eq(ConfirmDeleteTrusteeId), any(), any())(any()))
        .thenReturn(routes.AddTrusteeController.onPageLoad)
      when(mockUserAnswersCacheConnector.save(any(), any())(any(), any()))
        .thenReturn(Future.successful(Json.obj()))
      mutableFakeDataRetrievalAction.setDataToReturn(userAnswers.map(_.setOrException(OtherTrusteesId, true)))

      val jsonCaptor: ArgumentCaptor[JsObject] = ArgumentCaptor.forClass(classOf[JsObject])

      val result = route(app, httpPOSTRequest(httpPathPOST, valuesValid)).value

      status(result) mustEqual SEE_OTHER

      verify(mockUserAnswersCacheConnector, times(1)).save(any(), jsonCaptor.capture)(any(), any())

      jsonCaptor.getValue must containJson(expectedJson)

      redirectLocation(result) mustBe Some(routes.AddTrusteeController.onPageLoad.url)

      val jsonCaptorUA = ArgumentCaptor.forClass(classOf[JsValue])
      verify(mockUserAnswersCacheConnector, times(1)).save(any(), jsonCaptorUA.capture())(any(), any())
      val actualUA = {
        val jsValue:JsValue = jsonCaptorUA.getValue
        UserAnswers(jsValue.as[JsObject])
      }
      actualUA.get(OtherTrusteesId) mustBe None

    }
    "Save data to user answers and redirect to next page when valid data is submitted and remove other trustee id and any trustees id" in {

      val expectedJson = Json.obj()

      when(mockCompoundNavigator.nextPage(ArgumentMatchers.eq(ConfirmDeleteTrusteeId), any(), any())(any()))
        .thenReturn(routes.AddTrusteeController.onPageLoad)
      when(mockUserAnswersCacheConnector.save(any(), any())(any(), any()))
        .thenReturn(Future.successful(Json.obj()))
      mutableFakeDataRetrievalAction.setDataToReturn(userAnswers1.map(_.setOrException(OtherTrusteesId, true)
        .setOrException(AnyTrusteesId, true)))

      val jsonCaptor: ArgumentCaptor[JsObject] = ArgumentCaptor.forClass(classOf[JsObject])

      val result = route(app, httpPOSTRequest(httpPathPOST, valuesValid)).value

      status(result) mustEqual SEE_OTHER

      verify(mockUserAnswersCacheConnector, times(1)).save(any(), jsonCaptor.capture)(any(), any())

      jsonCaptor.getValue must containJson(expectedJson)

      redirectLocation(result) mustBe Some(routes.AddTrusteeController.onPageLoad.url)

      val jsonCaptorUA = ArgumentCaptor.forClass(classOf[JsValue])
      verify(mockUserAnswersCacheConnector, times(1)).save(any(), jsonCaptorUA.capture())(any(), any())
      val actualUA = {
        val jsValue:JsValue = jsonCaptorUA.getValue
        UserAnswers(jsValue.as[JsObject])
      }
      actualUA.get(OtherTrusteesId) mustBe None
      actualUA.get(AnyTrusteesId) mustBe None
    }

    "return a BAD REQUEST when invalid data is submitted" in {
      mutableFakeDataRetrievalAction.setDataToReturn(userAnswers)

      val result = route(app, httpPOSTRequest(httpPathPOST, valuesInvalid)).value

      status(result) mustEqual BAD_REQUEST

      contentAsString(result) must include(messages("messages__confirmDeleteTrustee__title"))
      contentAsString(result) must include(messages("error.summary.title"))

      verify(mockUserAnswersCacheConnector, times(0)).save(any(), any())(any(), any())
    }

    "redirect back to list of schemes for a POST when there is no data" in {
      mutableFakeDataRetrievalAction.setDataToReturn(None)

      val result = route(app, httpPOSTRequest(httpPathPOST, valuesValid)).value

      status(result) mustEqual SEE_OTHER

      redirectLocation(result).value mustBe controllers.preMigration.routes.ListOfSchemesController.onPageLoad(Scheme).url
    }
  }
}
