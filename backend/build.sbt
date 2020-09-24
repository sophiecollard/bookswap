name := "BookSwap"

scalaVersion := "2.13.3"

libraryDependencies ++= {
  val doobieVersion = "0.9.0"

  Seq(
    "com.beachape"  %% "enumeratum"       % "1.6.1",
    "org.tpolecat"  %% "doobie-core"      % doobieVersion,
    "org.tpolecat"  %% "doobie-hikari"    % doobieVersion,
    "org.tpolecat"  %% "doobie-postgres"  % doobieVersion,
    "org.tpolecat"  %% "doobie-scalatest" % doobieVersion,
    "org.typelevel" %% "cats-core"        % "2.0.0",
    "org.scalatest" %% "scalatest"        % "3.2.0" % Test
  )
}

scalacOptions ++= Seq(
  "-Xfatal-warnings",
  "-Ywarn-dead-code",
  "-Ywarn-numeric-widen",
  "-Ywarn-value-discard",
  "-Ywarn-unused"
)

addCompilerPlugin("org.typelevel" %% "kind-projector" % "0.11.0" cross CrossVersion.full)
