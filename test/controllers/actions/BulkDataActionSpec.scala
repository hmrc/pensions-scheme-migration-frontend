/*
 * Copyright 2021 HM Revenue & Customs
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

package controllers.actions

import base.SpecBase
import config.AppConfig
import connectors.cache.CurrentPstrCacheConnector
import connectors.{AncillaryPsaException, ListOfSchemesConnector, MinimalDetailsConnector, DelimitedAdminException, ListOfSchemes5xxException}
import models.requests.{AuthenticatedRequest, BulkDataRequest}
import models.{Items, MinPSA, ListOfLegacySchemes}
import org.mockito.ArgumentMatchers._
import org.mockito.MockitoSugar
import org.scalatest.EitherValues
import org.scalatest.concurrent.ScalaFutures
import play.api.libs.json.Json
import play.api.mvc.Result
import play.api.test.Helpers._
import uk.gov.hmrc.domain.PsaId
import utils.Data.psaId

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class BulkDataActionSpec
  extends SpecBase
    with MockitoSugar
    with EitherValues
    with ScalaFutures {
  class Harness(schemeCacheConnector: CurrentPstrCacheConnector,
                listOfSchemesConnector: ListOfSchemesConnector,
                minimalDetailsConnector: MinimalDetailsConnector,
                appConfig: AppConfig,
                isRequired: Boolean)
    extends BulkRetrievalImpl(schemeCacheConnector, listOfSchemesConnector, minimalDetailsConnector, appConfig, isRequired) {
    def callRefine[A](request: AuthenticatedRequest[A]): Future[Either[Result, BulkDataRequest[A]]] =
      refine(request)
  }

  private val schemeCacheConnector: CurrentPstrCacheConnector = mock[CurrentPstrCacheConnector]
  private val listOfSchemesConnector: ListOfSchemesConnector = mock[ListOfSchemesConnector]
  private val minimalDetailsConnector: MinimalDetailsConnector = mock[MinimalDetailsConnector]
  private val minPSA = MinPSA("test@test.com", false, Some("test company"), None, false, false)
  private val listOfSchemes = List(Items("test-pstr", "", true, "test-scheme", "", Some("")))

  "Bulk Data Action" when {

    "there is no scheme data in the cache but the data is required" must {
      "throw the user to psa overview page" in {
        when(schemeCacheConnector.fetch(any(), any())) thenReturn Future(None)

        val action = new Harness(schemeCacheConnector, listOfSchemesConnector, minimalDetailsConnector, appConfig, true)

        val futureResult = action.callRefine(
          AuthenticatedRequest(
            request = fakeRequest,
            externalId = "id",
            psaId = PsaId(psaId)
          )
        ).map(_.left.value)

        whenReady(futureResult) { result =>
          result.header.status mustBe SEE_OTHER
          redirectLocation(futureResult).value mustBe appConfig.psaOverviewUrl
        }
      }
    }

    "there is no scheme data in the cache and the data is not required" must {
      "call the api, gets the min PSA details and list of rac dac schemes data, save the data in the cache and" +
        "return the Bulk Data Request with minimal psa and list of schemes" in {
        when(schemeCacheConnector.fetch(any(), any())) thenReturn Future(None)
        when(minimalDetailsConnector.getPSADetails(any())(any(), any())).thenReturn(Future(minPSA))
        when(listOfSchemesConnector.getListOfSchemes(any())(any(), any())).thenReturn(Future(Right(ListOfLegacySchemes(1, Some(listOfSchemes)))))
        when(schemeCacheConnector.save(any())(any(), any())).thenReturn(Future(Json.obj()))
        val action = new Harness(schemeCacheConnector, listOfSchemesConnector, minimalDetailsConnector, appConfig, false)

        val futureResult = action.callRefine(AuthenticatedRequest(
          request = fakeRequest,
          externalId = "id",
          psaId = PsaId(psaId)
        )).map(_.right.value)

        whenReady(futureResult) { result =>
          result.md mustBe minPSA
          result.lisOfSchemes mustBe listOfSchemes
        }
        verify(schemeCacheConnector, times(1)).save(any())(any(), any())
      }
    }

    "there is no scheme data in the cache and the data is not required" must {
      "call the api, gets the min PSA details and no rac dac schemes data, save the data in the cache and" +
        "return the Bulk Data Request with minimal psa but no list of schemes" in {
        reset(schemeCacheConnector)
        val listOfSchemes = List(Items("test-pstr", "", false, "test-scheme", "", Some("")))
        when(schemeCacheConnector.fetch(any(), any())) thenReturn Future(None)
        when(minimalDetailsConnector.getPSADetails(any())(any(), any())).thenReturn(Future(minPSA))
        when(listOfSchemesConnector.getListOfSchemes(any())(any(), any())).thenReturn(Future(Right(ListOfLegacySchemes(1, Some(listOfSchemes)))))
        when(schemeCacheConnector.save(any())(any(), any())).thenReturn(Future(Json.obj()))
        val action = new Harness(schemeCacheConnector, listOfSchemesConnector, minimalDetailsConnector, appConfig, false)

        val futureResult = action.callRefine(AuthenticatedRequest(
          request = fakeRequest,
          externalId = "id",
          psaId = PsaId(psaId)
        )).map(_.right.value)

        whenReady(futureResult) { result =>
          result.md mustBe minPSA
          result.lisOfSchemes mustBe Nil
        }
        verify(schemeCacheConnector, times(1)).save(any())(any(), any())
      }
    }

    "there is scheme data in the cache" must {
      "return the Bulk Data Request with minimal psa and list of schemes" in {
        reset(schemeCacheConnector)
        when(schemeCacheConnector.fetch(any(), any())) thenReturn Future(Some(Json.obj("schemes" -> Json.toJson(listOfSchemes), "md" -> Json.toJson(minPSA))))
        val action = new Harness(schemeCacheConnector, listOfSchemesConnector, minimalDetailsConnector, appConfig, false)

        val futureResult = action.callRefine(AuthenticatedRequest(
          request = fakeRequest,
          externalId = "id",
          psaId = PsaId(psaId)
        )).map(_.right.value)

        whenReady(futureResult) { result =>
          result.md mustBe minPSA
          result.lisOfSchemes mustBe listOfSchemes
        }
        verify(schemeCacheConnector, never).save(any())(any(), any())
      }
    }

    "there is invalid scheme data in the cache" must {
      "redirect to psa overview page" in {
        reset(schemeCacheConnector)
        when(schemeCacheConnector.fetch(any(), any())) thenReturn Future(Some(Json.obj("invalid" -> "data")))
        val action = new Harness(schemeCacheConnector, listOfSchemesConnector, minimalDetailsConnector, appConfig, false)

        val futureResult = action.callRefine(AuthenticatedRequest(
          request = fakeRequest,
          externalId = "id",
          psaId = PsaId(psaId)
        )).map(_.left.value)

        whenReady(futureResult) { result =>
          result.header.status mustBe SEE_OTHER
          redirectLocation(futureResult).value mustBe appConfig.psaOverviewUrl
        }
      }
    }

    "DelimitedAdminException is thrown by minimal psa api call" must {
      "redirect to psa delimited page" in {
        reset(schemeCacheConnector, minimalDetailsConnector)
        when(schemeCacheConnector.fetch(any(), any())) thenReturn Future(None)
        when(minimalDetailsConnector.getPSADetails(any())(any(), any())).thenThrow(new DelimitedAdminException)
        val action = new Harness(schemeCacheConnector, listOfSchemesConnector, minimalDetailsConnector, appConfig, false)

        val futureResult = action.callRefine(AuthenticatedRequest(
          request = fakeRequest,
          externalId = "id",
          psaId = PsaId(psaId)
        )).map(_.left.value)

        whenReady(futureResult) { result =>
          result.header.status mustBe SEE_OTHER
          redirectLocation(futureResult).value mustBe appConfig.psaDelimitedUrl
        }
      }
    }

    "AncillaryPsaException is thrown by list schemes api call" must {
      "redirect to the 'cannot migrate' page" in {
        reset(schemeCacheConnector, minimalDetailsConnector, listOfSchemesConnector)
        when(schemeCacheConnector.fetch(any(), any())) thenReturn Future(None)
        when(listOfSchemesConnector.getListOfSchemes(any())(any(),any())).thenReturn(Future.failed(AncillaryPsaException()))

        val action = new Harness(schemeCacheConnector, listOfSchemesConnector, minimalDetailsConnector, appConfig, false)

        val futureResult = action.callRefine(AuthenticatedRequest(
          request = fakeRequest,
          externalId = "id",
          psaId = PsaId(psaId)
        )).map(_.left.value)

        whenReady(futureResult) { result =>
          result.header.status mustBe SEE_OTHER
          redirectLocation(futureResult).value mustBe controllers.preMigration.routes.CannotMigrateController.onPageLoad().url
        }
      }
    }

    "ListOfSchemes5xxException is thrown by list schemes api call" must {
      "redirect to the 'there is a problem' page" in {
        reset(schemeCacheConnector, minimalDetailsConnector, listOfSchemesConnector)
        when(schemeCacheConnector.fetch(any(), any())) thenReturn Future(None)
        when(listOfSchemesConnector.getListOfSchemes(any())(any(),any())).thenReturn(Future.failed(ListOfSchemes5xxException()))

        val action = new Harness(schemeCacheConnector, listOfSchemesConnector, minimalDetailsConnector, appConfig, false)

        val futureResult = action.callRefine(AuthenticatedRequest(
          request = fakeRequest,
          externalId = "id",
          psaId = PsaId(psaId)
        )).map(_.left.value)

        whenReady(futureResult) { result =>
          result.header.status mustBe SEE_OTHER
          redirectLocation(futureResult).value mustBe controllers.routes.ThereIsAProblemController.onPageLoad().url
        }
      }
    }
  }
}
