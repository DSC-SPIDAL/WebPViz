name := """WebPViz"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayJava)

scalaVersion := "2.11.1"

libraryDependencies ++= Seq(
  javaJdbc,
  javaEbean,
  cache,
  "org.mindrot" % "jbcrypt" % "0.3m",
  "com.opencsv" % "opencsv" % "3.3",
  "commons-io" % "commons-io" % "2.4",
  "org.mongodb" % "mongo-java-driver" % "3.0.4",
  javaWs
)

resolvers ++= Seq(
  "jBCrypt Repository" at "http://repo1.maven.org/maven2/org/"
)

