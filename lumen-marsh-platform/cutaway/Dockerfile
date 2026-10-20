# syntax=docker/dockerfile:1

FROM nginxinc/nginx-unprivileged:1.27-alpine
COPY nginx.conf /etc/nginx/templates/default.conf.template
COPY public/ /usr/share/nginx/html/
ENV NGINX_ENVSUBST_FILTER=DEMO_GATEWAY_SECRET
USER root
RUN printf 'ok\n' > /usr/share/nginx/html/health \
  && chown -R nginx:nginx /usr/share/nginx/html
USER nginx
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
  CMD wget -qO- http://127.0.0.1:8080/health | grep -q ok
CMD ["nginx", "-g", "daemon off;"]
