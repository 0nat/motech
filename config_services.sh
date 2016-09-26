#!/usr/bin/env bash

sudo apt-get update -qq

#Change root password in mysql
if [ "$DB" = "mysql" ]; then
    echo "USE mysql;\nUPDATE user SET password=PASSWORD('password') WHERE user='root';\nFLUSH PRIVILEGES;\n" | mysql -u root
fi