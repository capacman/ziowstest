import Dependencies._

ThisBuild / scalaVersion := "2.13.10"
ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / organization := "com.example"
ThisBuild / organizationName := "example"

lazy val root = (project in file("."))
  .settings(
    name := "ziowstest",
// https://mvnrepository.com/artifact/dev.zio/zio-http
    libraryDependencies ++= Seq("dev.zio" %% "zio-http" % "3.0.0-RC2")
  )

// See https://www.scala-sbt.org/1.x/docs/Using-Sonatype.html for instructions on how to publish to Sonatype.
