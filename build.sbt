import play.sbt.routes.RoutesKeys
import scoverage.ScoverageKeys

val appName = "pensions-scheme-migration-frontend"

val silencerVersion = "1.7.0"

lazy val microservice = Project(appName, file("."))
  .disablePlugins(JUnitXmlReportPlugin)
  .enablePlugins(play.sbt.PlayScala, SbtDistributablesPlugin)
  .settings(
    name                             := appName,
    majorVersion                     := 0,
    scalaVersion                     := "3.6.4",
    scalacOptions := Seq(
      "-deprecation",
      "-feature",
      "-unchecked",
      "-encoding", "utf8",
      "-Wconf:src=routes/.*:s",
      "-Wconf:cat=unused-imports&src=html/.*:s"
    ),
    libraryDependencies              ++= AppDependencies.all,
    PlayKeys.playDefaultPort         := 8213,
    TwirlKeys.templateImports ++= Seq(
      "config.AppConfig"
    ),
    RoutesKeys.routesImport ++= Seq(
      "models.Index",
      "models.establishers.EstablisherKind",
      "models.trustees.TrusteeKind",
      "models.Mode",
      "models.CheckMode",
      "models.NormalMode",
      "models.MigrationType",
      "models.Scheme",
      "models.RacDac",
      "models.entities._"
    ),
    TwirlKeys.templateImports ++= Seq(
      "play.twirl.api.HtmlFormat",
      "play.twirl.api.HtmlFormat._",
      "uk.gov.hmrc.govukfrontend.views.html.components._",
      "uk.gov.hmrc.hmrcfrontend.views.Implicits._",
      "uk.gov.hmrc.hmrcfrontend.views.html.components._",
      "uk.gov.hmrc.hmrcfrontend.views.html.helpers._",
      "viewmodels.govuk.all._",
    ),
// prevent removal of unused code which generates warning errors due to use of third-party libs
    uglifyCompressOptions := Seq("unused=false", "dead_code=false"),
// below line required to force asset pipeline to operate in dev rather than only prod
    // Removed uglify due to node 20 compile issues.
    // Suspected cause minification of already minified location-autocomplete.min.js -Pavel Vjalicin
    Assets / pipelineStages := Seq(concat),
    resolvers ++= Seq(
    Resolver.jcenterRepo,
    ),
    ScoverageKeys.coverageExcludedFiles := "<empty>;Reverse.*;.*filters.*;.*handlers.*;.*components.*;.*models.*;.*repositories.*;" +
      ".*BuildInfo.*;.*javascript.*;.*Routes.*;.*GuiceInjector;.*UserAnswersCacheConnector;" +
      ".*ControllerConfiguration;.*LanguageSwitchController;.*LanguageSelect.*;.*TestMongoPage.*;.*ErrorTemplate.*",
    ScoverageKeys.coverageMinimumStmtTotal := 80,
    ScoverageKeys.coverageFailOnMinimum := true,
    ScoverageKeys.coverageHighlighting := true,
    retrieveManaged := true
  )

lazy val testSettings: Seq[Def.Setting[_]] = Seq(
  javaOptions ++= Seq(
    "-Dconfig.resource=test.application.conf"
  )
)
