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

import play.api.mvc.PathBindable

object entities {
  class NamedEntity(val strName: String)
  class PensionManagementType(strName: String) extends NamedEntity(strName)
  final object Establisher extends PensionManagementType("establisher")
  final object Trustee extends PensionManagementType("trustee")

  val pensionManagementTypes: Seq[PensionManagementType] = Seq(
    Establisher,
    Trustee
  )

  class EntityType(strName: String) extends NamedEntity(strName)
  final object Company extends EntityType("company")
  final object Individual extends EntityType("individual")
  final object Partnership extends EntityType("partnership")

  val entityTypes: Seq[EntityType] = Seq(
    Company,
    Individual,
    Partnership
  )

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

  implicit def managementTypePathBinder(implicit stringBinder: PathBindable[String]): PathBindable[PensionManagementType] =
    namedEntityPathBindable(pensionManagementTypes)
  implicit def entityTypePathBinder(implicit stringBinder: PathBindable[String]): PathBindable[EntityType] =
    namedEntityPathBindable(entityTypes)


}
