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

package connectors.cache

import identifiers.TypedIdentifier
import models.MigrationLock
import org.scalatest.matchers.should.Matchers
import play.api.libs.json._
import play.api.mvc.Result
import play.api.mvc.Results._
import uk.gov.hmrc.http.HeaderCarrier

import scala.collection.mutable
import scala.concurrent.{ExecutionContext, Future}

trait FakeUserAnswersCacheConnector extends UserAnswersCacheConnector with Matchers {

  private val data: mutable.HashMap[String, JsValue] = mutable.HashMap()
  private val removed: mutable.ListBuffer[String] = mutable.ListBuffer()

  override def save(lock: MigrationLock, value: JsValue)
                   (implicit ec: ExecutionContext, hc: HeaderCarrier): Future[JsValue] = {
    data += (lock.pstr -> Json.toJson(value))
    Future.successful(Json.obj())
  }

  override  def remove(pstr: String)
                      (implicit ec: ExecutionContext, hc: HeaderCarrier): Future[Result] = {
    removed += pstr
    Future.successful(Ok)
  }

  override def fetch(cacheId: String)(implicit
                                      ec: ExecutionContext,
                                      hc: HeaderCarrier
  ): Future[Option[JsValue]] = {

    Future.successful(Some(Json.obj()))
  }

  def verify[A, I <: TypedIdentifier[A]](id: I, value: A)(implicit fmt: Format[A]): Unit = {
    data should contain(id.toString -> Json.toJson(value))
  }

  def verifyNot(id: TypedIdentifier[_]): Unit = {
    data should not contain key(id.toString)
  }

  def verifyRemoved(id: TypedIdentifier[_]): Unit = {
    removed should contain(id.toString)
  }

  def reset(): Unit = {
    data.clear()
    removed.clear()
  }

}

object FakeUserAnswersCacheConnector extends FakeUserAnswersCacheConnector

