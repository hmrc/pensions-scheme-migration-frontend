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

package viewmodels

import models.EntitySpoke
import play.api.libs.json.{Format, Json}

case class TaskList(
                                  h1: String,
                                  beforeYouStart: TaskListEntitySection,
                                  about: TaskListEntitySection,
                                  workingKnowledge: Option[TaskListEntitySection],
                                  addEstablisherHeader: Option[TaskListEntitySection],
                                  addTrusteeHeader: Option[TaskListEntitySection],
                                  establishers: Seq[TaskListEntitySection],
                                  trustees: Seq[TaskListEntitySection],
                                  declaration: Option[TaskListEntitySection]
                                )

object TaskList {
  implicit lazy val formats: Format[TaskList] = Json.format[TaskList]
}

case class TaskListEntitySection(
                                               isCompleted: Option[Boolean],
                                               entities: Seq[EntitySpoke],
                                               header: Option[String],
                                               p1: String*
                                             )
object TaskListEntitySection {
  implicit lazy val formats: Format[TaskListEntitySection] = Json.format[TaskListEntitySection]
}


