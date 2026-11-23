rootProject.name = "odontologia"

check(JavaVersion.current() == JavaVersion.VERSION_21) {
    "El backend requiere Java 21 Eclipse Temurin. Revisar JAVA_HOME."
}
check(System.getProperty("java.vendor").contains("Adoptium")) {
    "Seleccionar Eclipse Temurin mediante JAVA_HOME."
}
