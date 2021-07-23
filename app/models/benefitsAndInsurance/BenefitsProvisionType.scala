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

package models.benefitsAndInsurance

import play.api.data.Form
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

  def radios(form: Form[_]): Seq[Radios.Item] = {

    val field = form("value")
    val items = Seq(
      Radios.Radio(msg"howProvideBenefits.moneyPurchaseOnly", MoneyPurchaseOnly.toString),
      Radios.Radio(msg"howProvideBenefits.definedBenefitsOnly", DefinedBenefitsOnly.toString),
      Radios.Radio(msg"howProvideBenefits.mixedBenefits", MixedBenefits.toString)
    )

    Radios(field, items)
  }

  implicit val enumerable: Enumerable[BenefitsProvisionType] =
    Enumerable(values.map(v => v.toString -> v): _*)

}
