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

package services

import com.google.inject.Inject
import models.{ListOfSchemes, SchemeDetails}
import uk.gov.hmrc.http.HeaderCarrier
import utils.SchemeFuzzyMatcher

import scala.concurrent.{ExecutionContext, Future}

class SchemeSearchService @Inject()(fuzzyMatching: SchemeFuzzyMatcher) {

  private val pstrRegex = "^[0-9]{8}[A-Za-z]{2}$".r

  private val filterSchemesByPstrOrSchemeName
  : (String, List[SchemeDetails]) => List[SchemeDetails] =
    (searchText, list) => {
      searchText match {
        case _ if pstrRegex.findFirstIn(searchText).isDefined =>
          list.filter(_.pstr.exists(_ == searchText))
        case _ =>
          list.flatMap { schemeDetail =>
            val isMatch = fuzzyMatching.doFuzzyMatching(searchText, schemeDetail.name)
            if (isMatch) Some(schemeDetail) else None
          }
      }
    }

  def search(psaId: String, searchText: Option[String])(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[List[SchemeDetails]] = {
    val listOfSchemes = ListOfSchemes("2", Some(List(
      SchemeDetails("scheme 1", None, "1111"),
      SchemeDetails("scheme 2", None, "2222")
    )))

//    listSchemesConnector.getListOfSchemes(psaId).map {
//        case Right(listOfSchemes) =>
//          val filterSearchResults =
//            searchText.fold[List[SchemeDetails] => List[SchemeDetails]](identity)(
//              st => filterSchemesByPstrOrSchemeName(st, _: List[SchemeDetails])
//            )
//
//          filterSearchResults(listOfSchemes.schemeDetails.getOrElse(List.empty[SchemeDetails]))
//        case _ => List.empty[SchemeDetails]
//      }


    val filterSearchResults =
          searchText.fold[List[SchemeDetails] => List[SchemeDetails]](identity)(
            st => filterSchemesByPstrOrSchemeName(st, _: List[SchemeDetails])
          )

    Future.successful(filterSearchResults(listOfSchemes.schemeDetails.getOrElse(List.empty[SchemeDetails])))
  }

}
