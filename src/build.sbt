import play.sbt.PlayImport

name := """HshHelper"""

version := "1.0-SNAPSHOT"
PlayKeys.externalizeResources := false

// Workaround for Annotation.class See https://github.com/scala/scala-dev/issues/249
scalacOptions in (Compile, doc) += "-no-java-comments"

lazy val root = (project in file(".")).enablePlugins(PlayJava, PlayEbean)

scalaVersion := "2.12.6"

crossScalaVersions := Seq("2.11.12", "2.12.4")
javacOptions += "-Xlint:deprecation"

libraryDependencies += guice

// Test Database
libraryDependencies += "com.h2database" % "h2" % "1.4.197"

// needed to run the evolutions during startup
libraryDependencies += PlayImport.evolutions
libraryDependencies += javaJdbc % Test

//needed for sending emails (password reset)
libraryDependencies += "com.typesafe.play" %% "play-mailer" % "6.0.1"
libraryDependencies += "com.typesafe.play" %% "play-mailer-guice" % "6.0.1"

// https://mvnrepository.com/artifact/com.github.ua-parser/uap-java
libraryDependencies += "com.github.ua-parser" % "uap-java" % "1.4.0"

libraryDependencies += "org.mindrot" % "jbcrypt" % "0.4"
// Testing libraries for dealing with CompletionStage...
libraryDependencies += "org.assertj" % "assertj-core" % "3.6.2" % Test
libraryDependencies += "org.awaitility" % "awaitility" % "2.0.0" % Test
libraryDependencies += "org.mockito" % "mockito-core" % "2.10.0" % "test"

// Make verbose tests
testOptions in Test := Seq(Tests.Argument(TestFrameworks.JUnit, "-a", "-v"))

libraryDependencies += "joda-time" % "joda-time" % "2.10"

libraryDependencies += "io.github.keetraxx" % "recaptcha" % "0.5"

libraryDependencies += ws

javaOptions in Test += "-Dconfig.file=conf/application.test.conf"

// Build options and steps

enablePlugins(UniversalPlugin)
enablePlugins(JavaAppPackaging)

def filterOut(name: String): Boolean = {
  ! (name.endsWith(".gitignore") ||
    name.endsWith("secrets.conf") ||
    name.endsWith("application.conf") ||
    name.endsWith("application.test.conf") ||
    name.endsWith("routes") ||
    name.endsWith("logback.xml") ||
    name.endsWith("logback-test.xml"))
}

mappings in (Compile, packageDoc) := Seq()

mappings in (Compile,packageBin) ~= {
  (ms: Seq[(File,String)]) =>
    ms filter { case (file, toPath) => filterOut(toPath) }
}

mappings in Universal ++= (baseDirectory.value / "conf" * "application.conf" get) map
  (x => x -> ("conf/" + x.getName))
mappings in Universal ++= (baseDirectory.value / "conf" * "secrets.conf" get) map
  (x => x -> ("conf/" + x.getName))
mappings in Universal ++= (baseDirectory.value / "conf" * "logback.xml" get) map
  (x => x -> ("conf/" + x.getName))
// add jvm parameter for typesafe config
bashScriptExtraDefines += """addJava "-Dconfig.file=${app_home}/../conf/application.conf""""
batScriptExtraDefines += """call :add_java "-Dconfig.file=%APP_HOME%\conf\application.conf""""