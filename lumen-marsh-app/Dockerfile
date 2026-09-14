# syntax=docker/dockerfile:1

FROM debian:bookworm-slim AS flutter-sdk
RUN apt-get update \
  && apt-get install -y --no-install-recommends \
    ca-certificates \
    curl \
    git \
    unzip \
    xz-utils \
  && rm -rf /var/lib/apt/lists/*
ARG FLUTTER_VERSION=3.47.1
RUN git clone --depth 1 --branch ${FLUTTER_VERSION} https://github.com/flutter/flutter.git /flutter
ENV PATH="/flutter/bin:/flutter/bin/cache/dart-sdk/bin:${PATH}"
RUN flutter config --no-analytics \
  && flutter precache --web

FROM flutter-sdk AS build
WORKDIR /app
COPY pubspec.yaml pubspec.lock ./
RUN flutter pub get
COPY analysis_options.yaml ./
COPY lib ./lib
COPY web ./web
COPY assets ./assets
ARG VENUEOPS_API_BASE_URL=http://localhost:8080
RUN flutter build web --release \
  --dart-define=VENUEOPS_API_BASE_URL=${VENUEOPS_API_BASE_URL}

FROM nginxinc/nginx-unprivileged:1.27-alpine AS runtime
COPY docker/nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/build/web /usr/share/nginx/html
USER root
RUN printf 'ok\n' > /usr/share/nginx/html/health \
  && chown -R nginx:nginx /usr/share/nginx/html
USER nginx
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
  CMD wget -qO- http://127.0.0.1:8080/health | grep -q ok
CMD ["nginx", "-g", "daemon off;"]
