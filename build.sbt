import Dependencies._

lazy val root = (project in file(".")).
  settings(
    inThisBuild(List(
      organization := "com.example",
      scalaVersion := "2.11.13",
      version      := "0.1.0-SNAPSHOT"
    )),
    name := "Hello",
    libraryDependencies ++= Seq(
      scalaTest % Test,
      "com.typesafe.slick" %% "slick"           % "3.2.2",
      "com.h2database"      % "h2"              % "1.4.185",
      "mysql" % "mysql-connector-java" % "6.0.6",
      "com.typesafe.slick" %% "slick-hikaricp" % "3.2.1",
      "com.hankcs" % "hanlp" % "portable-1.3.4",
      "ch.qos.logback"      % "logback-classic" % "1.2.3",
      "org.apache.spark" %% "spark-core" % "2.3.0",
      "org.apache.spark" %% "spark-mllib" % "2.3.0"
    )
  )
