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

package controllers.establishers.individual

import controllers.establishers.individual.details.{EstablisherDOBController, WhatYouWillNeedController, EstablisherHasNINOController}
import models.{Mode, Index}
import play.api.mvc.{Action, AnyContent}

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class EstablisherIndividualController @Inject()(
  establisherNameController: EstablisherNameController,
  whatYouWillNeedController: WhatYouWillNeedController,
  establisherDOBController:EstablisherDOBController,
  establisherHasNINOController:EstablisherHasNINOController
                                         )(implicit val executionContext: ExecutionContext) {

  def onPageLoad(index: Index, mode:Mode, page: String): Action[AnyContent] = {
    page match {
      case "name" => establisherNameController.onPageLoad(index)
      case "details" => whatYouWillNeedController.onPageLoad(index)
      case "date-of-birth" => establisherDOBController.onPageLoad(index, mode)
      case "have-national-insurance-number" => establisherHasNINOController.onPageLoad(index, mode)
      case _ => throw new RuntimeException("No route")
    }
  }

  def onSubmit(index: Index, mode:Mode, page: String): Action[AnyContent] = {
    page match {
      case "name" => establisherNameController.onSubmit(index)
    }
  }

}
