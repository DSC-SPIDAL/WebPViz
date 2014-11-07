name := """WebPviz"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayJava)

scalaVersion := "2.11.1"

libraryDependencies ++= Seq(
  "org.webjars" % "bootstrap" % "3.3.0",
  javaJdbc,
  javaEbean,
  cache,
  javaWs
)
