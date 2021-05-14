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
import connectors.SessionDataCacheConnector
import org.mockito.Mockito.reset
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import play.api.mvc._
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.{Retrieval, ~}
import uk.gov.hmrc.auth.core.{AuthConnector, Enrolment, EnrolmentIdentifier, Enrolments}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

class AuthActionSpec
  extends SpecBase
    with BeforeAndAfterEach
    with MockitoSugar {

  import AuthActionSpec._

  override def beforeEach(): Unit = {
    reset(mockSessionDataCacheConnector)
    super.beforeEach()
  }

  "Auth Action" when {
    "the user has valid credentials" must {
      "return OK" in {
        val authAction = new AuthActionImpl(fakeAuthConnector(authRetrievals()), parser)
        val controller = new Harness(authAction)

        val result = controller.onPageLoad()(fakeRequest)
        status(result) mustBe OK
      }
    }
    "the user does not have valid enrolments" must {
      "redirect to unauthorised" in {
        val authAction = new AuthActionImpl(fakeAuthConnector(emptyAuthRetrievals), parser)
        val controller = new Harness(authAction)

        val result = controller.onPageLoad()(fakeRequest)
        status(result) mustBe SEE_OTHER
      }
    }
    "the user does not have valid id" must {
      "redirect to unauthorised" in {
        val authAction = new AuthActionImpl(fakeAuthConnector(erroneousRetrievals), parser)
        val controller = new Harness(authAction)

        val result = controller.onPageLoad()(fakeRequest)
        status(result) mustBe SEE_OTHER
      }
    }
  }
}

object AuthActionSpec extends SpecBase with MockitoSugar {

  private def fakeAuthConnector(stubbedRetrievalResult: Future[_]): AuthConnector =
    new AuthConnector {
      def authorise[A](predicate: Predicate, retrieval: Retrieval[A])
                      (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[A] =
        stubbedRetrievalResult.map(_.asInstanceOf[A])(ec)
    }

  private val enrolmentPSA = Enrolment(
    key = "HMRC-PODS-ORG",
    identifiers = Seq(EnrolmentIdentifier(key = "PSAID", value = "A0000000")),
    state = "",
    delegatedAuthRule = None
  )

  private def authRetrievals(
                              enrolments: Set[Enrolment] = Set(enrolmentPSA)
                            ): Future[Some[String] ~ Enrolments] =
    Future.successful(new ~(Some("id"), Enrolments(enrolments)))

  private def emptyAuthRetrievals: Future[Some[String] ~ Enrolments] =
    Future.successful(new ~(Some("id"), Enrolments(Set())))

  private def erroneousRetrievals: Future[None.type ~ Enrolments] =
    Future.successful(new ~(None, Enrolments(Set())))

  class Harness(
                 authAction: AuthAction,
                 val controllerComponents: MessagesControllerComponents = controllerComponents
               ) extends BaseController {
    def onPageLoad(): Action[AnyContent] = authAction.apply { _ => Ok }
  }

  private val parser: BodyParsers.Default = injector.instanceOf[BodyParsers.Default]

  private val mockSessionDataCacheConnector: SessionDataCacheConnector = mock[SessionDataCacheConnector]
}
