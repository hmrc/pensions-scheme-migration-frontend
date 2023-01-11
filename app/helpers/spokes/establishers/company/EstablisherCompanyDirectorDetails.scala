/*
 * Copyright 2023 HM Revenue & Customs
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

package helpers.spokes.establishers.company

import controllers.establishers.company.director.details.routes._
import helpers.spokes.Spoke
import models.Index.indexToInt
import models.{Index, NormalMode, SpokeTaskListLink}
import play.api.i18n.Messages
import services.DataPrefillService
import utils.UserAnswers


case class EstablisherCompanyDirectorDetails(
                                         index: Index,
                                         answers: UserAnswers,
                                         dataPrefillService: DataPrefillService
                                       ) extends Spoke {
  private val messageKeyPrefix = "messages__schemeTaskList__directors_"
  private val isDirectorNotExists= answers.allDirectorsAfterDelete(indexToInt(index)).isEmpty
  private val noOfIndividualTrustees = dataPrefillService.getListOfTrusteesToBeCopied(index)(answers).size
  private val linkKeyAndRoute: (String, String) = {
    isDirectorNotExists match {
      case true if noOfIndividualTrustees > 1 =>
        (if(isDirectorNotExists) s"${messageKeyPrefix}addLink" else s"${messageKeyPrefix}changeLink",
          controllers.establishers.company.director.routes.TrusteesAlsoDirectorsController.onPageLoad(index).url)
      case true if noOfIndividualTrustees == 1 =>
        (if(isDirectorNotExists) s"${messageKeyPrefix}addLink" else s"${messageKeyPrefix}changeLink",
          controllers.establishers.company.director.routes.TrusteeAlsoDirectorController.onPageLoad(index).url)
      case true =>
        (s"${messageKeyPrefix}addLink", WhatYouWillNeedController.onPageLoad(index).url)
      case _ =>
        (s"${messageKeyPrefix}changeLink", controllers.establishers.company.routes.AddCompanyDirectorsController.onPageLoad(index,NormalMode).url)
    }
  }

  override def changeLink(name: String)
                         (implicit messages: Messages): SpokeTaskListLink =
    SpokeTaskListLink(
      text = Messages(linkKeyAndRoute._1, name),
      target = linkKeyAndRoute._2,
      visuallyHiddenText = None
    )

  override def completeFlag(answers: UserAnswers): Option[Boolean] = None
}

