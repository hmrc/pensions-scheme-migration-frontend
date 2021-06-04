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

package helpers

import identifiers.TypedIdentifier
import models.Link
import play.api.i18n.Messages
import play.api.libs.json.Reads
import uk.gov.hmrc.viewmodels.{Content, SummaryList, Text, Html, MessageInterpolators}
import uk.gov.hmrc.viewmodels.SummaryList.{Action, Value, Row, Key}
import uk.gov.hmrc.viewmodels.Text.Literal
import utils.UserAnswers
import viewmodels.{Message, AnswerRow}

trait CYAHelper {

  def rows(viewOnly: Boolean, rows: Seq[SummaryList.Row]): Seq[SummaryList.Row] = {
    if (viewOnly) rows.map(_.copy(actions = Nil)) else rows
  }

  def booleanToText: Boolean => String = bool => if (bool) "site.yes" else "site.no"

  def boolAnswerOrAddLink(
                           id: TypedIdentifier[Boolean],
                           message: String,
                           url: String,
                           visuallyHiddenText: Option[Message] = None
                         )(
                           implicit ua: UserAnswers,
                           rds: Reads[Boolean],
                           messages: Messages
                         ): AnswerRow =
    ua.get(id) match {
      case None =>
        AnswerRow(
          label = message,
          answer = Seq(messages("site.not_entered")),
          answerIsMessageKey = false,
          changeUrl = addLink(url, visuallyHiddenText)
        )
      case Some(answer) =>
        AnswerRow(
          label = message,
          answer = Seq(booleanToText(answer)),
          answerIsMessageKey = true,
          changeUrl = changeLink(url, visuallyHiddenText)
        )
    }

  def answerOrAddLink[A](id: TypedIdentifier[A], message: String, url: String, visuallyHiddenText: Option[Message] = None, answerIsMessageKey: Boolean = false)
                        (implicit ua: UserAnswers, rds: Reads[A], messages: Messages): AnswerRow =
    ua.get(id) match {
      case None =>
        AnswerRow(
          label = message,
          answer = Seq(messages("site.not_entered")),
          answerIsMessageKey = answerIsMessageKey,
          changeUrl = addLink(url, visuallyHiddenText)
        )
      case Some(answer) =>
        AnswerRow(
          label = message,
          answer = Seq(answer.toString),
          answerIsMessageKey = answerIsMessageKey,
          changeUrl = changeLink(url, visuallyHiddenText)
        )
    }

  private def actionAdd[A](optionURL: Option[String], visuallyHiddenText: Option[Text])(implicit
    messages: Messages):Seq[Action] = {
    optionURL.toSeq.map { url =>
      Action(
        content = Html(s"<span  aria-hidden=true >${messages("site.add")}</span>"),
        href = url,
        visuallyHiddenText = visuallyHiddenText
      )
    }
  }

  private def actionChange[A](optionURL: Option[String], visuallyHiddenText: Option[Text])(implicit
    messages: Messages):Seq[Action] = {
    optionURL.toSeq.map { url =>
      Action(
        content = Html(s"<span  aria-hidden=true >${messages("site.change")}</span>"),
        href = url,
        visuallyHiddenText = visuallyHiddenText
      )
    }
  }

  def answerOrAddRow[A](id: TypedIdentifier[A],
                        message: String,
                        url: Option[String] = None,
                        visuallyHiddenText: Option[Text] = None,
                        answerTransform: Option[A => Content] = None)
                        (implicit ua: UserAnswers, rds: Reads[A], messages: Messages): Row =
    ua.get(id) match {
      case None =>
        Row(
          key = Key(msg"$message", classes = Seq("govuk-!-width-one-half")),
          value = Value(msg"site.not_entered", classes = Seq("govuk-!-width-one-third")),
          actions = actionAdd(url, visuallyHiddenText)
        )
      case Some(answer) =>
        Row(
          key = Key(msg"$message", classes = Seq("govuk-!-width-one-half")),
          value = answerTransform.fold(Value(Literal(answer.toString)))(transform => Value(transform(answer))),
          actions = actionChange(url, visuallyHiddenText)
        )
    }

  def changeLink(url: String, visuallyHiddenText: Option[Message] = None)
                (implicit messages: Messages): Option[Link] = Some(Link(messages("site.change"), url, visuallyHiddenText))

  def addLink(url: String, visuallyHiddenText: Option[Message] = None)
             (implicit messages: Messages): Option[Link] = Some(Link(messages("site.add"), url, visuallyHiddenText))
}

object CYAHelper {
  def getAnswer[A](id: TypedIdentifier[A])
    (implicit ua: UserAnswers, rds: Reads[A]): String =
    ua.get(id).getOrElse(throw MandatoryAnswerMissingException).toString
}

case object MandatoryAnswerMissingException extends Exception("An answer which was mandatory is missing from scheme details returned from TPSS")

