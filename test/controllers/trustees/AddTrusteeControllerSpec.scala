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
import helpers.AddToListHelper
import identifiers.beforeYouStart.SchemeTypeId
import identifiers.trustees.individual.TrusteeNameId
import identifiers.trustees.{AddTrusteeId, IsTrusteeNewId, TrusteeKindId}
import matchers.JsonMatchers
import models.trustees.TrusteeKind
import models.{PersonName, Scheme, SchemeType}
import org.mockito.ArgumentMatchers.any
import org.mockito.{ArgumentCaptor, ArgumentMatchers}
import play.api.Application
import play.api.data.Form
import play.api.inject.bind
import play.api.inject.guice.GuiceableModule
import play.api.libs.json.{JsObject, JsValue, Json}
import play.api.test.Helpers._
import play.twirl.api.Html
import uk.gov.hmrc.nunjucks.NunjucksSupport
import uk.gov.hmrc.viewmodels.{Radios, Table}
import utils.Data.{schemeName, ua}
import utils.{Enumerable, UserAnswers}

import scala.concurrent.Future

class AddTrusteeControllerSpec extends ControllerSpecBase with NunjucksSupport with JsonMatchers with Enumerable.Implicits {
  private val trusteeName: String = "Jane Doe"
  private val userAnswers: Option[UserAnswers] = ua.set(TrusteeKindId(0), TrusteeKind.Individual).flatMap(
    _.set(TrusteeNameId(0), PersonName("a", "b", true)).flatMap(
      _.set(IsTrusteeNewId(0), true).flatMap(
        _.set(TrusteeKindId(1), TrusteeKind.Individual).flatMap(
          _.set(TrusteeNameId(1), PersonName("c", "d", true)).flatMap(
            _.set(IsTrusteeNewId(1), true)
          ))))).toOption
  private val templateToBeRendered = "trustees/addTrustee.njk"
  private val form: Form[Boolean] = new ConfirmDeleteTrusteeFormProvider()(trusteeName)
  val table: Table = Table(head = Nil, rows = Nil)
  private val hideDeleteLink = false
  private val itemList: JsValue = Json.obj(
    "name" -> trusteeName,
    "changeUrl" ->  "controllers.establishers.company.routes.SpokeTaskListController.onPageLoad(0)",
    "removeUrl" ->   "controllers.establishers.ConfirmDeleteEstablisherController.onPageLoad(0,0)"
  )
  private val mockHelper: AddToListHelper = mock[AddToListHelper]

  private val mutableFakeDataRetrievalAction: MutableFakeDataRetrievalAction = new MutableFakeDataRetrievalAction()
  val extraModules: Seq[GuiceableModule] = Seq(
    bind[AddToListHelper].toInstance(mockHelper)
  )
  private val application: Application = applicationBuilderMutableRetrievalAction(mutableFakeDataRetrievalAction, extraModules).build()

  private def httpPathGET: String = controllers.trustees.routes.AddTrusteeController.onPageLoad().url
  private def httpPathPOST: String = controllers.trustees.routes.AddTrusteeController.onSubmit().url

  private val valuesValid: Map[String, Seq[String]] = Map(
    "value" -> Seq("true")
  )

  private val valuesInvalid: Map[String, Seq[String]] = Map(
    "value" -> Seq("invalid")
  )

  private val maxTrustees = 5

  private def jsonToPassToTemplate(userAnswers:Option[UserAnswers]): Form[Boolean] => JsObject = {
        val countOfTrustees = userAnswers.map(_.allTrusteesAfterDelete.size).getOrElse(0)
      form =>
        Json.obj(
          "form" -> form,
          "itemListComplete" -> itemList,
          "itemListIncomplete" -> itemList,
          "hideDeleteLink" -> hideDeleteLink,
          "radios" -> Radios.yesNo(form("value")),
          "schemeName" -> schemeName,
          "trusteeSize" -> countOfTrustees,
          "maxTrustees" -> maxTrustees
        )
   }

  override def beforeEach: Unit = {
    super.beforeEach
    when(mockRenderer.render(any(), any())(any())).thenReturn(Future.successful(Html("")))
    when(mockHelper.mapTrusteesToList(any(), any(), any(), any())).thenReturn(itemList)
    when(mockAppConfig.maxTrustees).thenReturn(maxTrustees)
  }


  "AddTrusteeController" must {

    "return OK and the correct view for a GET, passing the correct no of trustees and max trustees into template" in {
      val ua = userAnswers.map(_.setOrException(TrusteeNameId(1), PersonName("Bill", "Bloggs")))
      mutableFakeDataRetrievalAction.setDataToReturn(ua)
      val templateCaptor : ArgumentCaptor[String] = ArgumentCaptor.forClass(classOf[String])
      val jsonCaptor: ArgumentCaptor[JsObject] = ArgumentCaptor.forClass(classOf[JsObject])

      val result = route(application, httpGETRequest(httpPathGET)).value

      status(result) mustEqual OK

      verify(mockRenderer, times(1)).render(templateCaptor.capture(), jsonCaptor.capture())(any())

      templateCaptor.getValue mustEqual templateToBeRendered

      jsonCaptor.getValue must containJson(jsonToPassToTemplate(ua)(form))
    }

    "redirect to Session Expired page for a GET when there is no data" in {
      mutableFakeDataRetrievalAction.setDataToReturn(None)

      val result = route(application, httpGETRequest(httpPathGET)).value

      status(result) mustEqual SEE_OTHER

      redirectLocation(result).value mustBe controllers.preMigration.routes.ListOfSchemesController.onPageLoad(Scheme).url
    }

    "Save data to user answers and redirect to next page when valid data is submitted" in {

      when(mockCompoundNavigator.nextPage(ArgumentMatchers.eq(AddTrusteeId(Some(true))), any(), any())(any()))
        .thenReturn(routes.AddTrusteeController.onPageLoad())

      mutableFakeDataRetrievalAction.setDataToReturn(userAnswers)

      val result = route(application, httpPOSTRequest(httpPathPOST, valuesValid)).value

      status(result) mustEqual SEE_OTHER

      redirectLocation(result) mustBe Some(routes.AddTrusteeController.onPageLoad().url)
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

    "redirect to no trustees page if there are no added trustees and the scheme is a single trust" in {
      val userAnswersNoTrusteesSingleTrust = UserAnswers().setOrException(SchemeTypeId, SchemeType.SingleTrust)

      mutableFakeDataRetrievalAction.setDataToReturn(Some(userAnswersNoTrusteesSingleTrust))

      val result = route(application, httpGETRequest(httpPathGET)).value

      status(result) mustEqual SEE_OTHER
      redirectLocation(result) mustBe Some(routes.NoTrusteesController.onPageLoad().url)
    }

    "redirect to any trustees page if there are no added trustees and the scheme is NOT a single trust" in {
      val userAnswersNoTrusteesOtherTrust = UserAnswers().setOrException(SchemeTypeId, SchemeType.GroupLifeDeath)

      mutableFakeDataRetrievalAction.setDataToReturn(Some(userAnswersNoTrusteesOtherTrust))

      val result = route(application, httpGETRequest(httpPathGET)).value

      status(result) mustEqual SEE_OTHER
      redirectLocation(result) mustBe Some(routes.AnyTrusteesController.onPageLoad().url)
    }
  }
}
