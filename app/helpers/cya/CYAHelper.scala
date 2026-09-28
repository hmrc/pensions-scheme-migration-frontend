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
import uk.gov.hmrc.govukfrontend.views.Aliases.{HtmlContent, Key, Value}
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.{Content, Text}
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.{ActionItem, Actions, SummaryListRow}
import utils.UserAnswers

import java.time.LocalDate
import java.time.format.DateTimeFormatter

trait CYAHelper {

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

  protected def answerBooleanTransform(implicit messages: Messages): Option[Boolean => Text] = Some(opt => Text(Messages(s"booleanAnswer.${opt.toString}")))
  protected val answerStringTransform: Option[String => Text] = Some(opt => Text(opt))
  protected def answerBenefitsProvisionTypeTransform(implicit messages: Messages): Option[BenefitsProvisionType => Text] = Some(opt => Text(Messages(s"howProvideBenefits.${opt.toString}")))
  protected def answerPersonNameTransform(implicit messages: Messages): Option[PersonName => Text] = Some(opt => Text(Messages(s"${opt.fullName}")))
  protected def answerBenefitsTypeTransform(implicit messages: Messages): Option[BenefitsType => Text] = Some(opt => Text(Messages(s"benefitsType.${opt.toString}")))
  protected def referenceValueTransform(implicit messages: Messages): Option[ReferenceValue => Text] = Some(opt => Text(Messages(s"${opt.value}")))
  protected val answerDateTransform: Option[LocalDate => Text] = Some(date => Text(date.format(DateTimeFormatter.ofPattern("d-M-yyyy"))))
  protected def answerAddressTransform(implicit messages: Messages): Option[Address => HtmlContent] = Some(opt => addressAnswer(opt))

  def booleanToText: Boolean => String = bool => if (bool) "site.yes" else "site.no"
  def booleanToContent(implicit messages: Messages): Boolean => Content = bool => if (bool) Text(Messages("site.yes")) else Text(Messages("site.no"))

  private val attachDynamicIndex: (Map[String, String], Int) => Map[String, String] = (attributeMap, index) => {
    val attribute = attributeMap.getOrElse("id", s"add")
    Map("id" -> s"cya-0-${index.toString}-$attribute")
  }
  val rowsWithDynamicIndices: Seq[SummaryListRow] => Seq[SummaryListRow] = rows => rows.zipWithIndex.map { case (row, index) =>
    val newActions = row.actions.map { act =>
      val newItems = act.items.map { item =>
        item.copy(attributes = attachDynamicIndex(item.attributes, index))
      }
      act.copy(items = newItems)
    }
    row.copy(actions = newActions)
  }

  def actionAdd(optionURL: Option[String], visuallyHiddenText: Option[Text])(implicit messages: Messages): Option[Actions] = {
    val addVisuallyHidden = visuallyHiddenText.map {
      visuallyHiddn => messages("site.add") + " " + visuallyHiddn.value
    }
    optionURL.map { url =>
      Actions(items =
        Seq(ActionItem(
          content = HtmlContent(s"<span aria-hidden=true >${messages("site.add")}</span>"),
          href = url,
          visuallyHiddenText = addVisuallyHidden,
          attributes = Map("id" -> "change")
        ))
      )
    }
  }

  def actionChange(optionURL: Option[String], visuallyHiddenText: Option[Text])(implicit
    messages: Messages): Option[Actions] = {
    val changeVisuallyHidden = visuallyHiddenText.map {
      visuallyHiddn => messages("site.change") + " " + visuallyHiddn.value
    }
    optionURL.map { url =>
      Actions( items =
        Seq(ActionItem(
          content = HtmlContent(s"<span aria-hidden=true >${messages("site.change")}</span>"),
          href = url,
          visuallyHiddenText = changeVisuallyHidden,
          attributes = Map("id" -> "change")
        ))
      )
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
        SummaryListRow(
          key = Key(content = Text(Messages(message)), classes = "govuk-!-width-one-half"),
          value = Value(content = Text(Messages("site.incomplete")), classes = "govuk-!-width-one-third"),
          actions = actionAdd(url, visuallyHiddenText)
        )
      case Some(answer) =>
        SummaryListRow(
          key = Key(content = Text(Messages(message)), classes = "govuk-!-width-one-half"),
          value = answerTransform.fold(Value(Text(answer.toString)))(transform => Value(transform(answer))),
          actions = actionChange(url, visuallyHiddenText)
        )
    }

  def answerRow(message: String,
    answer: String,
    url: Option[String] = None,
    visuallyHiddenText: Option[Text] = None)
    (implicit messages: Messages): SummaryListRow = {
    SummaryListRow(
      key = Key(content = Text(Messages(message)), classes = "govuk-!-width-one-half"),
      value = Value(content = Text(answer)),
      actions = actionChange(url, visuallyHiddenText)
    )
  }

  def addRow(message: String,
                   url: Option[String] = None,
                   visuallyHiddenText: Option[Text] = None)
                  (implicit messages: Messages): SummaryListRow = {
    SummaryListRow(
      key = Key(content = Text(Messages(message)), classes = "govuk-!-width-one-half"),
      value = Value(content = Text(Messages("site.incomplete")), classes = "govuk-!-width-one-third"),
      actions = actionAdd(url, visuallyHiddenText)
    )
  }

}

object CYAHelper {
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

case class MandatoryAnswerMissingException(missingField:String)
  extends RuntimeException(s"An answer which was mandatory is missing from scheme details returned from TPSS: $missingField")
