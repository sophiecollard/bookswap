name := "BookSwap"

scalaVersion := "2.13.3"

libraryDependencies ++= Seq(
  "com.beachape" %% "enumeratum" % "1.6.1",
  "org.typelevel" %% "cats-core" % "2.0.0"
)

scalacOptions ++= Seq(
  // "-Xfatal-warnings"
)

addCompilerPlugin("org.typelevel" %% "kind-projector" % "0.11.0" cross CrossVersion.full)

