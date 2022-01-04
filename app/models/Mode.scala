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

package models

import play.api.mvc.{JavascriptLiteral, PathBindable}
import utils.WithName

sealed trait Mode

case object NormalMode extends WithName("") with Mode
case object CheckMode extends WithName("change") with Mode

object Mode {

  case class UnknownModeException() extends Exception

  implicit val jsLiteral: JavascriptLiteral[Mode] = {
    case NormalMode => "NormalMode"
    case CheckMode => "CheckMode"
  }

  implicit def modePathBindable(implicit stringBinder: PathBindable[String]): PathBindable[Mode] = new
      PathBindable[Mode] {

    val modes = Seq(NormalMode, CheckMode)

    override def bind(key: String, value: String): Either[String, Mode] = {
      stringBinder.bind(key, value) match {
        case Right(NormalMode.toString) => Right(NormalMode)
        case Right(CheckMode.toString) => Right(CheckMode)
        case _ => Left("Mode binding failed")
      }
    }

    override def unbind(key: String, value: Mode): String = {
      val modeValue = modes.find(_ == value).map(_.toString).getOrElse(throw UnknownModeException())
      stringBinder.unbind(key, modeValue)
    }
  }
}
