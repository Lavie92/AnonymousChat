pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven {  url = uri{ "https://jitpack.io"} }
        maven { url= uri{  "https://storage.zego.im/maven"} }
        maven { url = uri{  "https://cardinalcommerceprod.jfrog.io/artifactory/android"}
            credentials {
                username = "paypal_sgerritz"
                password = "AKCp8jQ8tAahqpT5JjZ4FRP2mW7GMoFZ674kGqHmupTesKeAY2G8NcmPKLuTxTGkKjDLRzDUQ"
            }
        }
    }
}

rootProject.name = "DoAn_ChuyenNganh"
include(":app")
include(":filterBadwordsLibrary")
