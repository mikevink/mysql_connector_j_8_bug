plugins {
    id("java")
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
//    implementation("mysql:mysql-connector-java:5.1.49")
    implementation("mysql:mysql-connector-java:8.0.30")
    implementation("org.apache.commons:commons-dbcp2:2.9.0")
    implementation("org.slf4j:slf4j-api:2.0.0")
    implementation("org.slf4j:slf4j-simple:2.0.0")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.1")
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}