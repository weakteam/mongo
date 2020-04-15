import BuildInfo._

addCommandAlias("fmt", "all scalafmtSbt scalafmt test:scalafmt")
addCommandAlias("fmtCheck", "all scalafmtSbtCheck scalafmtCheck test:scalafmtCheck")

val compilerOpts = Seq(
  "-deprecation", // Emit warning and location for usages of deprecated APIs.
  "-encoding", // Specify character encoding
  "utf-8", // used by source files.
  "-explaintypes", // Explain type errors in more detail.
  "-feature", // Emit warning and location for usages of features that should be imported explicitly.
  "-language:existentials", // Existential types (besides wildcard types) can be written and inferred
  "-language:experimental.macros", // Allow macro definition (besides implementation and application)
  "-language:higherKinds", // Allow higher-kinded types
  "-language:implicitConversions", // Allow definition of implicit functions called views
  "-unchecked", // Enable additional warnings where generated code depends on assumptions.
  "-Xcheckinit", // Wrap field accessors to throw an exception on uninitialized access.
  "-Xfatal-warnings", // Fail the compilation if there are any warnings.
  "-Xlint:adapted-args", // Warn if an argument list is modified to match the receiver.
  "-Xlint:constant", // Evaluation of a constant arithmetic expression results in an error.
  "-Xlint:delayedinit-select", // Selecting member of DelayedInit.
  "-Xlint:doc-detached", // A Scaladoc comment appears to be detached from its element.
  "-Xlint:inaccessible", // Warn about inaccessible types in method signatures.
  "-Xlint:infer-any", // Warn when a type argument is inferred to be `Any`.
  "-Xlint:missing-interpolator", // A string literal appears to be missing an interpolator id.
  "-Xlint:nullary-override", // Warn when non-nullary `def f()' overrides nullary `def f'.
  "-Xlint:nullary-unit", // Warn when nullary methods return Unit.
  "-Xlint:option-implicit", // Option.apply used implicit view.
  "-Xlint:package-object-classes", // Class or object defined in package object.
  "-Xlint:poly-implicit-overload", // Parameterized overloaded implicit methods are not visible as view bounds.
  "-Xlint:private-shadow", // A private field (or class parameter) shadows a superclass field.
  "-Xlint:stars-align", // Pattern sequence wildcard must align with sequence component.
  "-Xlint:type-parameter-shadow", // A local type parameter shadows a type already in scope.
  "-Ywarn-dead-code", // Warn when dead code is identified.
  "-Ywarn-extra-implicit", // Warn when more than one implicit parameter section is defined.
  "-Ywarn-numeric-widen", // Warn when numerics are widened.
  "-Ywarn-macros:after", // Only inspect expanded trees when generating unused symbol warnings.
  "-Ywarn-unused:implicits", // Warn if an implicit parameter is unused.
  "-Ywarn-unused:imports", // Warn if an import selector is not referenced.
  "-Ywarn-unused:locals", // Warn if a local definition is unused.
  "-Ywarn-unused:params", // Warn if a value parameter is unused.
  "-Ywarn-unused:patvars", // Warn if a variable bound in a pattern is unused.
  "-Ywarn-unused:privates", // Warn if a private member is unused.
  "-Ywarn-value-discard", // Warn when non-Unit expression results are unused.
  "-Ymacro-annotations"
)

inThisBuild(
  Seq(
    organization := "io.github.weakteam",
    scalaVersion := "2.13.1",
    crossScalaVersions := Seq("2.11.12", "2.12.11", "2.13.1"),
    cancelable in Global := true,
    onChangedBuildSource in Global := ReloadOnSourceChanges
  )
)

val bm4           = "0.3.1"
val kindProjector = "0.11.0"
val silencer      = "1.6.0"

val bm4Plugin           = "com.olegpy"      %% "better-monadic-for" % bm4
val kindProjectorPlugin = "org.typelevel"   % "kind-projector"      % kindProjector cross CrossVersion.full
val silencerPlugin      = "com.github.ghik" %% "silencer-plugin"     % silencer cross CrossVersion.full
val silencerLib         = "com.github.ghik" % "silencer-lib"        % silencer % Provided cross CrossVersion.full

val commonSettings = Seq(
  parallelExecution in Test := false,
  scalacOptions := compilerOpts,

  scalacOptions in compile in Compile ++= (CrossVersion.partialVersion(scalaVersion.value) match {
    case Some((2, y)) if y >= 12 => Seq("-Ymacro-annotations")
    case _ => Nil
  })
)

lazy val core = project
  .in(file("core"))
  .settings(
    scalafmtOnCompile := true,
    name := "mongo-core",
    version := "0.1.0",
    scalacOptions ++= compilerOpts
  )
  .settings(commonSettings: _*)
  .settings(
    libraryDependencies ++=
        silencerLib +: Seq(silencerPlugin, kindProjectorPlugin, bm4Plugin).map(compilerPlugin)
  )
  .enablePlugins(UniversalPlugin, JavaAppPackaging)
  .withBuildInfo
