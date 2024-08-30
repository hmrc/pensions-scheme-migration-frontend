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

import com.google.inject.AbstractModule
import com.google.inject.multibindings.Multibinder
import navigators._

class PODSModule extends AbstractModule {

  override def configure(): Unit = {

    val navigators = Multibinder.newSetBinder(binder(), classOf[Navigator])
    navigators.addBinding().to(classOf[BeforeYouStartNavigator])
    navigators.addBinding().to(classOf[BenefitsAndInsuranceNavigator])
    navigators.addBinding().to(classOf[AboutNavigator])
    navigators.addBinding().to(classOf[EstablishersNavigator])
    navigators.addBinding().to(classOf[EstablishersCompanyNavigator])
    navigators.addBinding().to(classOf[EstablishersCompanyDirectorNavigator])
    navigators.addBinding().to(classOf[EstablishersPartnershipNavigator])
    navigators.addBinding().to(classOf[EstablishersPartnerNavigator])
    navigators.addBinding().to(classOf[TrusteesNavigator])
    navigators.addBinding().to(classOf[TrusteesCompanyNavigator])
    navigators.addBinding().to(classOf[TrusteesPartnershipNavigator])
    navigators.addBinding().to(classOf[AdviserNavigator])
  }
}

