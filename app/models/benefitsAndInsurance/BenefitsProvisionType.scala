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

package models.benefitsAndInsurance

import play.api.data.Form
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.viewmodels.label.Label
import uk.gov.hmrc.govukfrontend.views.viewmodels.radios.RadioItem
import uk.gov.hmrc.viewmodels.{Radios, _}
import utils.{Enumerable, InputOption, WithName}

sealed trait BenefitsProvisionType

object BenefitsProvisionType extends Enumerable.Implicits {

  case object MoneyPurchaseOnly extends WithName("moneyPurchaseOnly") with BenefitsProvisionType
  case object DefinedBenefitsOnly extends WithName("definedBenefitsOnly") with BenefitsProvisionType
  case object MixedBenefits extends WithName("mixedBenefits") with BenefitsProvisionType

  val values: Seq[BenefitsProvisionType] = Seq(
    MoneyPurchaseOnly, DefinedBenefitsOnly, MixedBenefits
  )

  def options: Seq[InputOption] =
    Seq(
      MoneyPurchaseOnly, DefinedBenefitsOnly, MixedBenefits
    ) map { value =>
      InputOption(value.toString, s"benefitsProvisionType.${value.toString}")
    }

  def radios(form: Form[_])(implicit messages: Messages): Seq[RadioItem] = {
    val field = form("value")
    Seq(
      RadioItem(
        label = Some(Label(Some(Messages("howProvideBenefits.moneyPurchaseOnly")))),
        value = Some(MoneyPurchaseOnly.toString),
        checked = field.value.contains(MoneyPurchaseOnly.toString)
      ),
      RadioItem(
        label = Some(Label(Some(Messages("howProvideBenefits.definedBenefitsOnly")))),
        value = Some(DefinedBenefitsOnly.toString),
        checked = field.value.contains(DefinedBenefitsOnly.toString)
      ),
      RadioItem(
        label = Some(Label(Some(Messages("howProvideBenefits.mixedBenefits")))),
        value = Some(MixedBenefits.toString),
        checked = field.value.contains(MixedBenefits.toString)
      )
    )
  }

  implicit val enumerable: Enumerable[BenefitsProvisionType] =
    Enumerable(values.map(v => v.toString -> v): _*)

}
