name := "sprof"

libraryDependencies += "org.javassist" % "javassist" % "3.19.0-GA"

packageOptions := Seq(
  Package.ManifestAttributes("Premain-Class" -> "org.sprof.Profiler"),
  Package.ManifestAttributes("Agent-Class" -> "org.sprof.Profiler"),
  Package.ManifestAttributes("Can-Redefine-Classes" -> "true"),
  Package.ManifestAttributes("Can-Retransform-Classes" -> "true")
)