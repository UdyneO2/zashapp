plugins {
    id("com.android.application") version "8.2.0" apply false
    id("org.jetbrains.kotlin.android") version "1.9.20" apply false
}

tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}

// Global configuration untuk semua subprojects
subprojects {
    tasks.withType<JavaCompile> {
        options.encoding = "UTF-8"
        options.isIncremental = true
    }
    
    // Fix untuk classpath warnings
    configurations.matching { it.name == "classpath" }.all {
        isCanBeResolved = true
        isCanBeConsumed = false
    }
}
