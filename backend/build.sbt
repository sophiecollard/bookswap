name := "BookSwap"

scalaVersion := "2.13.3"

libraryDependencies ++= {
  val catsVersion = "2.2.0"
  val circeVersion = "0.12.3"
  val doobieVersion = "0.9.0"

  Seq(
    "com.beachape"  %% "enumeratum"        % "1.6.1",
    "com.beachape"  %% "enumeratum-doobie" % "1.6.0",
    "io.circe"      %% "circe-core"        % circeVersion,
    "io.circe"      %% "circe-parser"      % circeVersion,
    "org.tpolecat"  %% "doobie-core"       % doobieVersion,
    "org.tpolecat"  %% "doobie-hikari"     % doobieVersion,
    "org.tpolecat"  %% "doobie-postgres"   % doobieVersion,
    "org.tpolecat"  %% "doobie-scalatest"  % doobieVersion,
    "org.typelevel" %% "cats-core"         % catsVersion,
    "org.typelevel" %% "cats-effect"       % catsVersion,
    "org.scalatest" %% "scalatest"         % "3.2.0" % Test
  )
}

scalacOptions ++= Seq(
  "-deprecation",
  "-Xfatal-warnings",
  "-Ywarn-dead-code",
  "-Ywarn-numeric-widen",
  "-Ywarn-value-discard",
  "-Ywarn-unused"
)

addCompilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.1")
addCompilerPlugin("org.typelevel" %% "kind-projector" % "0.11.0" cross CrossVersion.full)
