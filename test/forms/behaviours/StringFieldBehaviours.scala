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

package forms.behaviours

import play.api.data.{Form, FormError}

trait StringFieldBehaviours extends FieldBehaviours {


  def fieldWithRegex(form: Form[?],
                     fieldName: String,
                     invalidString: String,
                     error: FormError): Unit = {

    "not bind strings invalidated by regex" in {
      val result = form.bind(Map(fieldName -> invalidString)).apply(fieldName)
      result.errors mustEqual Seq(error)
    }
  }

  def fieldWithTransform[A, B](form: Form[A],
                               transformName: String,
                               data: Map[String, String],
                               expected: B,
                               actual: A => B): Unit = {
    s"apply field transform $transformName" in {
      val result = form.bind(data)
      result.errors.size mustBe 0
      actual(result.get) mustBe expected
    }
  }

  def formWithTransform[A](form: Form[A],
                           data: Map[String, String],
                           expectedData: A): Unit = {
    s"bind the form with the transformation" in {
      val result = form.bind(data)
      result.errors.size mustBe 0
      result.get mustBe expectedData
    }
  }

}
