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

import play.api.data.Form
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.Aliases.RadioItem
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.Text
import utils.{Enumerable, WithName}

sealed trait Members

object Members {
  val values: Seq[Members] = Seq(
    None, One, TwoToEleven, TwelveToFifty, FiftyOneToTenThousand, MoreThanTenThousand
  )

  def radios(form: Form[?])(implicit messages: Messages): Seq[RadioItem] = {
    values.map(value =>
      RadioItem(
        content = Text(Messages(s"members.${value.toString}")),
        value = Some(value.toString),
        checked = form("value").value.contains(value.toString)
      )
    )
  }

  case object None extends WithName("opt1") with Members

  case object One extends WithName("opt2") with Members

  case object TwoToEleven extends WithName("opt3") with Members

  case object TwelveToFifty extends WithName("opt4") with Members

  case object FiftyOneToTenThousand extends WithName("opt5") with Members

  case object MoreThanTenThousand extends WithName("opt6") with Members

  implicit val enumerable: Enumerable[Members] =
    Enumerable(values.map(v => v.toString -> v)*)
}
