## client


```
ng new --standalone=false client

ng g c --module app --flat --skip-tests components/upload

ng g c --module app --flat --skip-tests components/view-image

ng g s --flat --skip-tests services/fileupload

ng g i model/UploadResult

ng serve --proxy-config proxy-config.js
```

## server

```
mvn clean spring-boot:run
```

```
http://localhost:4200/ -> default whatever component
http://localhost:4200/dog --> dog component
http://localhost:4200/api/user--> spring boot http://localhost:8080(4200)/api/user

bypass the CORS validation !

http://localhost:8080/api/rest/user
http://localhost:8080/api/user
```