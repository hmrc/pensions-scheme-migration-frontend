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
    scalaVersion                     := "2.13.12",
    scalacOptions += "-Wconf:cat=unused-imports&src=html/.*:s",
    scalacOptions += "-Wconf:src=routes/.*:s",
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
      "models.RacDac"
    ),
      // concatenate js
      Concat.groups := Seq(
  "javascripts/application.js" -> group(
        Seq(
          "lib/govuk-frontend/govuk/all.js",
          "lib/hmrc-frontend/hmrc/all.js",
          "javascripts/psm.js"
        )
      )
    ),
// prevent removal of unused code which generates warning errors due to use of third-party libs
    uglifyCompressOptions := Seq("unused=false", "dead_code=false"),
// below line required to force asset pipeline to operate in dev rather than only prod
    Assets / pipelineStages := Seq(concat, uglify)
  )
  .configs(IntegrationTest)
  .settings(resolvers ++= Seq(
    Resolver.jcenterRepo,
    )
  )
  .settings(
    ScoverageKeys.coverageExcludedFiles := "<empty>;Reverse.*;.*filters.*;.*handlers.*;.*components.*;.*models.*;.*repositories.*;" +
      ".*BuildInfo.*;.*javascript.*;.*Routes.*;.*GuiceInjector;.*UserAnswersCacheConnector;" +
      ".*ControllerConfiguration;.*LanguageSwitchController;.*LanguageSelect.*;.*TestMongoPage.*;.*ErrorTemplate.*",
    ScoverageKeys.coverageMinimumStmtTotal := 80,
    ScoverageKeys.coverageFailOnMinimum := true,
    ScoverageKeys.coverageHighlighting := true,
    scalacOptions ++= Seq("-Xfatal-warnings", "-feature")
  )
  .settings(
    scalacOptions ++= Seq("-Xfatal-warnings", "-feature"),
    retrieveManaged := true,
    update / evictionWarningOptions := EvictionWarningOptions.default.withWarnScalaVersionEviction(false)
  )

