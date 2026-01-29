import play.sbt.routes.RoutesKeys

val appName = "pensions-scheme-migration-frontend"

lazy val microservice = Project(appName, file("."))
  .disablePlugins(JUnitXmlReportPlugin)
  .enablePlugins(play.sbt.PlayScala, SbtDistributablesPlugin)
  .settings(
    name                             := appName,
    majorVersion                     := 0,
    scalaVersion                     := "3.6.4",
    scalacOptions ++= Seq(
      "-feature", // Enable feature warnings
      "-Xfatal-warnings", // Treat warnings as errors
      "-Wconf:src=routes/.*:silent", // Suppress warnings from routes files
      "-Wconf:src=twirl/.*:silent",  // Suppress warnings from twirl files
      "-Wconf:src=target/.*:silent", // Suppress warnings from target files
      "-Wconf:msg=Flag.*repeatedly:silent", // Suppress repeated flag warnings
      "-Wconf:msg=.*-Wunused.*:silent", // Suppress unused variable warnings
    ),
    libraryDependencies              ++= AppDependencies.all,
    PlayKeys.playDefaultPort         := 8213,
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
      "config.AppConfig",
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
    CodeCoverageSettings(),
    retrieveManaged := true
  )
