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

package controllers.trustees

import controllers.ControllerSpecBase
import controllers.actions.MutableFakeDataRetrievalAction
import forms.trustees.ConfirmDeleteTrusteeFormProvider
import identifiers.trustees.{AnyTrusteesId, ConfirmDeleteTrusteeId, OtherTrusteesId, TrusteeKindId}
import identifiers.trustees.individual.TrusteeNameId
import matchers.JsonMatchers
import models.trustees.TrusteeKind
import models.{Index, PersonName}
import org.mockito.ArgumentMatchers.any
import org.mockito.{ArgumentCaptor, ArgumentMatchers}
import play.api.Application
import play.api.data.Form
import play.api.libs.json.{JsObject, JsValue, Json}
import play.api.test.Helpers._
import play.twirl.api.Html
import uk.gov.hmrc.nunjucks.NunjucksSupport
import uk.gov.hmrc.viewmodels.Radios
import utils.Data.{schemeName, ua}
import utils.{Enumerable, UserAnswers}
import models.Scheme

import scala.concurrent.Future

class ConfirmDeleteTrusteeControllerSpec extends ControllerSpecBase with NunjucksSupport with JsonMatchers with Enumerable.Implicits {
  private val trusteeName: String = "Jane Doe"
  private val index: Index = Index(0)
  private val kind: TrusteeKind = TrusteeKind.Individual
  private val userAnswers: Option[UserAnswers] = ua.set(TrusteeNameId(0), PersonName("Jane", "Doe"))
    .flatMap(_.set(TrusteeNameId(1), PersonName("John", "Bose")))
      .flatMap(_.set(TrusteeKindId(0), kind))
        .flatMap(_.set(TrusteeKindId(1), kind)).toOption

  private val userAnswers1: Option[UserAnswers] = ua.set(TrusteeNameId(0), PersonName("Jane", "Doe"))
    .flatMap(_.set(TrusteeKindId(0), kind)).toOption

  private val templateToBeRendered = "delete.njk"
  private val form: Form[Boolean] = new ConfirmDeleteTrusteeFormProvider()(trusteeName)

  private val mutableFakeDataRetrievalAction: MutableFakeDataRetrievalAction = new MutableFakeDataRetrievalAction()

  private val application: Application = applicationBuilderMutableRetrievalAction(mutableFakeDataRetrievalAction).build()

  private def httpPathGET: String = controllers.trustees.routes.ConfirmDeleteTrusteeController.onPageLoad(index, kind).url
  private def httpPathPOST: String = controllers.trustees.routes.ConfirmDeleteTrusteeController.onSubmit(index, kind).url

  private val valuesValid: Map[String, Seq[String]] = Map(
    "value" -> Seq("true")
  )

  private val valuesInvalid: Map[String, Seq[String]] = Map(
    "value" -> Seq.empty
  )

  private val jsonToPassToTemplate: Form[Boolean] => JsObject = form =>
  Json.obj(
    "form" -> form,
    "titleMessage" -> messages("messages__confirmDeleteTrustee__title"),
    "name" -> trusteeName,
    "radios" -> Radios.yesNo(form("value")),
    "submitUrl" -> routes.ConfirmDeleteTrusteeController.onSubmit(index, kind).url,
    "schemeName" -> schemeName
  )

  override def beforeEach: Unit = {
    super.beforeEach
    when(mockRenderer.render(any(), any())(any())).thenReturn(Future.successful(Html("")))
  }


  "ConfirmDeleteTrusteeController" must {

    "return OK and the correct view for a GET" in {

      mutableFakeDataRetrievalAction.setDataToReturn(userAnswers)
      val templateCaptor : ArgumentCaptor[String] = ArgumentCaptor.forClass(classOf[String])
      val jsonCaptor: ArgumentCaptor[JsObject] = ArgumentCaptor.forClass(classOf[JsObject])


      val result = route(application, httpGETRequest(httpPathGET)).value

      status(result) mustEqual OK

      verify(mockRenderer, times(1)).render(templateCaptor.capture(), jsonCaptor.capture())(any())

      templateCaptor.getValue mustEqual templateToBeRendered

      jsonCaptor.getValue must containJson(jsonToPassToTemplate(form))
    }

    "redirect to Session Expired page for a GET when there is no data" in {
      mutableFakeDataRetrievalAction.setDataToReturn(None)

      val result = route(application, httpGETRequest(httpPathGET)).value

      status(result) mustEqual SEE_OTHER

      redirectLocation(result).value mustBe controllers.preMigration.routes.ListOfSchemesController.onPageLoad(Scheme).url
    }

    "Save data to user answers and redirect to next page when valid data is submitted and remove other trustee id" in {

      val expectedJson = Json.obj()

      when(mockCompoundNavigator.nextPage(ArgumentMatchers.eq(ConfirmDeleteTrusteeId), any(), any())(any()))
        .thenReturn(routes.AddTrusteeController.onPageLoad())
      when(mockUserAnswersCacheConnector.save(any(), any())(any(), any()))
        .thenReturn(Future.successful(Json.obj()))
      mutableFakeDataRetrievalAction.setDataToReturn(userAnswers.map(_.setOrException(OtherTrusteesId, true)))

      val jsonCaptor: ArgumentCaptor[JsObject] = ArgumentCaptor.forClass(classOf[JsObject])

      val result = route(application, httpPOSTRequest(httpPathPOST, valuesValid)).value

      status(result) mustEqual SEE_OTHER

      verify(mockUserAnswersCacheConnector, times(1)).save(any(), jsonCaptor.capture)(any(), any())

      jsonCaptor.getValue must containJson(expectedJson)

      redirectLocation(result) mustBe Some(routes.AddTrusteeController.onPageLoad().url)

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
        .thenReturn(routes.AddTrusteeController.onPageLoad())
      when(mockUserAnswersCacheConnector.save(any(), any())(any(), any()))
        .thenReturn(Future.successful(Json.obj()))
      mutableFakeDataRetrievalAction.setDataToReturn(userAnswers1.map(_.setOrException(OtherTrusteesId, true)
        .setOrException(AnyTrusteesId, true)))

      val jsonCaptor: ArgumentCaptor[JsObject] = ArgumentCaptor.forClass(classOf[JsObject])

      val result = route(application, httpPOSTRequest(httpPathPOST, valuesValid)).value

      status(result) mustEqual SEE_OTHER

      verify(mockUserAnswersCacheConnector, times(1)).save(any(), jsonCaptor.capture)(any(), any())

      jsonCaptor.getValue must containJson(expectedJson)

      redirectLocation(result) mustBe Some(routes.AddTrusteeController.onPageLoad().url)

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
