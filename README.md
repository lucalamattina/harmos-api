# Harmos API

Este proyecto es la API backend de Harmos.

## Ejecutar localmente

Para ejecutar la API localmente, sigue estos pasos:

1.  **Requisitos previos:** Asegúrate de tener instalado **Docker** y **Docker Compose** en tu sistema.

2.  **Levantar la base de datos local:** Ejecuta el siguiente comando en la raíz del proyecto para iniciar la base de datos definida en `docker-compose.yml`:

    ```bash
    make
    ```

3.  **Activar el perfil local:** Para que la aplicación utilice la base de datos local, debes activar el perfil `local`. Puedes hacerlo de las siguientes maneras:

    * **Desde tu IDE (IntelliJ IDEA, Eclipse, VSCode):**
        * Ve a la configuración de ejecución/debug de tu aplicación Spring Boot.
        * Busca la opción "Active profiles" (o similar).
        * Escribe `local` en ese campo.

    * **Desde la línea de comandos (usando Gradle):**
        * Ejecuta el siguiente comando para iniciar la aplicación con el perfil `local`:

            ```bash
            ./gradlew bootRun -Dspring.profiles.active=local
            ```

    * **Mediante una variable de entorno:**
        * **Linux/macOS:** Define la variable de entorno `SPRING_PROFILES_ACTIVE` antes de ejecutar la aplicación:

            ```bash
            export SPRING_PROFILES_ACTIVE=local
            ./gradlew bootRun
            ```

        * **Windows (Command Prompt):**

            ```bash
            set SPRING_PROFILES_ACTIVE=local
            gradlew.bat bootRun
            ```

¡Listo! Con estos pasos, la API de Harmos debería estar ejecutándose localmente utilizando la base de datos configurada con Docker Compose y el perfil `local`.