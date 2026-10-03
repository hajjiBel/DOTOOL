FROM nginx:1.27-alpine
ARG VERSION=dev
COPY index.html /usr/share/nginx/html/index.html
RUN sed -i "s/@@VERSION@@/${VERSION}/" /usr/share/nginx/html/index.html \
 && echo "${VERSION}" > /usr/share/nginx/html/version.txt
HEALTHCHECK --interval=15s --timeout=3s CMD wget -qO- http://localhost/ >/dev/null || exit 1
