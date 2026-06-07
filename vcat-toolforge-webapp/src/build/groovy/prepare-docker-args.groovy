// 1. Parse project.toml for apt packages
def tomlFile = new File(project.basedir.parentFile, "project.toml")
def aptPackagesList = []
if (tomlFile.exists()) {
    def inInstallBlock = false
    tomlFile.eachLine { line ->
        line = line.trim()
        if (line.contains("install = [")) {
            inInstallBlock = true
        }
        if (inInstallBlock) {
            // Extract values enclosed in quotes
            def matcher = line =~ /"([^"]+)"|'([^']+)'/
            if (matcher) {
                aptPackagesList << (matcher[0][1] ?: matcher[0][2])
            }
            if (line.contains("]")) {
                inInstallBlock = false
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
