package controllers.racdac

import com.google.inject.Inject
import config.AppConfig
import controllers.actions.AuthAction
import forms.racdac.RacDacBulkListFormProvider
import identifiers.establishers.AddEstablisherId
import models.requests.AuthenticatedRequest
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import renderer.Renderer
import services.SchemeSearchService
import uk.gov.hmrc.nunjucks.NunjucksSupport
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import uk.gov.hmrc.viewmodels.Radios

import scala.concurrent.{ExecutionContext, Future}

class BulkListController @Inject()(
                                    val appConfig: AppConfig,
                                    override val messagesApi: MessagesApi,
                                    authenticate: AuthAction,
                                    val controllerComponents: MessagesControllerComponents,
                                    formProvider: RacDacBulkListFormProvider,
                                    schemeSearchService: SchemeSearchService,
                                    renderer: Renderer
                                  )(implicit val ec: ExecutionContext)
  extends FrontendBaseController
    with I18nSupport with NunjucksSupport {


  def onPageLoad: Action[AnyContent] = (authenticate).async {
    implicit request =>
      schemeSearchService.searchAndRenderView(form(migrationType), pageNumber = 1, searchText = None, migrationType)
  }

  def onSubmit: Action[AnyContent] = authenticate.async {
    implicit request =>
    formProvider().bindFromRequest().fold(
        (formWithErrors: Form[String]) =>
          schemeSearchService.searchAndRenderView(formWithErrors, pageNumber = 1, searchText = None, migrationType),
      { case true =>
        Future.successful(Redirect(controllers.racdac.routes.DeclarationController.onPageLoad()))
      case _ =>
        Future.successful(Redirect(appConfig.psaOverviewUrl))
      }
      )
  }

}
