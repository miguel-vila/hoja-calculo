lazy val commonSettings = Seq(
  version := "0.0.0",
  scalaVersion := "2.11.7",
  scalacOptions ++= Seq(
    "-deprecation",
    "-encoding", "UTF-8",
    "-feature",//para poder ver feature warnings al compilar
    "-language:postfixOps", //para cosas como '5 seconds'
    "-language:implicitConversions",
    "-language:existentials",
    "-language:higherKinds",
    "-unchecked",
    "-language:reflectiveCalls",
    "-Xfatal-warnings",
    "-Xlint",
    "-Yno-adapted-args",
    "-Ywarn-dead-code",        // N.B. doesn't work well with the ??? hole
                               //"-Ywarn-numeric-widen",
                               //"-Ywarn-value-discard", // No muy buena idea combinar esto con akka
    "-Xfuture"
  ),
  resolvers ++= Seq(
    "Sonatype Releases"   at "http://oss.sonatype.org/content/repositories/releases",
    "Sonatype Snapshots"  at "http://oss.sonatype.org/content/repositories/snapshots",
    "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/",
    "miguel's maven repo" at "http://dl.bintray.com/miguelvilag/maven/"
  )
)

lazy val http4sVersion = "0.14.7"

val http4s = Seq(
  "org.http4s" %% "http4s-dsl" % http4sVersion,
  "org.http4s" %% "http4s-blaze-server" % http4sVersion,
  "org.http4s" %% "http4s-blaze-client" % http4sVersion,
  "org.slf4j" % "slf4j-simple" % "1.7.21"
)

lazy val root = project.in(file(".")).
  settings(commonSettings: _*)
  .aggregate(client, server, spreadSheetJS, spreadSheetJVM)
  .dependsOn(client, server, spreadSheetJS, spreadSheetJVM)
  .settings(
    publish := {},
    publishLocal := {},
    mainClass in Compile := Some("spreadsheet.Server"),
    stage := (stage in (server, Compile)).value
  )

lazy val server = project.in(file("server"))
  .enablePlugins(JavaServerAppPackaging)
  .settings(commonSettings: _*)
  .settings(
  name := "spreadsheet-server",
  persistLauncher := true,
  libraryDependencies ++= http4s,
  resources in Compile ++= {
    def andSourceMap(aFile: java.io.File) = Seq(
      aFile,
      file(aFile.getAbsolutePath + ".map")
    )
    andSourceMap((fastOptJS in (client, Compile)).value.data)
  })
  .dependsOn(spreadSheetJVM)

lazy val client = project.in(file("client"))
  .settings(commonSettings: _*)
  .enablePlugins(ScalaJSPlugin)
  .settings(
  persistLauncher in Compile := true,
  libraryDependencies ++= Seq(
	  "org.scala-js" %%% "scalajs-dom" % "0.9.1",
    "org.scala-js" %% "scala-parser-combinators_sjs0.6" % "1.0.2"
  ))
  .dependsOn(spreadSheetJS)

lazy val spreadSheet = crossProject
  .settings(commonSettings: _*)
  .settings(
    name := "spreadsheet-shared",
    libraryDependencies ++= Seq(
      "com.dallaway.richard" %%% "woot-model" % "0.1.1",
      "com.lihaoyi" %%% "upickle" % "0.4.1",
      "org.scalatest" %%% "scalatest" % "3.0.0-M10" % "test"
    )
  )

lazy val spreadSheetJVM: Project = spreadSheet.jvm
lazy val spreadSheetJS : Project = spreadSheet.js
