enablePlugins(ScalaJSPlugin)

name := "scala-js-spreadsheet"

version := "0.0.0"

scalaVersion := "2.11.8"

persistLauncher in Compile := true

scalacOptions ++= Seq(
  "-deprecation",
  "-encoding", "UTF-8",
  "-feature",//para poder ver feature warnings al compilar
  "-language:postfixOps", //para cosas como '5 seconds'
  "-language:implicitConversions",
  "-language:existentials",
  "-language:higherKinds",
  "-unchecked",
  "-language:reflectiveCalls", // para poder utilizar el .$each de la librería de mongodb
  "-Xfatal-warnings",
  "-Xlint",
  "-Yno-adapted-args",
  "-Ywarn-dead-code",        // N.B. doesn't work well with the ??? hole
  //"-Ywarn-numeric-widen",
  //"-Ywarn-value-discard", // No muy buena idea combinar esto con akka
  "-Xfuture"
)

resolvers ++= Seq(
    "Sonatype Releases"   at "http://oss.sonatype.org/content/repositories/releases",
    "Sonatype Snapshots"  at "http://oss.sonatype.org/content/repositories/snapshots",
    "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/",
    "spray repo"          at "http://repo.spray.io"
)

val scalazV = "7.1.0"

libraryDependencies ++= Seq(
	"org.scala-js" %%% "scalajs-dom" % "0.9.1",
  "org.scala-js" %% "scala-parser-combinators_sjs0.6" % "1.0.2"
//  "org.scalaz"          %%  "scalaz-core"                   % scalazV
)
