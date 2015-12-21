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
  "mysql" % "mysql-connector-java" % "5.1.21",
  "com.google.code.gson" % "gson" % "2.5",
  javaWs,
  "org.pac4j" % "play-pac4j_java" % "1.4.0" exclude("com.nimbusds", "nimbus-jose-jwt"),
  "org.pac4j" % "pac4j-http" % "1.7.0" exclude("com.nimbusds", "nimbus-jose-jwt"),
  "org.pac4j" % "pac4j-cas" % "1.7.0" exclude("com.nimbusds", "nimbus-jose-jwt"),
  "org.pac4j" % "pac4j-openid" % "1.7.0" exclude("com.nimbusds", "nimbus-jose-jwt"),
  "org.pac4j" % "pac4j-oauth" % "1.7.0" exclude("com.nimbusds", "nimbus-jose-jwt"),
  "org.pac4j" % "pac4j-saml" % "1.7.0"  exclude("com.nimbusds", "nimbus-jose-jwt"),
  "org.pac4j" % "pac4j-oidc" % "1.7.0" exclude("com.nimbusds", "nimbus-jose-jwt"),
  "com.typesafe.play" % "play-cache_2.11" % "2.3.0",
  "com.nimbusds" % "nimbus-jose-jwt" % "4.9"
)

resolvers ++= Seq(
  "jBCrypt Repository" at "http://repo1.maven.org/maven2/org/",
  "scribe-java-mvn-repo" at  "https://raw.github.com/fernandezpablo85/scribe-java/mvn-repo/"
)

