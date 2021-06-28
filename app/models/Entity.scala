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

package models

import controllers.establishers.routes._
import identifiers.establishers.individual.EstablisherNameId
import models.establishers.EstablisherKind
import play.api.libs.json.{Format, Json}

sealed trait Entity[ID] {
  def id: ID

  def name: String

  def isDeleted: Boolean

  def isCompleted: Boolean

  def editLink: Option[String]

  def deleteLink: Option[String]

  def index: Int
}

object Entity {
  implicit lazy val formats: Format[Entity[_]] = Json.format[Entity[_]]
}

case class EstablisherIndividualEntity(
                                        id: EstablisherNameId,
                                        name: String,
                                        isDeleted: Boolean,
                                        isCompleted: Boolean,
                                        isNewEntity: Boolean,
                                        noOfRecords: Int
                                      ) extends Establisher[EstablisherNameId] {
  override def editLink: Option[String] = None

  override def deleteLink: Option[String] =
    if (noOfRecords > 1)
      Some(ConfirmDeleteEstablisherController.onPageLoad(id.index, EstablisherKind.Individual).url)
    else
      None

  override def index: Int = id.index
}

object EstablisherIndividualEntity {
  implicit lazy val formats: Format[EstablisherIndividualEntity] = Json.format[EstablisherIndividualEntity]
}

sealed trait Establisher[T] extends Entity[T]

object Establisher {
  implicit lazy val formats: Format[Establisher[_]] = Json.format[Establisher[_]]
}