resolvers += "Typesafe repository" at "https://repo.typesafe.com/typesafe/releases/"

resolvers += "Sonatype snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/"

resolvers += "Artima Maven Repository" at "http://repo.artima.com/releases"

addSbtPlugin("org.scala-js" % "sbt-scalajs" % "0.6.12")

addSbtPlugin("com.typesafe.sbt" % "sbt-native-packager" % "1.1.1")
