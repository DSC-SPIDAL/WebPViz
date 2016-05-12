name := """WebPViz"""

version := "1.0"

lazy val root = (project in file(".")).enablePlugins(PlayJava)

scalaVersion := "2.11.7"

libraryDependencies ++= Seq(
  javaJdbc,
  cache,
  evolutions,
  "org.mindrot" % "jbcrypt" % "0.3m",
  "com.opencsv" % "opencsv" % "3.3",
  "commons-io" % "commons-io" % "2.4",
  "org.mongodb" % "mongo-java-driver" % "3.0.4",
  "mysql" % "mysql-connector-java" % "5.1.21",
  "com.google.code.gson" % "gson" % "2.5",
  javaWs
)

resolvers ++= Seq(
  "jBCrypt Repository" at "http://repo1.maven.org/maven2/org/"
)

lazy val myProject = (project in file("."))
  .enablePlugins(PlayJava, PlayEbean)

resolvers ++= Seq( Resolver.mavenLocal,
  "Sonatype snapshots repository" at "https://oss.sonatype.org/content/repositories/snapshots/",
  "Pablo repo" at "https://raw.github.com/fernandezpablo85/scribe-java/mvn-repo/")

routesGenerator := InjectedRoutesGenerator
