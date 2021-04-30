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

import com.google.inject.AbstractModule
import com.google.inject.multibindings.Multibinder
import navigators._

class PODSModule extends AbstractModule {

  //scalastyle:off method.length
  override def configure(): Unit = {

    val navigators = Multibinder.newSetBinder(binder(), classOf[Navigator])
    navigators.addBinding().to(classOf[BeforeYouStartNavigator])

    bind(classOf[CompoundNavigator]).to(classOf[CompoundNavigatorImpl])



//    bind(classOf[DataRetrievalAction]).to(classOf[DataRetrievalActionImpl]).asEagerSingleton()
//    bind(classOf[DataRequiredAction]).to(classOf[DataRequiredActionImpl]).asEagerSingleton()
//    bind(classOf[AllowSubmissionAction]).to(classOf[AllowSubmissionActionImpl]).asEagerSingleton()
//
//    // For session based storage instead of cred based, change to SessionIdentifierAction
//    bind(classOf[IdentifierAction]).to(classOf[AuthenticatedIdentifierAction]).asEagerSingleton()
//
//    bind(classOf[UserAnswersCacheConnector]).to(classOf[UserAnswersCacheConnectorImpl]).asEagerSingleton()
//    bind(classOf[CacheConnector]).to(classOf[FinancialInfoCacheConnector]).asEagerSingleton()
  }
}

