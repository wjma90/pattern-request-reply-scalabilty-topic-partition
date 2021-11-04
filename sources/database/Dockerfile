FROM mysql:5.7.25

ENV MYSQL_ROOT_PASSWORD=toor
ENV MYSQL_DATABASE=demobd
ENV MYSQL_ROOT_HOST=%

COPY scripts/ /docker-entrypoint-initdb.d/
CMD ["--default-authentication-plugin=mysql_native_password"]