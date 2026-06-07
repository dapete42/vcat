// 1. Verify Graphviz has been installed and can be executed
println "Verify Graphviz has been installed and can be executed"
try {
    def dotProcess = "/layers/heroku_deb-packages/packages/usr/bin/dot -V".execute()
    dotProcess.waitFor()
    if (dotProcess.exitValue() != 0) {
        throw new RuntimeException("Graphviz execution failed with exit code: " + dotProcess.exitValue())
    }
} catch (Exception e) {
    throw new RuntimeException("Failed to execute Graphviz: " + e.getMessage())
}

// 2. Verify OS and OS version in the Toolforge Heroku build environment
def expectedOs = project.properties['heroku.os']
def expectedVersion = project.properties['heroku.os.version']

println "Verify OS is ${expectedOs} ${expectedVersion} in the Toolforge Heroku build environment"

def os = "lsb_release -is".execute().text.trim()
def osVersion = "lsb_release -rs".execute().text.trim()

if (expectedOs != os) {
    throw new RuntimeException("OS is not ${expectedOs} in the Toolforge Heroku build environment (it is ${os})")
}

if (expectedVersion != osVersion) {
    throw new RuntimeException("OS version is not ${expectedVersion} in the Toolforge Heroku build environment (it is ${os_version})")
}
