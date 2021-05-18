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

package controllers


import base.SpecBase
import controllers.actions.FakeDataRetrievalAction
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import utils.Data.ua
import utils.{Enumerable, UserAnswers}

trait ControllerSpecBase extends SpecBase with Enumerable.Implicits {

  implicit val global = scala.concurrent.ExecutionContext.Implicits.global

  val cacheMapId = "id"

  def getEmptyData: FakeDataRetrievalAction = new FakeDataRetrievalAction(Some(UserAnswers()))

  def getSchemeName: FakeDataRetrievalAction = new FakeDataRetrievalAction(Some(ua))

  def dontGetAnyData: FakeDataRetrievalAction = new FakeDataRetrievalAction(None)

  def dontGetAnyDataViewOnly: FakeDataRetrievalAction = new FakeDataRetrievalAction(None)

  def asDocument(htmlAsString: String): Document = Jsoup.parse(htmlAsString)

}