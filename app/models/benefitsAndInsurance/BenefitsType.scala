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
import utils.{InputOption, WithName, Enumerable}

sealed trait BenefitsType

object BenefitsType extends Enumerable.Implicits {

  case object CollectiveMoneyPurchaseBenefits extends WithName("collectiveMoneyPurchaseBenefits") with BenefitsType
  case object CashBalanceBenefits extends WithName("cashBalanceBenefits") with BenefitsType
  case object OtherMoneyPurchaseBenefits extends WithName("otherMoneyPurchaseBenefits") with BenefitsType
  case object CollectiveMoneyPurchaseAndCashBalanceBenefits extends WithName("collectiveMoneyPurchaseAndCashBalanceBenefits") with BenefitsType
  case object CashBalanceAndOtherMoneyPurchaseBenefits  extends WithName("cashBalanceAndOtherMoneyPurchaseBenefits") with BenefitsType

  val values: Seq[BenefitsType] = Seq(
    CollectiveMoneyPurchaseBenefits, CashBalanceBenefits, OtherMoneyPurchaseBenefits, CollectiveMoneyPurchaseAndCashBalanceBenefits,
    CashBalanceAndOtherMoneyPurchaseBenefits
  )

  def options: Seq[InputOption] =
    Seq(
      CollectiveMoneyPurchaseBenefits, CashBalanceBenefits, OtherMoneyPurchaseBenefits, CollectiveMoneyPurchaseAndCashBalanceBenefits,
      CashBalanceAndOtherMoneyPurchaseBenefits
    ) map { value =>
      InputOption(value.toString, s"benefitsProvisionType.${value.toString}")
    }

  def radios(form: Form[_]): Seq[Radios.Item] = {

    val field = form("value")
    val items = Seq(
      Radios.Radio(msg"benefitsType.collectiveMoneyPurchaseBenefits", CollectiveMoneyPurchaseBenefits.toString),
      Radios.Radio(msg"benefitsType.cashBalanceBenefits", CashBalanceBenefits.toString),
      Radios.Radio(msg"benefitsType.otherMoneyPurchaseBenefits", OtherMoneyPurchaseBenefits.toString),
      Radios.Radio(msg"benefitsType.collectiveMoneyPurchaseAndCashBalanceBenefits", CollectiveMoneyPurchaseAndCashBalanceBenefits.toString),
      Radios.Radio(msg"benefitsType.cashBalanceAndOtherMoneyPurchaseBenefits", CashBalanceAndOtherMoneyPurchaseBenefits.toString),
    )

    Radios(field, items)
  }

  implicit val enumerable: Enumerable[BenefitsType] =
    Enumerable(values.map(v => v.toString -> v): _*)

}
