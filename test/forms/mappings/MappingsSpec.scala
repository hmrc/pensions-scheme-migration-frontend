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

package forms.mappings

import org.scalatest.OptionValues
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
import play.api.data.Forms._
import play.api.data.{Form, FormError}
import utils.{Enumerable, Generators}

import java.time.LocalDate

object MappingsSpec {

  sealed trait Foo

  case object Bar extends Foo

  case object Baz extends Foo

  object Foo {

    val values: Set[Foo] = Set(Bar, Baz)

    implicit val fooEnumerable: Enumerable[Foo] =
      Enumerable(values.toSeq.map(v => v.toString -> v): _*)
  }

}

class MappingsSpec extends AnyWordSpec with Matchers with OptionValues with Mappings with ScalaCheckDrivenPropertyChecks with Generators {

  import MappingsSpec._

  "boolean" must {

    val testForm: Form[Boolean] =
      Form(
        "value" -> boolean()
      )

    "bind true" in {
      val result = testForm.bind(Map("value" -> "true"))
      result.get mustEqual true
    }

    "bind false" in {
      val result = testForm.bind(Map("value" -> "false"))
      result.get mustEqual false
    }

    "not bind a non-boolean" in {
      val result = testForm.bind(Map("value" -> "not a boolean"))
      result.errors must contain(FormError("value", "error.boolean"))
    }

    "not bind an empty value" in {
      val result = testForm.bind(Map("value" -> ""))
      result.errors must contain(FormError("value", "error.required"))
    }

    "not bind an empty map" in {
      val result = testForm.bind(Map.empty[String, String])
      result.errors must contain(FormError("value", "error.required"))
    }

    "unbind" in {
      val result = testForm.fill(true)
      result.apply("value").value.value mustEqual "true"
    }
  }

  "int" must {

    val testForm: Form[Int] =
      Form(
        "value" -> int()
      )

    "bind a valid integer" in {
      val result = testForm.bind(Map("value" -> "1"))
      result.get mustEqual 1
    }

    "not bind an empty value" in {
      val result = testForm.bind(Map("value" -> ""))
      result.errors must contain(FormError("value", "error.required"))
    }

    "not bind an empty map" in {
      val result = testForm.bind(Map.empty[String, String])
      result.errors must contain(FormError("value", "error.required"))
    }

    "unbind a valid value" in {
      val result = testForm.fill(123)
      result.apply("value").value.value mustEqual "123"
    }
  }

  "enumerable" must {

    val testForm = Form(
      "value" -> enumerable[Foo]()
    )

    "bind a valid option" in {
      val result = testForm.bind(Map("value" -> "Bar"))
      result.get mustEqual Bar
    }

    "not bind an invalid option" in {
      val result = testForm.bind(Map("value" -> "Not Bar"))
      result.errors must contain(FormError("value", "error.invalid"))
    }

    "not bind an empty map" in {
      val result = testForm.bind(Map.empty[String, String])
      result.errors must contain(FormError("value", "error.required"))
    }
  }

  "date" must {
    case class TestClass(date: LocalDate)

    val testForm = Form(
      mapping(
        "date" -> dateMapping("messages__error__date", "error.invalid_date")
      )(TestClass.apply)(TestClass.unapply)
    )

    // scalastyle:off magic.number
    val testDate = LocalDate.of(1862, 6, 9)
    // scalastyle:on magic.number

    "bind valid data" in {
      val result = testForm.bind(
        Map(
          "date.day" -> testDate.getDayOfMonth.toString,
          "date.month" -> testDate.getMonthValue.toString,
          "date.year" -> testDate.getYear.toString
        )
      )

      result.errors.size mustBe 0
      result.get.date mustBe testDate
    }

    "not bind blank data" in {
      val result = testForm.bind(
        Map(
          "date.day" -> "",
          "date.month" -> "",
          "date.year" -> ""
        )
      )

      result.errors.size mustBe 3

      result.errors.contains(FormError("date.day", "error.date.day_blank")) mustBe true
      result.errors.contains(FormError("date.month", "error.date.month_blank")) mustBe true
      result.errors.contains(FormError("date.year", "error.date.year_blank")) mustBe true

    }

    "not bind non-numeric input" in {
      val result = testForm.bind(
        Map(
          "date.day" -> "A",
          "date.month" -> "B",
          "date.year" -> "C"
        )
      )

      result.errors.size mustBe 3

      result.errors.contains(FormError("date.day", "error.date.day_invalid")) mustBe true
      result.errors.contains(FormError("date.month", "error.date.month_invalid")) mustBe true
      result.errors.contains(FormError("date.year", "error.date.year_invalid")) mustBe true

    }

    "not bind invalid day" in {
      val result = testForm.bind(
        Map(
          "date.day" -> "0",
          "date.month" -> testDate.getMonthValue.toString,
          "date.year" -> testDate.getYear.toString
        )
      )

      result.errors.size mustBe 1
      result.errors must contain(FormError("date.day", "error.date.day_invalid"))
    }

    "not bind invalid month" in {
      val result = testForm.bind(
        Map(
          "date.day" -> testDate.getDayOfMonth.toString,
          "date.month" -> "0",
          "date.year" -> testDate.getYear.toString
        )
      )

      result.errors.size mustBe 1
      result.errors must contain(FormError("date.month", "error.date.month_invalid"))
    }

    "fill correctly from model" in {
      val testClass = TestClass(testDate)
      val result = testForm.fill(testClass)

      result("date.day").value.value mustBe "9"
      result("date.month").value.value mustBe "6"
      result("date.year").value.value mustBe "1862"
    }
  }
}
