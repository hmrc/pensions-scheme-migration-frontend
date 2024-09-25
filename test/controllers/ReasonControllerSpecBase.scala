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

package controllers

import controllers.actions.MutableFakeDataRetrievalAction
import forms.ReasonFormProvider
import models.{Mode, NormalMode, PersonName}
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceableModule
import services.common.details.CommonReasonService
import utils.Data

trait ReasonControllerSpecBase extends ControllerSpecBase {
  val personName: PersonName = PersonName("Jane", "Doe")

  val partnershipName = "test partnership"
  val companyName = "test company"
  val formProvider: ReasonFormProvider = new ReasonFormProvider()

  val schemeName: String = Data.schemeName
  val isPageHeading = true

  val mutableFakeDataRetrievalAction: MutableFakeDataRetrievalAction = new MutableFakeDataRetrievalAction()
  val mockCommonReasonService: CommonReasonService = mock[CommonReasonService]
  val extraModules: Seq[GuiceableModule] = Seq(
    bind[CommonReasonService].to(mockCommonReasonService)
  )

  val application: Application = applicationBuilderMutableRetrievalAction(mutableFakeDataRetrievalAction, extraModules).build()

  val formData: String = "Reason"

  val validValues: Map[String, Seq[String]] = Map("value" -> Seq("Reason"))
  val invalidValues: Map[String, Seq[String]] = Map("value" -> Seq(""))

  val establisherIndex = 1
  val directorIndex = 1
  val partnerIndex = 1
  val index = 1
  val mode: Mode = NormalMode
}
