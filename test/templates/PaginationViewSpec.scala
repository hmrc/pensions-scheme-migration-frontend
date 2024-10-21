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

package templates

import controllers.ControllerSpecBase
import controllers.actions.MutableFakeDataRetrievalAction
import matchers.JsonMatchers
import play.api.Application
import play.api.test.Helpers._
import utils.Enumerable
import views.html.templates.PaginationLinks

class PaginationViewSpec extends ControllerSpecBase with JsonMatchers with Enumerable.Implicits  {


  private val mutableFakeDataRetrievalAction: MutableFakeDataRetrievalAction = new MutableFakeDataRetrievalAction()
  private val application: Application = applicationBuilderMutableRetrievalAction(mutableFakeDataRetrievalAction).build()


  "Pagination view" must {

    "display pagination links for multiple pages" in {
      val totalItems = 100
      val pagination = 10
      val numberOfPages = 10
      val pageNumber = 5
      val baseUrl = "/test/url?page="

      val view = application.injector.instanceOf[PaginationLinks].apply(
        totalItems, pagination, numberOfPages, pageNumber, baseUrl
      )(messages)

      val content = contentAsString(view)

      //current page
      content must include("""<a class="govuk-link govuk-pagination__link" href="/test/url?page=5" aria-current="page">5</a>""")

      // previous link
      content must include("""<a class="govuk-link govuk-pagination__link" href="/test/url?page=4" rel="prev">""")
      content must include("Previous")

      // next link
      content must include("""<a class="govuk-link govuk-pagination__link" href="/test/url?page=6" rel="next">""")
      content must include("Next")

      // last page
      content must include("""<a class="govuk-link govuk-pagination__link" href="/test/url?page=10" aria-label="Page 10">10</a>""")
    }

    "not display pagination when total items are less than or equal to the pagination limit" in {
      val totalItems = 5
      val pagination = 10
      val numberOfPages = 1
      val pageNumber = 1
      val baseUrl = "/test/url?page="

      val view = application.injector.instanceOf[PaginationLinks].apply(
        totalItems, pagination, numberOfPages, pageNumber, baseUrl
      )(messages)

      val content = contentAsString(view)

      content must not include ("""<nav class="govuk-pagination" aria-label="Pagination">""")
    }

    "display only the first and last pages when on the first or last page of many" in {
      val totalItems = 100
      val pagination = 10
      val numberOfPages = 10
      val pageNumber = 1
      val baseUrl = "/test/url?page="

      val view = application.injector.instanceOf[PaginationLinks].apply(
        totalItems, pagination, numberOfPages, pageNumber, baseUrl
      )(messages)

      val content = contentAsString(view)

      // first page
      content must include("""<a class="govuk-link govuk-pagination__link" href="/test/url?page=1" aria-label="Page 1">1</a>""")

      // last page
      content must include("""<a class="govuk-link govuk-pagination__link" href="/test/url?page=10" aria-label="Page 10">10</a>""")

      // next page
      content must include("""<a class="govuk-link govuk-pagination__link" href="/test/url?page=2" rel="next">""")
    }

    "display previous link when on the second page" in {
      val totalItems = 100
      val pagination = 10
      val numberOfPages = 10
      val pageNumber = 2
      val baseUrl = "/test/url?page="

      val view = application.injector.instanceOf[PaginationLinks].apply(
        totalItems, pagination, numberOfPages, pageNumber, baseUrl
      )(messages)

      val content = contentAsString(view)

      content must include("""<a class="govuk-link govuk-pagination__link" href="/test/url?page=1" rel="prev">""")
    }

    "display next link when not on the last page" in {
      val totalItems = 100
      val pagination = 10
      val numberOfPages = 10
      val pageNumber = 9
      val baseUrl = "/test/url?page="

      val view = application.injector.instanceOf[PaginationLinks].apply(
        totalItems, pagination, numberOfPages, pageNumber, baseUrl
      )(messages)

      val content = contentAsString(view)

      // next page
      content must include("""<a class="govuk-link govuk-pagination__link" href="/test/url?page=10" rel="next">""")
    }
  }
}
