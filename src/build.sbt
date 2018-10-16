import play.sbt.PlayImport

name := """HshHelper"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayJava, PlayEbean)

scalaVersion := "2.12.6"

crossScalaVersions := Seq("2.11.12", "2.12.4")

libraryDependencies += guice
libraryDependencies += "org.mindrot" % "jbcrypt" % "0.4"


// Test Database
libraryDependencies += "com.h2database" % "h2" % "1.4.197"

// needed to run the evolutions during startup
libraryDependencies += PlayImport.evolutions

libraryDependencies += "org.mindrot" % "jbcrypt" % "0.4"
// Testing libraries for dealing with CompletionStage...
//libraryDependencies += "org.assertj" % "assertj-core" % "3.6.2" % Test
//libraryDependencies += "org.awaitility" % "awaitility" % "2.0.0" % Test

// Make verbose tests
//testOptions in Test := Seq(Tests.Argument(TestFrameworks.JUnit, "-a", "-v"))