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

package controllers.establishers

import controllers.ControllerSpecBase
import controllers.actions.MutableFakeDataRetrievalAction
import controllers.establishers.routes.AddEstablisherController
import identifiers.establishers.company.CompanyDetailsId
import identifiers.establishers.individual.EstablisherNameId
import identifiers.establishers.partnership.PartnershipDetailsId
import matchers.JsonMatchers
import models.establishers.EstablisherKind
import models.{CompanyDetails, Index, PartnershipDetails, PersonName}
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import play.api.Application
import play.api.libs.json.{JsObject, Json}
import play.api.test.Helpers._
import play.twirl.api.Html
import uk.gov.hmrc.nunjucks.NunjucksSupport
import utils.Data.{schemeName, ua}
import utils.{Enumerable, UserAnswers}

import scala.concurrent.Future
class AlreadyDeletedControllerSpec extends ControllerSpecBase with NunjucksSupport with JsonMatchers with Enumerable.Implicits {

  private val index: Index = Index(0)
  private val templateToBeRendered = "alreadyDeleted.njk"
  private val mutableFakeDataRetrievalAction: MutableFakeDataRetrievalAction = new MutableFakeDataRetrievalAction()
  private val application: Application = applicationBuilderMutableRetrievalAction(mutableFakeDataRetrievalAction).build()

  override def beforeEach(): Unit = {
    super.beforeEach()
    when(mockRenderer.render(any(), any())(any())).thenReturn(Future.successful(Html("")))
  }

  "AlreadyDeletedController" must {

    "return OK and the correct view for a GET for an individual" in {

      val individualName = PersonName("Jane", "Doe")
      val individualKind: EstablisherKind = EstablisherKind.Individual

      val jsonToPassToTemplate: JsObject =
        Json.obj(
          "title" -> messages("messages__alreadyDeleted__establisher_title"),
          "name" -> individualName.fullName,
          "schemeName" -> schemeName,
          "submitUrl" -> AddEstablisherController.onPageLoad.url
        )

      val userAnswers: Option[UserAnswers] = ua.set(EstablisherNameId(0), individualName).toOption

      def httpPathGET: String = controllers.establishers.routes.AlreadyDeletedController.onPageLoad(index, individualKind).url

      mutableFakeDataRetrievalAction.setDataToReturn(userAnswers)
      val templateCaptor : ArgumentCaptor[String] = ArgumentCaptor.forClass(classOf[String])
      val jsonCaptor: ArgumentCaptor[JsObject] = ArgumentCaptor.forClass(classOf[JsObject])

      val result = route(application, httpGETRequest(httpPathGET)).value

      status(result) mustEqual OK

      verify(mockRenderer, times(1)).render(templateCaptor.capture(), jsonCaptor.capture())(any())

      templateCaptor.getValue mustEqual templateToBeRendered

      jsonCaptor.getValue must containJson(jsonToPassToTemplate)
    }

    "return OK and the correct view for a GET for a company" in {

      val companyName = CompanyDetails("CompanyName")
      val companyKind: EstablisherKind = EstablisherKind.Company

      val jsonToPassToTemplate: JsObject =
        Json.obj(
          "title" -> messages("messages__alreadyDeleted__establisher_title"),
          "name" -> companyName.companyName,
          "schemeName" -> schemeName,
          "submitUrl" -> AddEstablisherController.onPageLoad.url
        )

      val userAnswers: Option[UserAnswers] = ua.set(CompanyDetailsId(0), companyName).toOption

      def httpPathGET: String = controllers.establishers.routes.AlreadyDeletedController.onPageLoad(index, companyKind).url

      mutableFakeDataRetrievalAction.setDataToReturn(userAnswers)
      val templateCaptor : ArgumentCaptor[String] = ArgumentCaptor.forClass(classOf[String])
      val jsonCaptor: ArgumentCaptor[JsObject] = ArgumentCaptor.forClass(classOf[JsObject])

      val result = route(application, httpGETRequest(httpPathGET)).value

      status(result) mustEqual OK

      verify(mockRenderer, times(1)).render(templateCaptor.capture(), jsonCaptor.capture())(any())

      templateCaptor.getValue mustEqual templateToBeRendered

      jsonCaptor.getValue must containJson(jsonToPassToTemplate)
    }

    "return OK and the correct view for a GET for a partnership" in {

      val partnershipName = PartnershipDetails("PartnershipName")
      val partnershipKind: EstablisherKind = EstablisherKind.Partnership

      val jsonToPassToTemplate: JsObject =
        Json.obj(
          "title" -> messages("messages__alreadyDeleted__establisher_title"),
          "name" -> partnershipName.partnershipName,
          "schemeName" -> schemeName,
          "submitUrl" -> AddEstablisherController.onPageLoad.url
        )

      def httpPathGET: String = controllers.establishers.routes.AlreadyDeletedController.onPageLoad(index, partnershipKind).url

      val userAnswers: Option[UserAnswers] = ua.set(PartnershipDetailsId(0), partnershipName).toOption

      mutableFakeDataRetrievalAction.setDataToReturn(userAnswers)
      val templateCaptor : ArgumentCaptor[String] = ArgumentCaptor.forClass(classOf[String])
      val jsonCaptor: ArgumentCaptor[JsObject] = ArgumentCaptor.forClass(classOf[JsObject])

      val result = route(application, httpGETRequest(httpPathGET)).value

      status(result) mustEqual OK

      verify(mockRenderer, times(1)).render(templateCaptor.capture(), jsonCaptor.capture())(any())

      templateCaptor.getValue mustEqual templateToBeRendered

      jsonCaptor.getValue must containJson(jsonToPassToTemplate)
    }

  }
}
