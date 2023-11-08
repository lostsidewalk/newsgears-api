<link rel="stylesheet" type="text/css" href="style.css">

# newsgears-api

newsgears is a multi-user, self-hosted all-in-one RSS reader/aggregator platform.

This repository contains the API server, which provides HTTP-based REST access to the core feed subscription 
and aggregation capabilities of the entire platform. 

## To self-host:

#### (Optional) If you want to enable OAUTH2 via Google, provide the following environment:
- ```SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_GOOGLE_CLIENTID=@null```
- ```SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_GOOGLE_CLIENTSECRET=@null```
- ```SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_GOOGLE_REDIRECTURI=http://localhost:8080/oauth2/callback/{registrationId}```
- ```SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_GOOGLE_SCOPE=email,profile```

Get your own values for client Id/client secret from Google and supply them in place of the '@null' values above.

The value of the OAuth2 redirect URI should be:

```
http://localhost:8080/oauth2/callback/{registrationId}
```

The value of the ```scope``` property must be ```email,profile```, regardless of the OAuth2 provider.

If you don't want to use OAuth2, you'll have to go through the account registration process in order to login.

<hr>

## 2. Quick-start using pre-built containers:

If you don't want to do development, just start NewsGears using pre-built containers:

```
docker ...
```

<hr>

## 3. For local development:

If you don't want to use the pre-built containers (i.e., you want to make custom code changes and build your own containers), then use the following instructions.

### Setup command aliases:

A script called `build_module.sh` is provided to expedite image assembly.  Setup command aliases to run it to build the required images after you make code changes:

```
alias ng-api='./build_module.sh newsgears-api'
```

#### Alternately, setup aliases build debuggable containers:

```
alias ng-api='./build_module.sh newsgears-api --debug 45005'
```

*Debuggable containers pause on startup until a remote debugger is attached on the specified port.*

### Build and run:

#### Run the following command in the directory that contains ```newsgears-api```:

```
ng-api && docker ...
```

Boot down in the regular way, by using ```docker ...``` in the ```newsgears-api``` directory.

<hr> 

You can also use the `ng-api` alias to rebuild the container (i.e., to deploy code changes).

```
$ ng-api # rebuild the API server container 
```
