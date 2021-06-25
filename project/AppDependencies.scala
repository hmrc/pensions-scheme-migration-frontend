import sbt._

object AppDependencies {
  import play.core.PlayVersion.current
  val compile = Seq(
    "uk.gov.hmrc"                   %% "bootstrap-frontend-play-27"     % "5.3.0",
    "uk.gov.hmrc"                   %% "play-nunjucks"                  % "0.28.0-play-27",
    "uk.gov.hmrc"                   %% "play-nunjucks-viewmodel"        % "0.14.0-play-27",
    "org.webjars.npm"               %  "govuk-frontend"                 % "3.7.0",
    "org.webjars.npm"               %  "hmrc-frontend"                  % "1.19.0",
    "uk.gov.hmrc"                   %% "play-conditional-form-mapping"  % "1.9.0-play-27",
    "uk.gov.hmrc"                   %% "play-language"                  % "5.1.0-play-27",
    "com.google.inject.extensions"  %  "guice-multibindings"            % "4.2.2",
    "uk.gov.hmrc"                   %% "domain"                         % "5.11.0-play-27"
  )

  val test = Seq(
    "uk.gov.hmrc"             %% "bootstrap-test-play-27"   % "4.1.0" % Test,
    "org.scalatest"           %% "scalatest"                % "3.0.7"  % Test,
    "org.jsoup"               %  "jsoup"                    % "1.13.1" % Test,
    "com.typesafe.play"       %% "play-test"                % current  % Test,
    "com.vladsch.flexmark"    %  "flexmark-all"             % "0.36.8" % "test, it",
    "org.scalatestplus.play"  %% "scalatestplus-play"       % "4.0.2",
    "org.mockito"             %  "mockito-all"              % "1.10.19",
    "org.scalacheck"          %% "scalacheck"               % "1.14.0",
    "com.github.tomakehurst"  %  "wiremock-jre8"            % "2.21.0",
    "org.pegdown"             %  "pegdown"                  % "1.6.0",
    "org.mockito"             %  "mockito-all"              % "1.10.19"
  )
}
