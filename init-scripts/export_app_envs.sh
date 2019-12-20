#! /bin/sh

APP_SECRETS_FOLDER=/var/run/secrets/nais.io/vault/su-person
[ -d "$APP_SECRETS_FOLDER" ] && {
    source "$APP_SECRETS_FOLDER"/*.env
    export $(cut -d= -f1 "$APP_SECRETS_FOLDER"/*.env)
}
