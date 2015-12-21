name := """WebPViz"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayJava)

scalaVersion := "2.11.6"

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
  javaWs,
  "org.pac4j" % "play-pac4j-java" % "2.0.1-SNAPSHOT",
  "org.pac4j" % "pac4j-http" % "1.8.2",
  "org.pac4j" % "pac4j-cas" % "1.8.2",
  "org.pac4j" % "pac4j-openid" % "1.8.2",
  "org.pac4j" % "pac4j-oauth" % "1.8.2",
  "org.pac4j" % "pac4j-saml" % "1.8.2",
  "org.pac4j" % "pac4j-oidc" % "1.8.2",
  "org.pac4j" % "pac4j-gae" % "1.8.2",
  "org.pac4j" % "pac4j-jwt" % "1.8.2",
  "org.pac4j" % "pac4j-ldap" % "1.8.2",
  "org.pac4j" % "pac4j-sql" % "1.8.2",
  "org.pac4j" % "pac4j-mongo" % "1.8.2",
  "org.pac4j" % "pac4j-stormpath" % "1.8.2"
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