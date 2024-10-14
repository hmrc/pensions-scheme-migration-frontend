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

package viewmodels

import play.api.i18n.Messages
import play.twirl.api.{Html, HtmlFormat}

import scala.language.implicitConversions

@deprecated("Use Messages instead, leftover after migration to twirl from nunjucks", since = "0.248.0")
sealed trait Message {

  @deprecated("Use Messages instead, leftover after migration to twirl from nunjucks", since = "0.248.0")
  def resolve(implicit messages: Messages): String

  @deprecated("Use Messages instead, leftover after migration to twirl from nunjucks", since = "0.248.0")
  def withArgs(args: Any*): Message

  @deprecated("Use Messages instead, leftover after migration to twirl from nunjucks", since = "0.248.0")
  def html(implicit messages: Messages): HtmlFormat.Appendable
}

@deprecated("Use Messages instead, leftover after migration to twirl from nunjucks", since = "0.248.0")
object Message {

  @deprecated("Use Messages instead, leftover after migration to twirl from nunjucks", since = "0.248.0")
  def apply(key: String, args: Any*): Message =
    Resolvable(key, args)

  @deprecated("Use Messages instead, leftover after migration to twirl from nunjucks", since = "0.248.0")
  case class Resolvable(key: String, args: Seq[Any]) extends Message {

    @deprecated("Use Messages instead, leftover after migration to twirl from nunjucks", since = "0.248.0")
    override def resolve(implicit messages: Messages): String = {
      val transformedArgs = args.map {
        case r@Resolvable(_, _) => r.resolve
        case x => x
      }
      messages(key, transformedArgs: _*)
    }

    @deprecated("Use Messages instead, leftover after migration to twirl from nunjucks", since = "0.248.0")
    override def withArgs(args: Any*): Message =
      copy(args = args)


    @deprecated("Use Messages instead, leftover after migration to twirl from nunjucks", since = "0.248.0")
    override def html(implicit messages: Messages): HtmlFormat.Appendable =
      Html(resolve)
  }

  @deprecated("Use Messages instead, leftover after migration to twirl from nunjucks", since = "0.248.0")
  case class Literal(value: String) extends Message {

    @deprecated("Use Messages instead, leftover after migration to twirl from nunjucks", since = "0.248.0")
    override def resolve(implicit messages: Messages): String =
      value

    @deprecated("Use Messages instead, leftover after migration to twirl from nunjucks", since = "0.248.0")
    override def withArgs(args: Any*): Message = this

    @deprecated("Use Messages instead, leftover after migration to twirl from nunjucks", since = "0.248.0")
    override def html(implicit messages: Messages): HtmlFormat.Appendable =
      Html(resolve)
  }

  @deprecated("Use Messages instead, leftover after migration to twirl from nunjucks", since = "0.248.0")
  implicit def literal(string: String): Message = Literal(string)

  @deprecated("Use Messages instead, leftover after migration to twirl from nunjucks", since = "0.248.0")
  implicit def resolve(message: Message)(implicit messages: Messages): String =
    message.resolve

  @deprecated("Use Messages instead, leftover after migration to twirl from nunjucks", since = "0.248.0")
  implicit def resolveOption(message: Option[Message])(implicit messages: Messages): Option[String] =
    message.map(_.resolve)
}
