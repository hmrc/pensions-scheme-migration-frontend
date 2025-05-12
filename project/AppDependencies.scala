import sbt.*

object AppDependencies {

  private val hmrcBootstrapVersion = "9.11.0"
  private val playVersion = "play-30"
  private val compile: Seq[ModuleID] = Seq(
    play.sbt.PlayImport.ws,
    "uk.gov.hmrc"                   %% s"bootstrap-frontend-$playVersion"             % hmrcBootstrapVersion,
    "org.webjars.npm"               %  "govuk-frontend"                               % "5.10.0",
    "uk.gov.hmrc"                   %% s"play-conditional-form-mapping-$playVersion"  % "3.3.0",
    "com.google.inject.extensions"  %  "guice-multibindings"                          % "4.2.3",
    "uk.gov.hmrc"                   %% s"domain-$playVersion"                         % "11.0.0",
    "com.fasterxml.jackson.module"  %% "jackson-module-scala"                         % "2.19.0",
    "uk.gov.hmrc"                   %% s"play-frontend-hmrc-$playVersion"             % "12.1.0"
  )


  val test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"             %% "bootstrap-test-play-30"   % hmrcBootstrapVersion  % Test,
    "org.mockito"             %% "mockito-scala"            % "1.17.37"             % Test,
    "org.scalatestplus"       %% "scalacheck-1-18"          % "3.2.19.0"            % Test
  )

  val all: Seq[ModuleID] = compile ++ test

}
