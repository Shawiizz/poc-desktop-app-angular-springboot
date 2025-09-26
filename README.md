# Desktop app using SpringBoot as a backend and Angular as a frontend

This project is a desktop application that utilizes SpringBoot for the backend and Angular for the frontend.       
It is designed to be inside the same jar file, making it easy to run.

## Why?

Angular is a great framework for building web applications, but sometimes you need a desktop application.
This project allows you to leverage the power of Angular while still being able to run it as a desktop application.

## How?

The project is structured as a SpringBoot application with an embedded Angular frontend.
When you build the project, the Angular application is compiled and placed in the `src/main/resources/static` directory
of the SpringBoot application.
This allows SpringBoot to serve the Angular application as static content.

To run the application, you simply need to execute the SpringBoot application, which will serve the Angular frontend.

### Using a remote frontend instead of the bundled one

If you want to point the desktop shell to an external (already deployed) frontend instead of the packaged Angular build, set the property in `application.properties`:

```
frontend.remote-url=https://my-remote-frontend.example.com
```

If this property is blank or commented, the locally packaged Angular assets are served.

At runtime the Java window will open directly on the remote URL (still allowing the backend REST APIs to be called from that origin if CORS is properly configured on your remote build).

### Communication between Angular and SpringBoot

The Angular frontend communicates with the SpringBoot backend using RESTful APIs (see `api.service.ts` and `app.ts` for
example usages).
You can define your REST endpoints in the SpringBoot application and call them from the Angular application using HTTP
requests.

You could also use WebSockets or other communication methods if needed.

## Developers

### Frontend

When developing, you can run the Angular application separately using the Angular CLI.
This allows you to take advantage of features like hot-reloading and easier debugging.

To run the Angular application separately, navigate to the `frontend` directory and use the following command:

```bash
npm run dev
```

This will start the Angular development server, and you can access the application at `http://localhost:4200`.

### Backend

For the SpringBoot backend, you can run it using your IDE or by using the following command:

```bash
./gradlew bootRun -Dspring.profiles.active=dev
```

Make sure to comment out the line `launchDesktopApp();` in the `DesktopApplication.java` file to prevent the Angular
application from launching automatically during development.

### Building without Angular (backend only)

By default, the Gradle build runs the Angular build (`buildAngular`) and copies the result into `static/`.

If you just need a backend jar (because you will always use a remote frontend), you can skip Angular with:

```bash
./gradlew bootJarNoAngular bootJar -x copyAngularBuild -x buildAngular
```

For running with a custom remote frontend using args (for testing purposes), you can use:
```bash
java -Dfrontend.remote-url="https://my-remote-frontend.example.com" -jar build/libs/<jar-name>.jar
```

### Building the project

When you are ready to build the project for production, you can use the following command:

```bash
./gradlew build
```

This will compile the Angular application and package it with the SpringBoot application into a single jar file located
in the `build/libs` directory.