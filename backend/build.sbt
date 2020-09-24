name := "BookSwap"

scalaVersion := "2.13.3"

libraryDependencies ++= Seq(
  "com.beachape" %% "enumeratum" % "1.6.1",
  "org.typelevel" %% "cats-core" % "2.0.0",
  "org.scalatest" %% "scalatest" % "3.2.0" % "test"
)

scalacOptions ++= Seq(
  "-Xfatal-warnings",
  "-Ywarn-dead-code",
  "-Ywarn-numeric-widen",
  "-Ywarn-value-discard",
  "-Ywarn-unused"
)

addCompilerPlugin("org.typelevel" %% "kind-projector" % "0.11.0" cross CrossVersion.full)
