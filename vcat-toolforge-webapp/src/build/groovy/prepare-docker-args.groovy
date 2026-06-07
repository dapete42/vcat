// 1. Parse project.toml for apt packages
def tomlFile = new File(project.basedir.parentFile, "project.toml")
def aptPackagesList = []
if (tomlFile.exists()) {
    def tomlText = tomlFile.text
    def blockMatcher = tomlText =~ /(?s)install\s*=\s*\[([^]]+)]/
    if (blockMatcher.find()) {
        def blockContent = blockMatcher.group(1)
        blockContent.eachLine { line ->
            def matcher = line.trim() =~ /"([^"]+)"|'([^']+)'/
            if (matcher) {
                aptPackagesList << (matcher[0][1] ?: matcher[0][2])
            }
        }
    }
}
def aptPackages = aptPackagesList.join(" ")
project.properties.setProperty("docker.apt_packages", aptPackages)

// 2. Get java.runtime.version from system.properties and store it in
// docker.java_version
def propsFile = new File(project.basedir.parentFile, "system.properties")
if (propsFile.exists()) {
    def sysProps = new Properties()
    propsFile.withInputStream { sysProps.load(it) }

    // Map java.runtime.version specifically to docker.java_version
    def javaVersion = sysProps.getProperty("java.runtime.version")
    if (javaVersion != null) {
        project.properties.setProperty("docker.java_version", javaVersion)
    }
}
