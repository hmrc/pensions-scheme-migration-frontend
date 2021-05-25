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

package helpers

import helpers.spokes.{AboutMembersSpoke, BeforeYouStartSpoke, Spoke}
import models.EntitySpoke
import utils.{Enumerable, UserAnswers}

class SpokeCreationService extends Enumerable.Implicits {

  def getBeforeYouStartSpoke(answers: UserAnswers, name: String): Seq[EntitySpoke] =
    Seq(createSpoke(answers, BeforeYouStartSpoke, name))

  def membershipDetailsSpoke(answers: UserAnswers, name: String): Seq[EntitySpoke] =
    Seq(createSpoke(answers, AboutMembersSpoke, name))

  def createSpoke(answers: UserAnswers, spoke: Spoke, name: String): EntitySpoke =
    EntitySpoke(spoke.changeLink(name), spoke.completeFlag(answers))

}
