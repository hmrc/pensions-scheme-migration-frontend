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

package utils

case class FakeCountryOptions(_options: Seq[InputOption]) extends CountryOptions(null, null) {
  override lazy val options: Seq[InputOption] = _options
}

object FakeCountryOptions {
  def testData: FakeCountryOptions = FakeCountryOptions(
    Seq(
      InputOption("GB", "United Kingdom"),
      InputOption("PN", "Ponteland")
    )
  )
}