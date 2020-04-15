import BuildInfo._
import Dependencies._

addCommandAlias("fmt", "all scalafmtSbt scalafmt test:scalafmt")
addCommandAlias("fmtCheck", "all scalafmtSbtCheck scalafmtCheck test:scalafmtCheck")
addCommandAlias("to212", "set scalaVersion in ThisBuild := \"2.12.11\"")
addCommandAlias("to213", "set scalaVersion in ThisBuild := \"2.13.1\"")

val compilerOpts212 = Seq(
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
  "-Ywarn-numeric-widen", // Warn when numerics are widened.
  "-Ywarn-value-discard", // Warn when non-Unit expression results are unused.
  "-Xlint:constant", // Evaluation of a constant arithmetic expression results in an error.
  "-Ywarn-macros:after", // Only inspect expanded trees when generating unused symbol warnings.
  "-Ywarn-unused:locals", // Warn if a local definition is unused.
  "-Ywarn-unused:params", // Warn if a value parameter is unused.
  "-Ywarn-unused:imports", // Warn if an import selector is not referenced.
  "-Ywarn-unused:patvars", // Warn if a variable bound in a pattern is unused.
  "-Ywarn-unused:privates", // Warn if a private member is unused.
  "-Ywarn-unused:implicits", // Warn if an implicit parameter is unused.
  "-Ywarn-extra-implicit" // Warn when more than one implicit parameter section is defined.
)

val compilerOpts212Only = Seq(
  "-Xlint:unsound-match", // Pattern match may not be typesafe.
  "-Xlint:by-name-right-associative", // By-name parameter of right associative operator.
  "-Xfuture" // Turn on future language features.
)

val compilerOpts213 = Seq("-Ymacro-annotations")

lazy val setMinorVersion = minorVersion := {
  CrossVersion.partialVersion(scalaVersion.value) match {
    case Some((2, v)) => v.toInt
    case _            => 0
  }
}

inThisBuild(
  Seq(
    organization := "io.github.weakteam",
    scalaVersion := "2.13.1",
    crossScalaVersions := Seq("2.12.11", "2.13.1"),
    cancelable in Global := true,
    onChangedBuildSource in Global := ReloadOnSourceChanges,
    setMinorVersion
  )
)

val commonSettings = Seq(
  parallelExecution in Test := false,
  scalacOptions := compilerOpts212,
  scalacOptions in (Compile, compile) ++= {
    minorVersion.value match {
      case 13 => compilerOpts213
      case 12 => compilerOpts212Only
      case _  => Nil
    }
  },
  scalacOptions in Test --= compilerOpts213,
  libraryDependencies ++= {
    minorVersion.value match {
      case 13 => Seq(scalaOrganization.value % "scala-reflect" % scalaVersion.value)
      case 12 =>
        Seq(
          compilerPlugin(Dependencies.Plugins.macroParadise),
          scalaOrganization.value % "scala-reflect" % scalaVersion.value
        )
    }
  }
)

lazy val core = project
  .in(file("core"))
  .settings(
    scalafmtOnCompile := true,
    name := "mongo-core",
    version := "0.1.0"
  )
  .settings(commonSettings: _*)
  .settings(
    libraryDependencies ++=
        Dependencies.Plugins.plugins ++ Dependencies.TestLibraries.testLibraries.map(_ % Test)
  )
  .enablePlugins(UniversalPlugin, JavaAppPackaging)
  .withBuildInfo
