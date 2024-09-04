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

package models

import play.api.mvc.{JavascriptLiteral, PathBindable}

object entities {
  sealed abstract class NamedEntity(val strName: String)
  sealed abstract class PensionManagementType(strName: String) extends NamedEntity(strName)
  case object Establisher extends PensionManagementType("establisher")
  case object Trustee extends PensionManagementType("trustee")

  val pensionManagementTypes: Seq[PensionManagementType] = Seq(
    Establisher,
    Trustee
  )

  implicit def managementTypePathBinder(implicit stringBinder: PathBindable[String]): PathBindable[PensionManagementType] =
    namedEntityPathBindable(pensionManagementTypes)

  sealed abstract class EntityType(strName: String) extends NamedEntity(strName)
  case object Company extends EntityType("company")
  case object Individual extends EntityType("individual")
  case object Partnership extends EntityType("partnership")

  val entityTypes: Seq[EntityType] = Seq(
    Company,
    Individual,
    Partnership
  )

  implicit val entityTypeJsLiteral: JavascriptLiteral[EntityType] = _.strName
  implicit def entityTypePathBinder(implicit stringBinder: PathBindable[String]): PathBindable[EntityType] =
    namedEntityPathBindable(entityTypes)

  sealed abstract class JourneyType(strName: String) extends NamedEntity(strName)
  case object Address extends JourneyType("address")
  case object Contacts extends JourneyType("contacts")
  case object Details extends JourneyType("details")

  val journeyTypes: Seq[JourneyType] = Seq(
    Address,
    Contacts,
    Details
  )

  implicit def journeyTypePathBinder(implicit stringBinder: PathBindable[String]): PathBindable[JourneyType] =
    namedEntityPathBindable(journeyTypes)

  private def namedEntityPathBindable[T <: NamedEntity](list: Seq[T])(implicit stringBinder: PathBindable[String]) = new PathBindable[T] {
    override def bind(key: String, value: String): Either[String, T] = {
      stringBinder.bind(key, value).flatMap { namedEntityStr =>
        list.collectFirst {
          case namedEntity if namedEntity.strName == namedEntityStr => Right(namedEntity)
        }.getOrElse(Left(s"""Entity type of $namedEntityStr doesn't exist"""))
      }
    }

    override def unbind(key: String, namedEntity: T): String = {
      namedEntity.strName
    }
  }

}
