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

package helpers.cya

import identifiers.TypedIdentifier
import models._
import models.benefitsAndInsurance.{BenefitsProvisionType, BenefitsType}
import play.api.i18n.Messages
import play.api.libs.json.Reads
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.{Content, HtmlContent}
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist._
import uk.gov.hmrc.viewmodels.{MessageInterpolators, Text}
import utils.UserAnswers

import java.time.LocalDate
import java.time.format.DateTimeFormatter

trait CYAHelperForTwirl {

  private def addrLineToHtml(l: String): String =
    s"""<span class="govuk-!-display-block">$l</span>"""

  private def addressAnswer(addr: Address)
    (implicit messages: Messages): HtmlContent =
    HtmlContent(
      addrLineToHtml(addr.addressLine1) +
        addrLineToHtml(addr.addressLine2) +
        addr.addressLine3.fold("")(addrLineToHtml) +
        addr.addressLine4.fold("")(addrLineToHtml) +
        addr.postcode.fold("")(addrLineToHtml) +
        addrLineToHtml(messages("country." + addr.country))
    )

  protected def answerBooleanTransform()(implicit messages: Messages): Option[Boolean => HtmlContent] =
    Some(opt => HtmlContent(Messages(s"booleanAnswer.${opt.toString}")))
  protected val answerStringTransform: Option[String => HtmlContent] = Some(opt => HtmlContent(opt))
  protected val answerBenefitsProvisionTypeTransform: Option[BenefitsProvisionType => Text] = Some(opt => msg"howProvideBenefits.${opt.toString}")
  protected val answerPersonNameTransform: Option[PersonName => Text] = Some(opt => msg"${opt.fullName}")
  protected val answerBenefitsTypeTransform: Option[BenefitsType => Text] = Some(opt => msg"benefitsType.${opt.toString}")
  protected val referenceValueTransform: Option[ReferenceValue => HtmlContent] = Some(opt => HtmlContent(opt.value))
  protected val answerDateTransform: Option[LocalDate => Text] = Some(date => lit"${date.format(DateTimeFormatter.ofPattern("d-M-yyyy"))}")
  protected def answerAddressTransform(implicit messages: Messages): Option[Address => HtmlContent] = Some(opt => addressAnswer(opt))

  def rows(viewOnly: Boolean, rows: Seq[SummaryListRow]): Seq[SummaryListRow] =
    if (viewOnly) rows.map(_.copy(actions = None)) else rows

  def booleanToText: Boolean => String = bool => if (bool) "site.yes" else "site.no"

  def booleanToContent(implicit messages: Messages): Boolean => HtmlContent = bool =>
    if (bool) {
      HtmlContent(Messages("site.yes"))
    } else
      HtmlContent(Messages("site.no"))

  private val attachDynamicIndexToEachActionItem: (Seq[ActionItem], Int) => Seq[ActionItem] = (actionItemSeq, index) => {
    val updatedActionItems = actionItemSeq.map { actionItem => {
      val attributes: String = actionItem.attributes.getOrElse("id", s"add")
      val updatedAttributeValue = Map("id" -> s"cya-0-${index.toString}-$attributes")
      actionItem.copy(attributes = updatedAttributeValue)
    }}
    updatedActionItems
  }

  val rowsWithDynamicIndices: Seq[SummaryListRow] => Seq[SummaryListRow] = rows => rows.zipWithIndex.map { case (row, index) =>
    val newActions = row.actions.map { act => act.copy(items = attachDynamicIndexToEachActionItem(act.items, index)) }
    row.copy(actions = newActions)
  }

  def actionAdd[A](optionURL: Option[String], visuallyHiddenText: Option[Text])(implicit messages: Messages): Seq[ActionItem] = {
    val addVisuallyHidden = visuallyHiddenText.map {
      visuallyHiddn => messages("site.add") + " " + visuallyHiddn.resolve
    }
    optionURL.toSeq.map { url =>
      ActionItem(
        content = HtmlContent(s"<span aria-hidden=true >${messages("site.add")}</span>"),
        href = url,
        visuallyHiddenText = addVisuallyHidden,
        attributes = Map("id" -> "change")
      )
    }
  }

  def actionChange[A](optionURL: Option[String], visuallyHiddenText: Option[Text])(implicit
    messages: Messages): Option[Actions] = {
    val changeVisuallyHidden = visuallyHiddenText.map {
      visuallyHiddn => messages("site.change") + " " + visuallyHiddn.resolve
    }
    optionURL match {
      case Some(url) => Some(
        Actions(
          items = Seq(
            ActionItem(
              content = HtmlContent(s"<span aria-hidden=true >${messages("site.change")}</span>"),
              href = url,
              visuallyHiddenText = changeVisuallyHidden,
              attributes = Map("id" -> "change")
            )
          )
        )
      )
      case _ => None
    }
  }

  def answerOrAddRow[A](id: TypedIdentifier[A],
    message: String,
    url: Option[String] = None,
    visuallyHiddenText: Option[Text] = None,
    answerTransform: Option[A => Content] = None)
    (implicit ua: UserAnswers, rds: Reads[A], messages: Messages): SummaryListRow =
    ua.get(id) match {
      case None =>
        SummaryListRowViewModel(
          key = KeyViewModel(HtmlContent(message)).withCssClass("govuk-!-width-one-half"),
          value = ValueViewModel(HtmlContent(Messages("site.incomplete"))).withCssClass("govuk-!-width-one-third"),
          actions = actionAdd(url, visuallyHiddenText)
        )
      case Some(answer) =>
        SummaryListRow(
          key = KeyViewModel(HtmlContent(message)).withCssClass("govuk-!-width-one-half"),
          value = answerTransform.fold(ValueViewModel(HtmlContent(answer.toString)))(transform => Value(transform(answer))),
          actions = actionChange(url, visuallyHiddenText)
        )
    }

  def answerRow[A](message: String,
    answer: String,
    url: Option[String] = None,
    visuallyHiddenText: Option[Text] = None)
    (implicit messages: Messages): SummaryListRow = {
    val value = ValueViewModel(HtmlContent(answer))

    SummaryListRow(
      key = KeyViewModel(HtmlContent(message)).withCssClass("govuk-!-width-one-half"),
      value = value,
      actions = actionChange(url, visuallyHiddenText)
    )
  }

  def addRow[A](message: String,
                   url: Option[String] = None,
                   visuallyHiddenText: Option[Text] = None)
                  (implicit messages: Messages): SummaryListRow = {
    val value = ValueViewModel(
      HtmlContent(
        HtmlFormat.escape(messages("site.incomplete"))
      )
    ).withCssClass("govuk-!-width-one-third")

    SummaryListRowViewModel(
      key = KeyViewModel(HtmlContent(message)).withCssClass("govuk-!-width-one-half"),
      value = value,
      actions = actionAdd(url, visuallyHiddenText)
    )
  }

//  def changeLink(url: String, visuallyHiddenText: Option[Message] = None)
//    (implicit messages: Messages): Option[Link] = Some(Link(messages("site.change"), url, visuallyHiddenText))

//  def addLink(url: String, visuallyHiddenText: Option[Message] = None)
//    (implicit messages: Messages): Option[Link] = Some(Link(messages("site.add"), url, visuallyHiddenText))



  private object SummaryListRowViewModel {

    def apply(
               key: Key,
               value: Value
             ): SummaryListRow =
      SummaryListRow(
        key   = key,
        value = value
      )

    def apply(
               key: Key,
               value: Value,
               actions: Seq[ActionItem]
             ): SummaryListRow =
      SummaryListRow(
        key     = key,
        value   = value,
        actions = Some(Actions(items = actions))
      )
  }

  object KeyViewModel {

    def apply(content: Content): Key =
      Key(content = content)
  }

  implicit class FluentKey(key: Key) {

    def withCssClass(className: String): Key =
      if (key.classes.isEmpty) {
        key copy (classes = className)
      } else {
        key copy (classes = s"${key.classes} $className")
      }
  }

  object ValueViewModel {

    def apply(content: Content): Value =
      Value(content = content)
  }

  implicit class FluentValue(value: Value) {

    def withCssClass(className: String): Value = {
      if (value.classes.isEmpty) {
        value copy (classes = className)
      } else {
        value copy (classes = s"${value.classes} $className")
      }
    }
  }
}

object CYAHelperForTwirl {
  def getAnswer[A](id: TypedIdentifier[A])
    (implicit ua: UserAnswers, rds: Reads[A]): String =
    ua.get(id)
      .getOrElse(throw MandatoryAnswerMissingException(id.toString))
      .toString

  def getName(id: TypedIdentifier[PersonName])
    (implicit ua: UserAnswers, rds: Reads[PersonName]): String =
    ua.get(id)
      .getOrElse(throw MandatoryAnswerMissingException(id.toString))
      .fullName

  def getCompanyName(id: TypedIdentifier[CompanyDetails])
    (implicit ua: UserAnswers, rds: Reads[CompanyDetails]): String =
    ua.get(id)
      .getOrElse(throw MandatoryAnswerMissingException(id.toString))
      .companyName

  def getPartnershipName(id: TypedIdentifier[PartnershipDetails])
                    (implicit ua: UserAnswers, rds: Reads[PartnershipDetails]): String =
    ua.get(id)
      .getOrElse(throw MandatoryAnswerMissingException(id.toString))
      .partnershipName
}

case class MandatoryAnswerMissingExceptionForTwirl(missingField:String)
  extends Exception(s"An answer which was mandatory is missing from scheme details returned from TPSS: $missingField")
